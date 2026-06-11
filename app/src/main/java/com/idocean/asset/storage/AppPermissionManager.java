package com.idocean.asset.storage;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

/**
 * Gom logic kiem tra quyen ghi file cho cac man export.
 */
public final class AppPermissionManager {
    private AppPermissionManager() {
    }

    public static boolean hasExportStoragePermission(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static Intent buildManageAllFilesAccessIntent(Context context) {
        Intent intent;
        try {
            intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            if (context != null) {
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            }
        } catch (Exception exception) {
            intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
        }
        return intent;
    }
}
