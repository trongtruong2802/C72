package com.idocean.asset.data.checkout;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.idocean.asset.model.CheckInResultItem;
import com.idocean.asset.model.CheckOutFormData;
import com.idocean.asset.model.CheckoutAssetItem;
import com.idocean.asset.model.ImportedCheckoutData;
import com.idocean.asset.data.io.CsvReader;
import com.idocean.asset.data.io.CsvWriter;
import com.idocean.asset.data.io.ExportFileManager;
import com.idocean.asset.utils.StringUtils;
import com.idocean.asset.utils.TimeFormatUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Quan ly format CSV cho luong check out / check in.
 */
public class CheckoutCsvRepository {
    public static final String CHECKOUT_EXPORT_TYPE = "IDO_CHECKOUT";
    public static final String CHECKIN_EXPORT_TYPE = "IDO_CHECKIN";
    public static final String EXPORT_VERSION = "1";

    private static final String[] CHECKOUT_HEADERS = new String[]{
            "export_type",
            "export_version",
            "ticket_id",
            "exported_at",
            "carrier_name",
            "department",
            "purpose",
            "event_name",
            "checkout_at",
            "expected_return_at",
            "approver",
            "note",
            "identity_key",
            "matched_from_cache",
            "tid",
            "code",
            "asset_name",
            "asset_type",
            "serial_number",
            "assigned_user",
            "asset_department",
            "location",
            "scan_source",
            "scanned_at"
    };

    private static final String[] CHECKIN_HEADERS = new String[]{
            "export_type",
            "export_version",
            "ticket_id",
            "checkout_source_file",
            "checkout_exported_at",
            "checkin_exported_at",
            "carrier_name",
            "department",
            "purpose",
            "event_name",
            "checkout_at",
            "expected_return_at",
            "approver",
            "note",
            "identity_key",
            "result_status",
            "matched_by",
            "expected_in_ticket",
            "tid",
            "code",
            "asset_name",
            "asset_type",
            "serial_number",
            "assigned_user",
            "asset_department",
            "location",
            "checkout_scan_source",
            "checkout_scanned_at",
            "checkin_scan_source",
            "checkin_scanned_at",
            "result_note"
    };

    private static final DateTimeFormatter EXPORT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final DateTimeFormatter CHECKOUT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    public String now() {
        return LocalDateTime.now().format(EXPORT_DATE_FORMAT);
    }

    public String today() {
        return LocalDate.now().format(CHECKOUT_DATE_FORMAT);
    }


    public boolean isValidDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate.parse(value.trim(), CHECKOUT_DATE_FORMAT);
            return true;
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }

    public long parseDateToMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return -1L;
        }
        try {
            LocalDate parsed = LocalDate.parse(value.trim(), CHECKOUT_DATE_FORMAT);
            return parsed.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return -1L;
        }
    }

    public String formatDate(long timeMillis) {
        if (timeMillis <= 0L) {
            return "";
        }
        return LocalDate.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault())
                .format(CHECKOUT_DATE_FORMAT);
    }

    public String generateTicketId() {
        return "CO_" + TimeFormatUtils.fileTimestamp();
    }

    public File exportCheckout(Context context, CheckOutFormData formData, List<CheckoutAssetItem> items) throws IOException {
        File exportFile = ExportFileManager.resolveExportFile(buildCheckoutExportFileName(formData));
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(exportFile, false), StandardCharsets.UTF_8))) {
            CsvWriter.writeUtf8Bom(writer);
            CsvWriter.writeExcelSeparatorHint(writer, ',');
            CsvWriter.writeQuotedLine(writer, ',', CHECKOUT_HEADERS);
            for (CheckoutAssetItem item : items) {
                CsvWriter.writeQuotedLine(writer, ',',
                        CHECKOUT_EXPORT_TYPE,
                        EXPORT_VERSION,
                        formData.getTicketId(),
                        formData.getExportedAt(),
                        formData.getCarrierName(),
                        formData.getDepartment(),
                        formData.getPurpose(),
                        formData.getEventName(),
                        formData.getCheckoutAt(),
                        formData.getExpectedReturnAt(),
                        formData.getApprover(),
                        formData.getNote(),
                        item.getIdentityKey(),
                        String.valueOf(item.isMatchedFromCache()),
                        item.getTid(),
                        item.getCode(),
                        item.getAssetName(),
                        item.getAssetType(),
                        item.getSerialNumber(),
                        item.getAssignedUser(),
                        item.getDepartment(),
                        item.getLocation(),
                        item.getScanSource(),
                        formatTimestamp(item.getScannedAt())
                );
            }
        }
        ExportFileManager.notifyMediaScanner(context, exportFile);
        return exportFile;
    }

    public ImportedCheckoutData importCheckout(Context context, Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Khong mo duoc file CSV check out.");
            }
            return importCheckout(context, uri, inputStream);
        }
    }

    public ImportedCheckoutData importCheckout(Context context, Uri uri, InputStream inputStream) throws IOException {
        List<CheckoutAssetItem> items = new ArrayList<>();
        CheckOutFormData formData = null;
        Map<String, CheckoutAssetItem> uniqueItems = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("File CSV check out dang rong.");
            }
            char delimiter = ';';
            if (isExcelSeparatorHint(headerLine)) {
                delimiter = resolveDelimiterFromSeparatorHint(headerLine);
                headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IOException("File CSV check out khong co header.");
                }
            }

            Map<String, Integer> headerIndex = CsvReader.buildHeaderIndex(CsvReader.parseDelimitedLine(headerLine, delimiter));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> row = CsvReader.parseDelimitedLine(line, delimiter);
                String exportType = CsvReader.getValue(headerIndex, row, "export_type");
                if (!exportType.isEmpty() && !CHECKOUT_EXPORT_TYPE.equals(exportType)) {
                    continue;
                }

                if (formData == null) {
                    formData = new CheckOutFormData(
                            CsvReader.getValue(headerIndex, row, "ticket_id"),
                            CsvReader.getValue(headerIndex, row, "exported_at"),
                            CsvReader.getValue(headerIndex, row, "carrier_name"),
                            CsvReader.getValue(headerIndex, row, "department"),
                            CsvReader.getValue(headerIndex, row, "purpose"),
                            CsvReader.getValue(headerIndex, row, "event_name"),
                            CsvReader.getValue(headerIndex, row, "checkout_at"),
                            CsvReader.getValue(headerIndex, row, "expected_return_at"),
                            CsvReader.getValue(headerIndex, row, "approver"),
                            CsvReader.getValue(headerIndex, row, "note")
                    );
                }

                String tid = CsvReader.getValue(headerIndex, row, "tid");
                String code = CsvReader.getValue(headerIndex, row, "code");
                String identityKey = CsvReader.getValue(headerIndex, row, "identity_key");
                if (identityKey.isEmpty()) {
                    identityKey = buildIdentityKey(tid, code);
                }
                if (identityKey.isEmpty() || uniqueItems.containsKey(identityKey)) {
                    continue;
                }

                uniqueItems.put(identityKey, new CheckoutAssetItem(
                        identityKey,
                        tid,
                        code,
                        CsvReader.getValue(headerIndex, row, "asset_name"),
                        CsvReader.getValue(headerIndex, row, "asset_type"),
                        CsvReader.getValue(headerIndex, row, "serial_number"),
                        CsvReader.getValue(headerIndex, row, "asset_department"),
                        CsvReader.getValue(headerIndex, row, "assigned_user"),
                        CsvReader.getValue(headerIndex, row, "location"),
                        CsvReader.getValue(headerIndex, row, "scan_source"),
                        parseTimestamp(CsvReader.getValue(headerIndex, row, "scanned_at")),
                        Boolean.parseBoolean(CsvReader.getValue(headerIndex, row, "matched_from_cache"))
                ));
            }
        }

        items.addAll(uniqueItems.values());
        if (formData == null) {
            throw new IOException("File CSV check out khong dung dinh dang IDO Asset.");
        }
        return new ImportedCheckoutData(resolveDisplayName(context, uri), formData, items);
    }

    public File exportCheckIn(Context context, ImportedCheckoutData importedData, List<CheckInResultItem> items) throws IOException {
        CheckOutFormData formData = importedData.getFormData();
        File exportFile = ExportFileManager.resolveExportFile(
                "checkin_" + ExportFileManager.sanitizeFileName(formData.getTicketId()) + "_" + TimeFormatUtils.fileTimestamp() + ".csv"
        );
        String checkinExportedAt = now();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(exportFile, false), StandardCharsets.UTF_8))) {
            CsvWriter.writeUtf8Bom(writer);
            CsvWriter.writeExcelSeparatorHint(writer, ',');
            CsvWriter.writeQuotedLine(writer, ',', CHECKIN_HEADERS);
            for (CheckInResultItem item : items) {
                CsvWriter.writeQuotedLine(writer, ',',
                        CHECKIN_EXPORT_TYPE,
                        EXPORT_VERSION,
                        formData.getTicketId(),
                        importedData.getSourceFileName(),
                        formData.getExportedAt(),
                        checkinExportedAt,
                        formData.getCarrierName(),
                        formData.getDepartment(),
                        formData.getPurpose(),
                        formData.getEventName(),
                        formData.getCheckoutAt(),
                        formData.getExpectedReturnAt(),
                        formData.getApprover(),
                        formData.getNote(),
                        item.getIdentityKey(),
                        item.getStatus().name(),
                        item.getMatchedBy(),
                        String.valueOf(item.isExpectedInTicket()),
                        item.getTid(),
                        item.getCode(),
                        item.getAssetName(),
                        item.getAssetType(),
                        item.getSerialNumber(),
                        item.getAssignedUser(),
                        item.getDepartment(),
                        item.getLocation(),
                        item.getCheckoutScanSource(),
                        formatTimestamp(item.getCheckoutScannedAt()),
                        item.getCheckinScanSource(),
                        formatTimestamp(item.getCheckinScannedAt()),
                        item.getNote()
                );
            }
        }
        ExportFileManager.notifyMediaScanner(context, exportFile);
        return exportFile;
    }

    public String buildIdentityKey(String tid, String code) {
        String normalizedTid = StringUtils.normalizeKey(tid);
        if (!normalizedTid.isEmpty()) {
            return "TID:" + normalizedTid;
        }
        String normalizedCode = StringUtils.normalizeKey(code);
        if (!normalizedCode.isEmpty()) {
            return "CODE:" + normalizedCode;
        }
        return "";
    }

    private boolean isExcelSeparatorHint(String line) {
        String cleaned = CsvReader.cleanCell(line).toLowerCase(Locale.ROOT);
        return cleaned.startsWith("sep=");
    }

    private char resolveDelimiterFromSeparatorHint(String line) {
        String cleaned = CsvReader.cleanCell(line);
        return cleaned.length() > 4 ? cleaned.charAt(4) : ';';
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) {
            return "";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(EXPORT_DATE_FORMAT);
    }

    private long parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            LocalDateTime parsed = LocalDateTime.parse(value.trim(), EXPORT_DATE_FORMAT);
            return parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return 0L;
        }
    }

    private String resolveDisplayName(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String name = cursor.getString(nameIndex);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        String lastSegment = uri == null ? "" : uri.getLastPathSegment();
        return lastSegment == null ? "checkout_import.csv" : lastSegment;
    }

    private String buildCheckoutExportFileName(CheckOutFormData formData) {
        String[] segments = new String[]{
                "IDO_CHECKOUT",
                ExportFileManager.sanitizeFileNameSegment(formData == null ? "" : formData.getDepartment()),
                ExportFileManager.sanitizeFileNameSegment(formData == null ? "" : formData.getCarrierName()),
                ExportFileManager.sanitizeFileNameSegment(formData == null ? "" : formData.getCheckoutAt()),
                ExportFileManager.sanitizeFileNameSegment(formData == null ? "" : formData.getExpectedReturnAt()),
                ExportFileManager.sanitizeFileNameSegment(formData == null ? "" : formData.getEventName())
        };
        return String.join("_", segments) + ".csv";
    }
}
