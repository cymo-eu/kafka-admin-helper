package eu.cymo.kafka.admin.model.acl;

import java.util.Collection;

public class KafkaConfigWrapper {

    private Collection<KafkaAcl> acls;

    public KafkaConfigWrapper(Collection<KafkaAcl> acls) {
        this.acls = acls;
    }

    public KafkaConfigWrapper() {
    }

    public Collection<KafkaAcl> getAcls() {
        return acls;
    }

    public void setAcls(Collection<KafkaAcl> acls) {
        this.acls = acls;
    }
}
