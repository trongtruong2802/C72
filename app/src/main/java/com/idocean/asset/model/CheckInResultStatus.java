package com.idocean.asset.model;

import java.io.Serializable;

/**
 * Trang thai doi chieu check in.
 */
public enum CheckInResultStatus implements Serializable {
    RETURNED("Da mang ve"),
    MISSING("Con thieu"),
    OUTSIDE("Phat sinh / Ngoai phieu");

    private final String label;

    CheckInResultStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
