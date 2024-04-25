package eu.cymo.kafka.admin.model.topic;

import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.config.ConfigResource;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class KafkaTopic {

    private String name;
    private Integer partitions;
    private Map<String,String> config;
    public KafkaTopic() {
    }

    public KafkaTopic(String name, Integer partitions, Map<String, String> config) {
        this.name = name;
        this.partitions = partitions;
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaTopic that = (KafkaTopic) o;
        return Objects.equals(name, that.name) && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, config);
    }

    public static KafkaTopic fromTopicConfig(Map.Entry<ConfigResource, Config> configResourceConfigEntry) {


        return new KafkaTopic(
                configResourceConfigEntry.getKey().name(),
                null,
                configResourceConfigEntry.getValue().entries().stream()
                        .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value)));
    }
    public static KafkaTopic fromTopicDescription(Map.Entry<String, TopicDescription> stringTopicDescriptionEntry) {
        return new KafkaTopic(stringTopicDescriptionEntry.getKey(), stringTopicDescriptionEntry.getValue().partitions().size(), null);
    }
}
