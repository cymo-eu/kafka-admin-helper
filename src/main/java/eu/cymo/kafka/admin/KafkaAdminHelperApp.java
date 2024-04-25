package eu.cymo.kafka.admin;

import eu.cymo.kafka.admin.model.acl.KafkaAcl;
import eu.cymo.kafka.admin.model.acl.KafkaConfigWrapper;
import eu.cymo.kafka.admin.model.topic.KafkaTopic;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KafkaAdminHelperApp {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminHelperApp.class);

    public enum Mode {
        APPLY, IMPORT, PLAN
    }

    record ApplyAction(List<KafkaAcl> aclstoAdd, List<KafkaAcl> aclsToRemove) { }

    public static void main(String[] args) {

        Options options = new Options();

        Option kafkaConfig = Option.builder("k")
                .longOpt("kafkaConfig")
                .hasArg()
                .argName("config")
                .desc("Path to the Kafka configuration file")
                .required()
                .build();

        Option stateFileLocation = Option.builder("sf")
                .longOpt("stateFileLocation")
                .hasArg()
                .argName("file")
                .desc("Path to the state file location")
                .build();

        Option stateDirectoryLocation = Option.builder("sd")
                .longOpt("stateDirectoryLocation")
                .hasArg()
                .argName("dir")
                .desc("Path to the state directory location")
                .build();

        OptionGroup modeGroup = new OptionGroup();
        modeGroup.addOption(new Option("a", "apply", false, "Run in apply mode. Configurations will be applied to the cluster."));
        modeGroup.addOption(new Option("i", "import", false, "Run in import mode. Configurations from the cluster will be printed to the console in yaml format."));
        modeGroup.addOption(new Option("p", "plan", false, "Run in plan mode. Configurations will be compared to the cluster. Security reporting will also run."));
        modeGroup.setRequired(true);

        options.addOptionGroup(modeGroup);
        options.addOption(kafkaConfig);
        options.addOption(stateFileLocation);
        options.addOption(stateDirectoryLocation);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine line = parser.parse(options, args);

            Mode mode = null;
            if (line.hasOption("apply")) {
                mode = Mode.APPLY;
            } else if (line.hasOption("import")) {
                mode = Mode.IMPORT;
            } else if (line.hasOption("plan")) {
                mode = Mode.PLAN;
            }

            // Check state directory requirements for apply and report modes
            if ((mode == Mode.APPLY || mode == Mode.PLAN) && (!line.hasOption(stateFileLocation.getOpt()) && !line.hasOption(stateDirectoryLocation.getOpt()))) {
                throw new ParseException("State file, State directory or both must be provided in apply or plan mode.");
            }

            String kafkaConfigPath = line.getOptionValue(kafkaConfig.getOpt());
            String stateFilePath = line.getOptionValue(stateFileLocation.getOpt());
            String stateDirPath = line.getOptionValue(stateDirectoryLocation.getOpt());

            var adminClient = KafkaAdminClient.create(retrieveConfig(kafkaConfigPath));
            var clusterAcls = retrieveClusterAcls(adminClient);
            var clusterTopics = retrieveClusterTopics(adminClient);

            switch (mode) {
                case PLAN -> handlePlan(clusterAcls, stateFilePath, stateDirPath);
                case APPLY -> handleApply(adminClient, handlePlan(clusterAcls, stateFilePath, stateDirPath));
                case IMPORT -> handleImport(clusterAcls, clusterTopics);
                case null -> log.error("Invalid mode. This should be unreachable");
            }

        } catch (ParseException exp) {
            log.error("Parsing failed. Reason: {}", exp.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Main", options);
        }
    }

    private static void handleApply(AdminClient adminClient, ApplyAction applyAction) {
        log.info("Materializing");
        try {
            if (!applyAction.aclstoAdd.isEmpty()) {
                addAcls(adminClient, applyAction.aclstoAdd);
            }
            if (!applyAction.aclsToRemove.isEmpty()) {
                removeAcls(adminClient, applyAction.aclsToRemove);

            }
        } catch (InterruptedException iex){
            log.error("Operation interrupted. State unclear[{}]", iex.getMessage());
        } catch (ExecutionException eex){
            log.error("Execution exception. State unclear[{}]", eex.getMessage());
        }
    }

    private static ApplyAction handlePlan(Collection<KafkaAcl> clusterAcls, String stateLocationPath, String stateDirPath) {
        Collection<KafkaAcl> desiredState = new ArrayList<>();
        if(stateLocationPath != null){
            desiredState.addAll(parseDesiredState(stateLocationPath));
        }
        if (stateDirPath != null){
            desiredState.addAll(new StateConfigDirectory(stateDirPath)
                            .getStateConfigs()
                            .stream()
                            .flatMap(path -> parseDesiredState(path).stream())
                            .toList());
        }

        var aclsToAdd = desiredState.stream().filter(acl -> !clusterAcls.contains(acl)).collect(Collectors.toList());
        var aclsToRemove = clusterAcls.stream().filter(acl -> !desiredState.contains(acl)).collect(Collectors.toList());

        log.info("ACL diff status: {} to add / {} to remove", aclsToAdd.size(), aclsToRemove.size());

        log.info("ACLs to add:{} {}", System.lineSeparator(), aclsToAdd.stream().map(acl -> acl.toString() + System.lineSeparator()).collect(Collectors.toList()));
        log.info("ACLs to remove:{} {}", System.lineSeparator(), aclsToRemove.stream().map(acl -> acl.toString() + System.lineSeparator()).collect(Collectors.toList()));

        securityCompliancyReport(desiredState);

        return new ApplyAction(aclsToAdd, aclsToRemove);
    }

    private static void handleImport(Collection<KafkaAcl> kafkaAcls, Collection<KafkaTopic> kafkaTopics) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        // Customize Representer to avoid adding YAML tags
        Representer representer = new Representer(options);
        representer.addClassTag(KafkaAcl.class, Tag.MAP);
        representer.addClassTag(KafkaTopic.class, Tag.MAP);

        Yaml yaml = new Yaml(representer, options);
        log.info(yaml.dump(kafkaAcls));
        log.info(yaml.dump(kafkaTopics));
    }


    private static void removeAcls(AdminClient adminClient, List<KafkaAcl> remove) throws InterruptedException, ExecutionException {
        DeleteAclsResult deleteResult = adminClient.deleteAcls(remove.stream()
                .map(acl -> acl.toAclBinding().toFilter())
                .collect(Collectors.toList()));
        deleteResult.all().whenComplete((Collection<?> c, Throwable ex) -> {
            if (ex != null) {
                log.error("Delete ACLs failed [{}]", ex.getMessage());
            }
            log.info("Remove result => #attempted {} ACLs:{}{}",
                    deleteResult.values().size(),
                    System.lineSeparator(),
                    deleteResult.values().values().stream()
                            .map(result -> result.isCompletedExceptionally() || result.isCancelled() ? "[FAILED] " : "[SUCCESS] " + result.toString() + System.lineSeparator())
                            .collect(Collectors.toList()));
        }).get();
    }

    private static void addAcls(AdminClient adminClient, List<KafkaAcl> add) throws InterruptedException, ExecutionException {
        CreateAclsResult addResult = adminClient.createAcls(add.stream().map(KafkaAcl::toAclBinding).collect(Collectors.toList()));
        addResult.all().whenComplete((Void v, Throwable e) -> {
            if (e != null) {
                log.error("Add ACLs failed");
            }
            log.info("Add result => #attempted {} ACLs:{}{}",
                    addResult.values().size(),
                    System.lineSeparator(),
                    addResult.values().values().stream()
                            .map(kafkaFuture -> kafkaFuture.isCompletedExceptionally() || kafkaFuture.isCancelled() ? "[FAILED] " : "[SUCCESS] " + kafkaFuture.toString() + System.lineSeparator())
                            .collect(Collectors.toList()));

        }).get();
    }

    private static void securityCompliancyReport(Collection<KafkaAcl> desiredState) {
        //Write to wildcard is ILLEGAL
        log.info("~~~~~~Security Compliancy Scan~~~~~~");
        log.info("Wildcard Topic Write ACLs");
        desiredState.stream().filter(acl -> (acl.getPermissionType().equals(AclPermissionType.ALLOW) || acl.getPermissionType().equals(AclPermissionType.ANY)) && (acl.getOperation().equals(AclOperation.WRITE) || acl.getOperation().equals(AclOperation.IDEMPOTENT_WRITE)) && acl.getResourceType().equals(ResourceType.TOPIC) && acl.getName().trim().equals("*")).forEach(i -> log.info(i.toString()));
        log.info("God delete ACL");
        desiredState.stream().filter(acl -> (acl.getPermissionType().equals(AclPermissionType.ALLOW) || acl.getPermissionType().equals(AclPermissionType.ANY)) && acl.getOperation().equals(AclOperation.DELETE) && acl.getName().equals("*")).forEach(i -> log.info(i.toString()));
        log.info("Read all topics");
        desiredState.stream().filter(acl -> (acl.getPermissionType().equals(AclPermissionType.ALLOW) || acl.getPermissionType().equals(AclPermissionType.ANY)) && acl.getOperation().equals(AclOperation.READ) && acl.getResourceType().equals(ResourceType.TOPIC) && acl.getName().equals("*")).forEach(i -> log.info(i.toString()));
        log.info("Read all consumers");
        desiredState.stream().filter(acl -> (acl.getPermissionType().equals(AclPermissionType.ALLOW) || acl.getPermissionType().equals(AclPermissionType.ANY)) && acl.getOperation().equals(AclOperation.READ) && acl.getResourceType().equals(ResourceType.GROUP) && acl.getName().equals("*")).forEach(i -> log.info(i.toString()));
        log.info("~~~~~~~~~~~~");
    }


    private static Properties retrieveConfig(String path) {
        Properties p = new Properties();
        try {
            InputStream is = new FileInputStream(path);
            p.load(is);
        } catch (IOException ex) {
            log.error("Fatal error while loading configfile: {}  ", ex.getMessage());
        }
        return p;
    }

    private static Collection<KafkaAcl> retrieveClusterAcls(AdminClient ac) {
        try {
            return ac.describeAcls(AclBindingFilter.ANY).values().get().stream()
                    .map(KafkaAcl::from)
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            log.error("Interrupted why fetching ACLs: {}", e.getMessage());
            e.printStackTrace();
            System.exit(2);
        } catch (ExecutionException e) {
            log.error("Fetch ACLs failed: {}", e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
        return null;
    }

    private static Collection<KafkaTopic> retrieveClusterTopics(AdminClient ac) {
        try {
            var topics = ac.listTopics().names().get();

            return Stream.concat(ac.describeTopics(topics).allTopicNames().get()
                    .entrySet().stream()
                    .map(KafkaTopic::fromTopicDescription),ac.describeConfigs(topics.stream()
                            .map(topicName->new ConfigResource(ConfigResource.Type.TOPIC, topicName))
                            .collect(Collectors.toList()))
                    .all().get().entrySet().stream()
                    .map(KafkaTopic::fromTopicConfig))
                    .collect(Collectors.toMap(
                            KafkaTopic::getName,
                            entry -> entry,
                            (prevVal, newVal) -> new KafkaTopic(
                                    prevVal.getName(),
                                    prevVal.getPartitions()!=null?prevVal.getPartitions():newVal.getPartitions(),
                                    prevVal.getConfig()!=null?prevVal.getConfig():newVal.getConfig())
                    )).values().stream().toList();

        } catch (InterruptedException e) {
            log.error("Interrupted why fetching Topics: {}", e.getMessage());
            e.printStackTrace();
            System.exit(2);
        } catch (ExecutionException e) {
            log.error("Fetch Topics failed: {}", e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
        return null;
    }

    private static Collection<KafkaAcl> parseDesiredState(String desiredStateConfigPath) {
        Yaml yaml = new Yaml();
        try {
            InputStream is = new FileInputStream(desiredStateConfigPath);
            KafkaConfigWrapper aclWrapper = yaml.loadAs(is, KafkaConfigWrapper.class);
            Set<KafkaAcl> aclSet = aclWrapper.getAcls().stream().map(acl -> new KafkaAcl(acl.getPrincipal(), acl.getOperation(), acl.getPermissionType(), acl.getHost(), acl.getName(), acl.getResourceType(), acl.getPatternType())).collect(Collectors.toSet());
            if (aclWrapper.getAcls().size() != aclSet.size()) {
                log.warn("desired ACL state contains duplicate configurations");
            }
            return aclSet;
        } catch (IOException ex) {
            log.error("Fatal error while loading desired state: {}  ", ex.getMessage());
            System.exit(2);
        }
        return null;

    }

}
