package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.UserSummary;

import java.util.List;
import java.util.Optional;

public interface UserClient {

    Optional<UserSummary> findById(Long id);

    List<UserSummary> findByUsernameContainingIgnoreCase(String username);

    List<UserSummary> findAll();
}
