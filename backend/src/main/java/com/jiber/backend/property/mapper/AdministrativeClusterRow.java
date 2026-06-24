package com.jiber.backend.property.mapper;

import java.math.BigDecimal;

public class AdministrativeClusterRow {

    private String sido;
    private String sigungu;
    private String legalDong;
    private String label;
    private BigDecimal centerLat;
    private BigDecimal centerLng;
    private Integer propertyCount;
    private Integer transactionCount;
    private Long averageDealAmount;

    public String getSido() {
        return sido;
    }

    public void setSido(String sido) {
        this.sido = sido;
    }

    public String getSigungu() {
        return sigungu;
    }

    public void setSigungu(String sigungu) {
        this.sigungu = sigungu;
    }

    public String getLegalDong() {
        return legalDong;
    }

    public void setLegalDong(String legalDong) {
        this.legalDong = legalDong;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(BigDecimal centerLat) {
        this.centerLat = centerLat;
    }

    public BigDecimal getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(BigDecimal centerLng) {
        this.centerLng = centerLng;
    }

    public Integer getPropertyCount() {
        return propertyCount;
    }

    public void setPropertyCount(Integer propertyCount) {
        this.propertyCount = propertyCount;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Long getAverageDealAmount() {
        return averageDealAmount;
    }

    public void setAverageDealAmount(Long averageDealAmount) {
        this.averageDealAmount = averageDealAmount;
    }
}
