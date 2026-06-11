package com.idocean.asset.data.repository;

/**
 * Runtime metrics de dashboard doc nhanh tu cac man nghiep vu.
 */
public class DashboardMetricsRepository {
    private static DashboardMetricsRepository instance;

    private boolean inventorySummaryReady;
    private int expectedCount;
    private int checkedCount;
    private int missingCount;
    private int outsideCount;
    private long updatedAt;

    private DashboardMetricsRepository() {
    }

    public static synchronized DashboardMetricsRepository getInstance() {
        if (instance == null) {
            instance = new DashboardMetricsRepository();
        }
        return instance;
    }

    public synchronized void updateInventorySummary(int expectedCount, int checkedCount, int missingCount, int outsideCount) {
        this.inventorySummaryReady = true;
        this.expectedCount = Math.max(0, expectedCount);
        this.checkedCount = Math.max(0, checkedCount);
        this.missingCount = Math.max(0, missingCount);
        this.outsideCount = Math.max(0, outsideCount);
        this.updatedAt = System.currentTimeMillis();
    }

    public synchronized boolean hasInventorySummary() {
        return inventorySummaryReady;
    }

    public synchronized int getExpectedCount() {
        return expectedCount;
    }

    public synchronized int getCheckedCount() {
        return checkedCount;
    }

    public synchronized int getMissingCount() {
        return missingCount;
    }

    public synchronized int getOutsideCount() {
        return outsideCount;
    }

    public synchronized long getUpdatedAt() {
        return updatedAt;
    }

    public synchronized void clear() {
        inventorySummaryReady = false;
        expectedCount = 0;
        checkedCount = 0;
        missingCount = 0;
        outsideCount = 0;
        updatedAt = 0L;
    }
}
