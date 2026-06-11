package com.idocean.asset.data.repository;

import com.idocean.asset.data.dto.InventoryCheckinResponseDto;

/**
 * Ket qua nghiep vu sau khi gui batch kiem ke.
 */
public final class InventoryCheckinUploadResult {
    public enum Outcome {
        SUCCESS,
        WARNING
    }

    private final Outcome outcome;
    private final InventoryCheckinResponseDto response;
    private final String userMessage;

    private InventoryCheckinUploadResult(
            Outcome outcome,
            InventoryCheckinResponseDto response,
            String userMessage
    ) {
        this.outcome = outcome == null ? Outcome.SUCCESS : outcome;
        this.response = response;
        this.userMessage = userMessage == null ? "" : userMessage.trim();
    }

    public static InventoryCheckinUploadResult success(
            InventoryCheckinResponseDto response,
            String userMessage
    ) {
        return new InventoryCheckinUploadResult(Outcome.SUCCESS, response, userMessage);
    }

    public static InventoryCheckinUploadResult warning(
            InventoryCheckinResponseDto response,
            String userMessage
    ) {
        return new InventoryCheckinUploadResult(Outcome.WARNING, response, userMessage);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public InventoryCheckinResponseDto getResponse() {
        return response;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public boolean isWarning() {
        return outcome == Outcome.WARNING;
    }

    public boolean isSuccess() {
        return outcome == Outcome.SUCCESS;
    }
}
