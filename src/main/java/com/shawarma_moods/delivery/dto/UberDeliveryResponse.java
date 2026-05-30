package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UberDeliveryResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("tracking_url")
    private String trackingUrl;

    @JsonProperty("fee")
    private Integer fee;

    @JsonProperty("currency")
    private String currency;

    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getTrackingUrl() { return trackingUrl; }
    public Integer getFee() { return fee; }
    public String getCurrency() { return currency; }
}
