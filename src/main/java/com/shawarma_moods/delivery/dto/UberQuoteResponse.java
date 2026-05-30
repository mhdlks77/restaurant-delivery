package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UberQuoteResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("fee")
    private Integer fee;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("estimated_pickup_time")
    private Long estimatedPickupTime;

    @JsonProperty("estimated_dropoff_time")
    private Long estimatedDropoffTime;

    public String getId() { return id; }
    public Integer getFee() { return fee; }
    public String getCurrency() { return currency; }
    public Long getEstimatedPickupTime() { return estimatedPickupTime; }
    public Long getEstimatedDropoffTime() { return estimatedDropoffTime; }
}
