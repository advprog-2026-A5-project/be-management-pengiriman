package id.ac.ui.cs.advprog.bemanagementpengiriman.events;

public interface EventPublisher {
    void publish(String topic, Object event);
}
