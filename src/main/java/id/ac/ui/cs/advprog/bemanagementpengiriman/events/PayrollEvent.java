package id.ac.ui.cs.advprog.bemanagementpengiriman.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PayrollEvent {
    private final Long userId;
    private final String role;
    private final Double kilogram;
}
