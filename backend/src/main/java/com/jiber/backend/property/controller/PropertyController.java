package com.jiber.backend.property.controller;

import com.jiber.backend.auth.dto.AuthUserPrincipal;
import com.jiber.backend.property.dto.MapSearchRequest;
import com.jiber.backend.property.dto.NewApartmentAddressSearchResponse;
import com.jiber.backend.property.dto.NewApartmentAnalysisRequest;
import com.jiber.backend.property.dto.NewApartmentAnalysisResponse;
import com.jiber.backend.property.dto.PropertyDetailResponse;
import com.jiber.backend.property.dto.PropertyMapResponse;
import com.jiber.backend.property.dto.PropertySearchRequest;
import com.jiber.backend.property.dto.PropertySearchResponse;
import com.jiber.backend.property.dto.ShapRequest;
import com.jiber.backend.property.dto.ShapResponse;
import com.jiber.backend.property.dto.ValuationRequest;
import com.jiber.backend.property.dto.ValuationResponse;
import com.jiber.backend.property.service.PropertyAddressSearchService;
import com.jiber.backend.property.service.PropertyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final PropertyAddressSearchService addressSearchService;

    public PropertyController(PropertyService propertyService, PropertyAddressSearchService addressSearchService) {
        this.propertyService = propertyService;
        this.addressSearchService = addressSearchService;
    }

    @GetMapping("/map")
    public PropertyMapResponse mapSearch(@Valid @ParameterObject @ModelAttribute MapSearchRequest request) {
        request.validateRanges();
        return propertyService.findMapProperties(request);
    }

    @GetMapping("/search")
    public PropertySearchResponse filterSearch(@Valid @ParameterObject @ModelAttribute PropertySearchRequest request) {
        request.validateRanges();
        return propertyService.searchProperties(request);
    }

    @GetMapping("/new-analysis/address-search")
    public List<NewApartmentAddressSearchResponse> newApartmentAddressSearch(
            @RequestParam @NotBlank @Size(min = 2, max = 120) String query
    ) {
        return addressSearchService.searchNewApartmentAddresses(query);
    }

    @GetMapping("/{propertyId}")
    public PropertyDetailResponse detail(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return propertyService.getPropertyDetail(propertyId, principal);
    }

    @PostMapping("/{propertyId}/valuation")
    public ValuationResponse valuation(
            @PathVariable Long propertyId,
            @Valid @RequestBody ValuationRequest request
    ) {
        return propertyService.valuateApartment(propertyId, request);
    }

    @PostMapping("/{propertyId}/shap")
    public ShapResponse shap(
            @PathVariable Long propertyId,
            @Valid @RequestBody ShapRequest request
    ) {
        return propertyService.explainApartment(propertyId, request);
    }

    @PostMapping("/new-analysis")
    public NewApartmentAnalysisResponse analyzeNewApartment(
            @Valid @RequestBody NewApartmentAnalysisRequest request
    ) {
        return propertyService.analyzeNewApartment(request);
    }
}
