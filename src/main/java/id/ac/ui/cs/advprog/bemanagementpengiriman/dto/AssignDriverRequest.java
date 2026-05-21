package id.ac.ui.cs.advprog.bemanagementpengiriman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDriverRequest {
    @NotNull
    private Long driverId;

    @NotEmpty
    @Valid
    private List<HarvestItemDto> harvestItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HarvestItemDto {
        @NotNull
        private UUID harvestId;
    }
}
