package eu.cymo.kafka.admin.model;

import eu.cymo.kafka.admin.model.acl.KafkaAcl;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.Assert;
import org.junit.Test;

public class KafkaAclTest {

    @Test
    public void testEquals(){
        Assert.assertEquals(
                new KafkaAcl("User:123", AclOperation.READ, AclPermissionType.ALLOW,"*","test123", ResourceType.TOPIC, PatternType.LITERAL),
                new KafkaAcl("User:123", AclOperation.READ, AclPermissionType.ALLOW,"*","test123", ResourceType.TOPIC, PatternType.LITERAL)
        );

    }
}
