package com.idocean.asset.data.repository;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Locale;

/**
 * Helper nho de phan loai va dinh dang thong diep loi cho sync/mutation.
 */
final class AssetErrorFormatter {
    private AssetErrorFormatter() {
    }

    static boolean isTimeout(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof SocketTimeoutException || throwable instanceof InterruptedIOException) {
            return true;
        }
        String message = safeLowerMessage(throwable);
        return message.contains("timeout")
                || message.contains("time out")
                || message.contains("timed out");
    }

    static String buildConnectivityMessage(String endpointName, String actionLabel, Throwable throwable) {
        String safeEndpoint = safe(endpointName);
        String safeAction = safe(actionLabel);
        if (isTimeout(throwable)) {
            return "Ket noi toi backend " + safeEndpoint + " bi qua thoi gian cho khi " + safeAction + ". Thay doi chua duoc luu.";
        }
        return "Khong ket noi duoc backend " + safeEndpoint + " khi " + safeAction + ". Thay doi chua duoc luu.";
    }

    static String buildHttpMessage(int httpCode, String errorBody, String endpointName, String actionLabel) {
        String safeEndpoint = safe(endpointName);
        String safeAction = safe(actionLabel);
        String normalizedError = safe(errorBody).toLowerCase(Locale.ROOT);
        if (httpCode == 404 && normalizedError.contains(safeEndpoint.toLowerCase(Locale.ROOT)) && normalizedError.contains("not registered")) {
            return "Backend " + safeEndpoint + " chua duoc kich hoat tren n8n. Can activate workflow hoac tao production webhook /" + safeEndpoint + ".";
        }
        if (httpCode == 404) {
            return "Khong tim thay endpoint " + safeEndpoint + " tren backend khi " + safeAction + ".";
        }
        if (httpCode >= 500) {
            return "Backend " + safeEndpoint + " dang loi " + httpCode + " khi " + safeAction + ".";
        }
        return "Backend " + safeEndpoint + " tra HTTP " + httpCode + " khi " + safeAction + ".";
    }

    static String buildBusinessMessage(String endpointName, String actionLabel, String detail) {
        String safeEndpoint = safe(endpointName);
        String safeAction = safe(actionLabel);
        String safeDetail = safe(detail);
        if (safeDetail.isEmpty()) {
            return "Backend " + safeEndpoint + " bao xu ly " + safeAction + " khong thanh cong.";
        }
        return "Backend " + safeEndpoint + " bao xu ly " + safeAction + " khong thanh cong: " + safeDetail;
    }

    static String buildDebugSummary(String endpointName, String actionLabel, Throwable throwable, String extraDetail) {
        StringBuilder builder = new StringBuilder();
        builder.append("endpoint=").append(safe(endpointName));
        builder.append(" | action=").append(safe(actionLabel));
        if (throwable != null) {
            builder.append(" | type=").append(throwable.getClass().getSimpleName());
            String message = safe(throwable.getMessage());
            if (!message.isEmpty()) {
                builder.append(" | message=").append(message);
            }
        }
        String safeExtra = safe(extraDetail);
        if (!safeExtra.isEmpty()) {
            builder.append(" | extra=").append(safeExtra);
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeLowerMessage(Throwable throwable) {
        String message = throwable == null ? "" : throwable.getMessage();
        return safe(message).toLowerCase(Locale.ROOT);
    }
}
