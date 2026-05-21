package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import java.util.List;

public record KebunDetailResponse(
        String code,
        String name,
        double luas,
        List<Object> coordinates,
        String mandorId,
        List<String> supirIds
) {
}
