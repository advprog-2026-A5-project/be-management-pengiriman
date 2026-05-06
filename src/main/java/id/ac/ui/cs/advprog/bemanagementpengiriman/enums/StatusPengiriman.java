package id.ac.ui.cs.advprog.bemanagementpengiriman.enums;

import lombok.Getter;

@Getter
public enum StatusPengiriman {

    MEMUAT("MEMUAT"),
    MENGIRIM("MENGIRIM"),
    TIBA_DI_TUJUAN("TIBA_DI_TUJUAN"),
    APPROVED_MANDOR("APPROVED_MANDOR"),
    REJECTED_MANDOR("REJECTED_MANDOR"),
    APPROVED_ADMIN("APPROVED_ADMIN"),
    REJECTED_ADMIN("REJECTED_ADMIN"),
    PARTIALLY_REJECTED_ADMIN("PARTIALLY_REJECTED_ADMIN");


    private final String value;
    private StatusPengiriman(String value) {
        this.value = value;
    }

    public boolean canDriverTransitionTo(StatusPengiriman next) {
        return switch (this) {
            case MEMUAT -> next == MENGIRIM;
            case MENGIRIM -> next == TIBA_DI_TUJUAN;
            default -> false;
        };
    }
}