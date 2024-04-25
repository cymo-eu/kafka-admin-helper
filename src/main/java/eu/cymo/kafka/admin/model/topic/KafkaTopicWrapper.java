package eu.cymo.kafka.admin.model.topic;

import java.util.Collection;

public class KafkaTopicWrapper {

    private Collection<KafkaTopic> topics;

    public KafkaTopicWrapper(Collection<KafkaTopic> topics) {
        this.topics = topics;
    }

    public KafkaTopicWrapper() {
    }

    public Collection<KafkaTopic> getTopics() {
        return topics;
    }

    public void setTopics(Collection<KafkaTopic> topics) {
        this.topics = topics;
    }
}
