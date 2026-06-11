package com.idocean.asset.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.idocean.asset.data.dto.SessionDto;
import com.idocean.asset.model.SessionConfig;

/**
 * Luu cau hinh phien lam viec tam thoi tren may.
 */
public class SessionRepository {
    private static final String PREF_NAME = "ido_asset_session";
    private static final String KEY_OPERATOR = "operator_name";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_NOTE = "session_note";
    private static final String KEY_MANUAL_INPUT = "manual_entry_each_session";

    private final SharedPreferences preferences;

    public SessionRepository(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public SessionConfig getSession() {
        SessionDto dto = new SessionDto(
                preferences.getString(KEY_OPERATOR, "Nhân viên kho"),
                preferences.getString(KEY_DEPARTMENT, "IT"),
                preferences.getString(KEY_NOTE, ""),
                preferences.getBoolean(KEY_MANUAL_INPUT, false)
        );
        return new SessionConfig(dto.operatorName, dto.department, dto.sessionNote, dto.manualEntryEachSession);
    }

    public void saveSession(SessionConfig config) {
        preferences.edit()
                .putString(KEY_OPERATOR, config.getOperatorName())
                .putString(KEY_DEPARTMENT, config.getDepartment())
                .putString(KEY_NOTE, config.getSessionNote())
                .putBoolean(KEY_MANUAL_INPUT, config.isManualEntryEachSession())
                .apply();
    }
}
