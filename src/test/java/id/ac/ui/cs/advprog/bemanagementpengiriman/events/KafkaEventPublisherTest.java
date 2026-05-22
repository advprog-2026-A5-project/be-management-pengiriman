package id.ac.ui.cs.advprog.bemanagementpengiriman.events;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaEventPublisherTest {

    @Test
    void publish_shouldSendEventToKafkaTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate);
        Object event = new Object();

        publisher.publish("pengiriman-topic", event);

        verify(kafkaTemplate).send("pengiriman-topic", event);
    }
}
