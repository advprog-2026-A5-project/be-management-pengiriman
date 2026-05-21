package id.ac.ui.cs.advprog.bemanagementpengiriman.client;

import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.KebunDetailResponse;
import id.ac.ui.cs.advprog.bemanagementpengiriman.dto.MandorKebunAssignmentResponse;

import java.util.Optional;

public interface KebunClient {
    Optional<MandorKebunAssignmentResponse> getMandorKebunAssignment(Long mandorId);

    Optional<KebunDetailResponse> getKebunDetail(String kebunCode);
}
