package com.idocean.asset.config;

import com.idocean.asset.BuildConfig;
import com.idocean.asset.data.io.ExportFileManager;

import java.io.File;

/**
 * Cấu hình dùng chung cho toàn app.
 */
public final class AppConfig {
    public static final String BASE_URL = BuildConfig.BASE_URL;
    public static final String API_KEY = BuildConfig.API_KEY;
    public static final String EXPORT_FOLDER_NAME = "IDO Asset";

    private AppConfig() {
    }

    public static File getExportDirectory() {
        return ExportFileManager.getExportDirectory();
    }

    public static boolean hasApiKey() {
        return API_KEY != null && !API_KEY.trim().isEmpty();
    }
}
