package com.jiber.backend.publicdata;

public enum PublicDataApiType {
    SALE("/1613000/RTMSDataSvcAptTradeDev/getRTMSDataSvcAptTradeDev"),
    RENT("/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent");

    private final String endpointPath;

    PublicDataApiType(String endpointPath) {
        this.endpointPath = endpointPath;
    }

    public String endpointPath() {
        return endpointPath;
    }
}
