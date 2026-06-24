package com.jiber.backend.property.service;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import com.jiber.backend.property.dto.PropertyType;
import org.springframework.stereotype.Service;

@Service
public class PropertyAiEligibilityService {

    public PropertyType ensureApartmentSupported(PropertyType propertyType) {
        if (propertyType != PropertyType.APARTMENT) {
            throw new ApiException(ErrorCode.VALUATION_UNSUPPORTED_PROPERTY_TYPE);
        }
        return propertyType;
    }
}
