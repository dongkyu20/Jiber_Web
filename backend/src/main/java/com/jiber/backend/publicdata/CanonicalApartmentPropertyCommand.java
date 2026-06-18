package com.jiber.backend.publicdata;

import java.math.BigDecimal;

public class CanonicalApartmentPropertyCommand {

    private Long propertyId;
    private String propertyType;
    private String name;
    private String sido;
    private String sigungu;
    private String legalDong;
    private String jibun;
    private String jibunAddress;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer builtYear;
    private String sourceSystem;

    public static CanonicalApartmentPropertyCommand from(CanonicalApartmentRawRow row) {
        var command = new CanonicalApartmentPropertyCommand();
        command.propertyType = "APARTMENT";
        command.name = row.apartmentName();
        command.sido = row.sido();
        command.sigungu = row.sigungu();
        command.legalDong = row.legalDong();
        command.jibun = row.jibun();
        command.jibunAddress = row.fullAddress();
        command.latitude = row.latitude();
        command.longitude = row.longitude();
        command.builtYear = row.builtYear();
        command.sourceSystem = CanonicalApartmentUpsertService.SOURCE_SYSTEM;
        return command;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public String getName() {
        return name;
    }

    public String getSido() {
        return sido;
    }

    public String getSigungu() {
        return sigungu;
    }

    public String getLegalDong() {
        return legalDong;
    }

    public String getJibun() {
        return jibun;
    }

    public String getJibunAddress() {
        return jibunAddress;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Integer getBuiltYear() {
        return builtYear;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }
}
