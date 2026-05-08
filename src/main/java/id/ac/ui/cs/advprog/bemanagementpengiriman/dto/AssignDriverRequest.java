package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDriverRequest {
    private Long driverId;
    private List<HarvestItemDto> harvestItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HarvestItemDto {
        private Long harvestId;
        private double weightKg;
    }
}