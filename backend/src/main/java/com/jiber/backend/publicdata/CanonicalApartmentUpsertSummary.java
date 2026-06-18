package com.jiber.backend.publicdata;

public record CanonicalApartmentUpsertSummary(
        int processedCount,
        int propertyCreatedCount,
        int transactionCreatedCount,
        int transactionSkippedCount,
        int skippedCount,
        int failedCount
) {
    public static CanonicalApartmentUpsertSummary empty() {
        return new CanonicalApartmentUpsertSummary(0, 0, 0, 0, 0, 0);
    }

    public CanonicalApartmentUpsertSummary addProcessed() {
        return new CanonicalApartmentUpsertSummary(
                processedCount + 1,
                propertyCreatedCount,
                transactionCreatedCount,
                transactionSkippedCount,
                skippedCount,
                failedCount
        );
    }

    public CanonicalApartmentUpsertSummary addPropertyCreated() {
        return new CanonicalApartmentUpsertSummary(
                processedCount,
                propertyCreatedCount + 1,
                transactionCreatedCount,
                transactionSkippedCount,
                skippedCount,
                failedCount
        );
    }

    public CanonicalApartmentUpsertSummary addTransactionCreated() {
        return new CanonicalApartmentUpsertSummary(
                processedCount,
                propertyCreatedCount,
                transactionCreatedCount + 1,
                transactionSkippedCount,
                skippedCount,
                failedCount
        );
    }

    public CanonicalApartmentUpsertSummary addTransactionSkipped() {
        return new CanonicalApartmentUpsertSummary(
                processedCount,
                propertyCreatedCount,
                transactionCreatedCount,
                transactionSkippedCount + 1,
                skippedCount,
                failedCount
        );
    }

    public CanonicalApartmentUpsertSummary addSkipped() {
        return new CanonicalApartmentUpsertSummary(
                processedCount,
                propertyCreatedCount,
                transactionCreatedCount,
                transactionSkippedCount,
                skippedCount + 1,
                failedCount
        );
    }

    public CanonicalApartmentUpsertSummary addFailed() {
        return new CanonicalApartmentUpsertSummary(
                processedCount,
                propertyCreatedCount,
                transactionCreatedCount,
                transactionSkippedCount,
                skippedCount,
                failedCount + 1
        );
    }
}
