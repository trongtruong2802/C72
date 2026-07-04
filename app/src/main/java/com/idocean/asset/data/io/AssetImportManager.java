package com.idocean.asset.data.io;

import android.content.Context;
import android.net.Uri;

import com.idocean.asset.data.mapper.AssetMapper;
import com.idocean.asset.model.Asset;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Importer CSV chuẩn hóa dữ liệu về model nội bộ Asset.
 */
public class AssetImportManager {

    public List<Asset> importFromUri(Context context, Uri uri) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException("Chưa chọn file CSV để import.");
        }

        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IllegalArgumentException("Không thể mở file CSV đã chọn.");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String> rawLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    rawLines.add(line);
                }
            }

            if (rawLines.isEmpty()) {
                throw new IllegalArgumentException("File CSV đang trống.");
            }

            char delimiter = CsvReader.resolveDelimiter(rawLines.get(0));
            List<String> headers = CsvReader.parseDelimitedLine(rawLines.get(0), delimiter);
            if (headers.isEmpty()) {
                throw new IllegalArgumentException("Không đọc được header CSV.");
            }

            List<String> normalizedHeaders = new ArrayList<>();
            for (String header : headers) {
                normalizedHeaders.add(AssetMapper.normalizeHeader(header));
            }

            List<Asset> assets = new ArrayList<>();
            for (int index = 1; index < rawLines.size(); index++) {
                List<String> values = CsvReader.parseDelimitedLine(rawLines.get(index), delimiter);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < normalizedHeaders.size(); i++) {
                    String value = i < values.size() ? values.get(i) : "";
                    row.put(normalizedHeaders.get(i), value.trim());
                }
                Asset asset = AssetMapper.fromCsv(row, index);
                if (!asset.getAssetCode().isEmpty() || !asset.getAssetName().isEmpty() || !asset.getTid().isEmpty()) {
                    assets.add(asset);
                }
            }
            return assets;
        }
    }
}
