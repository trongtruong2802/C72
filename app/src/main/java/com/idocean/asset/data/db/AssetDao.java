package com.idocean.asset.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.idocean.asset.model.Asset;

import java.util.List;

@Dao
public interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceAll(List<Asset> assets);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(Asset asset);

    @Query("SELECT * FROM assets")
    List<Asset> getAll();

    @Query("SELECT COUNT(*) FROM assets")
    int count();

    @Query("DELETE FROM assets")
    void clear();

    @Query("SELECT * FROM assets WHERE assetCode = :assetCode LIMIT 1")
    Asset getByCode(String assetCode);

    @Query("SELECT * FROM assets WHERE tid = :tid LIMIT 1")
    Asset getByTid(String tid);
}
