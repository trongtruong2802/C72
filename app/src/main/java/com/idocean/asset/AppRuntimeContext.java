package com.idocean.asset;

import android.content.Context;

/**
 * Giu application context de cac helper runtime dung lai an toan,
 * khong phu thuoc vao custom Application luc khoi dong.
 */
public final class AppRuntimeContext {
    private static volatile Context appContext;

    private AppRuntimeContext() {
    }

    public static void init(Context context) {
        if (context == null) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        appContext = applicationContext == null ? context : applicationContext;
    }

    public static Context get() {
        return appContext;
    }
}
