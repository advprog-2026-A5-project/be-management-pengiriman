package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

public interface PaymentClient {
    void requestPayroll(Long actorId, Long userId, String role, Double kilogram);
}
