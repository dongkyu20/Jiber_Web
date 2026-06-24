package com.jiber.backend.publicdata.service;

import com.jiber.backend.publicdata.client.*;
import com.jiber.backend.publicdata.config.*;
import com.jiber.backend.publicdata.dto.*;
import com.jiber.backend.publicdata.mapper.*;
import com.jiber.backend.publicdata.service.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanonicalApartmentUpsertService {

    public static final String SOURCE_SYSTEM = "PUBLIC_DATA_PORTAL";

    private final PublicDataImportMapper importMapper;

    public CanonicalApartmentUpsertService(PublicDataImportMapper importMapper) {
        this.importMapper = importMapper;
    }

    @Transactional
    public CanonicalApartmentUpsertSummary upsertEligibleRawRows(Long importRunId) {
        var summary = CanonicalApartmentUpsertSummary.empty();
        for (var row : importMapper.findCanonicalUpsertCandidates(importRunId)) {
            summary = summary.addProcessed();
            try {
                if (!row.hasSuccessfulGeocoding()) {
                    importMapper.markRawCanonicalSkipped(row.rawTransactionId());
                    summary = summary.addSkipped();
                    continue;
                }
                var propertyResolution = resolveProperty(row);
                if (propertyResolution.created()) {
                    summary = summary.addPropertyCreated();
                }
                var existingTransaction = importMapper.findCanonicalTransactionIdBySourceKey(row.sourceKey());
                if (existingTransaction.isPresent()) {
                    importMapper.markRawCanonicalApplied(row.rawTransactionId());
                    summary = summary.addTransactionSkipped();
                    continue;
                }
                var inserted = importMapper.insertCanonicalApartmentTransaction(
                        CanonicalApartmentTransactionCommand.from(propertyResolution.propertyId(), row)
                );
                importMapper.markRawCanonicalApplied(row.rawTransactionId());
                summary = inserted > 0 ? summary.addTransactionCreated() : summary.addTransactionSkipped();
            } catch (RuntimeException exception) {
                importMapper.markRawCanonicalFailed(row.rawTransactionId());
                summary = summary.addFailed();
            }
        }
        return summary;
    }

    private PropertyResolution resolveProperty(CanonicalApartmentRawRow row) {
        return importMapper.findCanonicalApartmentPropertyId(row)
                .map(propertyId -> new PropertyResolution(propertyId, false))
                .orElseGet(() -> insertProperty(row));
    }

    private PropertyResolution insertProperty(CanonicalApartmentRawRow row) {
        var command = CanonicalApartmentPropertyCommand.from(row);
        importMapper.insertCanonicalApartmentProperty(command);
        if (command.getPropertyId() == null) {
            throw new IllegalStateException("Canonical apartment property insert did not return propertyId");
        }
        return new PropertyResolution(command.getPropertyId(), true);
    }

    private record PropertyResolution(Long propertyId, boolean created) {
    }
}
