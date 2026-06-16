package com.jiber.backend.publicdata;

public record PublicDataImportCommand(
        boolean dryRun,
        Integer limit
) {
}
