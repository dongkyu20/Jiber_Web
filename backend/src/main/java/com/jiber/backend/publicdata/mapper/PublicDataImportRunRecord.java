package com.jiber.backend.publicdata.mapper;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import java.time.OffsetDateTime;

public class PublicDataImportRunRecord {

    private Long importRunId;
    private String status;
    private Integer targetMonths;
    private String targetRegions;
    private Boolean dryRun;
    private Integer fetchedCount;
    private Integer stagedCount;
    private Integer geocodedCount;
    private Integer failedCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String failureReason;

    public static PublicDataImportRunRecord started(PublicDataImportProperties properties, PublicDataImportCommand command) {
        var record = new PublicDataImportRunRecord();
        record.status = "RUNNING";
        record.targetMonths = properties.importMonths();
        record.targetRegions = properties.targetRegions().toString();
        record.dryRun = command.dryRun();
        record.fetchedCount = 0;
        record.stagedCount = 0;
        record.geocodedCount = 0;
        record.failedCount = 0;
        record.startedAt = OffsetDateTime.now();
        return record;
    }

    public PublicDataImportRunRecord finish(String status, PublicDataImportSummary summary, String failureReason) {
        var record = new PublicDataImportRunRecord();
        record.importRunId = importRunId;
        record.status = status;
        record.targetMonths = targetMonths;
        record.targetRegions = targetRegions;
        record.dryRun = dryRun;
        record.fetchedCount = summary.fetchedCount();
        record.stagedCount = summary.stagedCount();
        record.geocodedCount = summary.geocodedCount();
        record.failedCount = summary.failedCount();
        record.startedAt = startedAt;
        record.finishedAt = OffsetDateTime.now();
        record.failureReason = failureReason;
        return record;
    }

    public Long importRunId() {
        return importRunId;
    }

    public Long getImportRunId() {
        return importRunId;
    }

    public void setImportRunId(Long importRunId) {
        this.importRunId = importRunId;
    }

    public String getStatus() {
        return status;
    }

    public Integer getTargetMonths() {
        return targetMonths;
    }

    public String getTargetRegions() {
        return targetRegions;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public Integer getFetchedCount() {
        return fetchedCount;
    }

    public Integer getStagedCount() {
        return stagedCount;
    }

    public Integer getGeocodedCount() {
        return geocodedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
