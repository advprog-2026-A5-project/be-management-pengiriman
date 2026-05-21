package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

public record MandorKebunAssignmentResponse(
        Long mandorId,
        String kebunId,
        String kebunCode,
        String kebunName,
        boolean active
) {
}
