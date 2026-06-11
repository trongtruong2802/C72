package com.idocean.asset.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DashboardMetricsRepositoryTest {

    @Test
    public void clear_resetsInventorySummaryState() {
        DashboardMetricsRepository repository = DashboardMetricsRepository.getInstance();
        repository.clear();

        repository.updateInventorySummary(20, 12, 7, 1);

        assertTrue(repository.hasInventorySummary());
        assertEquals(20, repository.getExpectedCount());
        assertEquals(12, repository.getCheckedCount());
        assertEquals(7, repository.getMissingCount());
        assertEquals(1, repository.getOutsideCount());

        repository.clear();

        assertFalse(repository.hasInventorySummary());
        assertEquals(0, repository.getExpectedCount());
        assertEquals(0, repository.getCheckedCount());
        assertEquals(0, repository.getMissingCount());
        assertEquals(0, repository.getOutsideCount());
        assertEquals(0L, repository.getUpdatedAt());
    }
}
