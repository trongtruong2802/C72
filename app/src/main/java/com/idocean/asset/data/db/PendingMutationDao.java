package com.idocean.asset.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingMutationDao {
    @Insert
    void insert(PendingMutation mutation);

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt ASC")
    List<PendingMutation> getAllPending();

    @Delete
    void delete(PendingMutation mutation);

    @Query("DELETE FROM pending_mutations")
    void clear();

    @Query("SELECT COUNT(*) FROM pending_mutations")
    int count();
}
