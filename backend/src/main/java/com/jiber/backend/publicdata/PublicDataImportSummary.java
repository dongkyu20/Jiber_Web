package com.jiber.backend.publicdata;

public record PublicDataImportSummary(
        boolean dryRun,
        int fetchedCount,
        int stagedCount,
        int geocodedCount,
        int failedCount
) {
    public static PublicDataImportSummary empty(boolean dryRun) {
        return new PublicDataImportSummary(dryRun, 0, 0, 0, 0);
    }

    public PublicDataImportSummary addFetched(int count) {
        return new PublicDataImportSummary(dryRun, fetchedCount + count, stagedCount, geocodedCount, failedCount);
    }

    public PublicDataImportSummary addStaged() {
        return new PublicDataImportSummary(dryRun, fetchedCount, stagedCount + 1, geocodedCount, failedCount);
    }

    public PublicDataImportSummary addGeocoded() {
        return new PublicDataImportSummary(dryRun, fetchedCount, stagedCount, geocodedCount + 1, failedCount);
    }

    public PublicDataImportSummary addFailure() {
        return new PublicDataImportSummary(dryRun, fetchedCount, stagedCount, geocodedCount, failedCount + 1);
    }

    public boolean reachedLimit(Integer limit) {
        return limit != null && limit > 0 && stagedCount >= limit;
    }
}
