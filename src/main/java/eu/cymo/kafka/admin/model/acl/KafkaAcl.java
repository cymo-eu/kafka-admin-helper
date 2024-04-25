package eu.cymo.kafka.admin.model.acl;

import org.apache.kafka.common.acl.AccessControlEntry;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;

import java.util.Objects;

public class KafkaAcl {

    private String principal;
    private AclOperation operation;
    private AclPermissionType permissionType;
    private String host;
    private String name;
    private ResourceType resourceType;
    private PatternType patternType;

    public KafkaAcl(String principal, AclOperation operation, AclPermissionType permissionType, String host, String name, ResourceType resourceType, PatternType patternType) {
        this.principal = principal;
        this.operation = operation;
        this.permissionType = permissionType;
        this.host = host;
        this.name = name;
        this.resourceType= resourceType;
        this.patternType = patternType;
    }

    public KafkaAcl(){

    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public AclOperation getOperation() {
        return operation;
    }

    public void setOperation(AclOperation operation) {
        this.operation = operation;
    }

    public AclPermissionType getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(AclPermissionType permissionType) {
        this.permissionType = permissionType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public PatternType getPatternType() {
        return patternType;
    }

    public void setPatternType(PatternType patternType) {
        this.patternType = patternType;
    }

    public static KafkaAcl from(AclBinding acl){
        return new KafkaAcl(acl.entry().principal(),acl.entry().operation(),acl.entry().permissionType(),acl.entry().host(),acl.pattern().name(),acl.pattern().resourceType(), acl.pattern().patternType());
    }


    public AclBinding toAclBinding(){
        return new AclBinding(new ResourcePattern(this.resourceType,this.name, this.patternType), new AccessControlEntry(this.principal,this.host, this.operation, this.permissionType));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaAcl kafkaAcl = (KafkaAcl) o;
        return Objects.equals(principal, kafkaAcl.principal) &&
                operation == kafkaAcl.operation &&
                permissionType == kafkaAcl.permissionType &&
                Objects.equals(host, kafkaAcl.host) &&
                Objects.equals(name, kafkaAcl.name) &&
                resourceType == kafkaAcl.resourceType &&
                patternType == kafkaAcl.patternType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(principal, operation, permissionType, host, name, resourceType, patternType);
    }

    @Override
    public String toString() {
        return "KafkaAcl{" +
                "principal='" + principal + '\'' +
                ", operation=" + operation +
                ", permissionType=" + permissionType +
                ", host='" + host + '\'' +
                ", name='" + name + '\'' +
                ", resourceType=" + resourceType +
                ", patternType=" + patternType +
                '}';
    }
}


