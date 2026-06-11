package com.idocean.asset.ui.lookup;

import com.idocean.asset.model.Asset;

public final class LookupState {
    private Asset currentAsset;
    private boolean editing;
    private boolean saving;

    public Asset getCurrentAsset() {
        return currentAsset;
    }

    public void setCurrentAsset(Asset currentAsset) {
        this.currentAsset = currentAsset;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public boolean isSaving() {
        return saving;
    }

    public void setSaving(boolean saving) {
        this.saving = saving;
    }

    public boolean hasCurrentAsset() {
        return currentAsset != null;
    }

    public void reset() {
        currentAsset = null;
        editing = false;
        saving = false;
    }
}
