package com.jiber.backend.property;

import java.util.List;
import java.util.Optional;

public interface ApartmentComplexHouseholdLookup {

    default Optional<Integer> findHouseholdCount(PropertyDetailRow property) {
        return findHouseholdCount(property, List.of());
    }

    Optional<Integer> findHouseholdCount(PropertyDetailRow property, List<String> apartmentNameHints);
}
