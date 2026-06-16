package com.jiber.backend.publicdata;

public record CanonicalApartmentUpsertDecision(
        boolean eligible,
        String reason
) {
}
