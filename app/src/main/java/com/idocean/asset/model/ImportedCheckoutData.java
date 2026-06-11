package com.idocean.asset.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ket qua import mot file check out de doi chieu check in.
 */
public class ImportedCheckoutData implements Serializable {
    private final String sourceFileName;
    private final CheckOutFormData formData;
    private final List<CheckoutAssetItem> expectedItems;

    public ImportedCheckoutData(String sourceFileName, CheckOutFormData formData, List<CheckoutAssetItem> expectedItems) {
        this.sourceFileName = sourceFileName == null ? "" : sourceFileName;
        this.formData = formData;
        this.expectedItems = expectedItems == null ? new ArrayList<>() : new ArrayList<>(expectedItems);
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public CheckOutFormData getFormData() {
        return formData;
    }

    public List<CheckoutAssetItem> getExpectedItems() {
        return new ArrayList<>(expectedItems);
    }
}
