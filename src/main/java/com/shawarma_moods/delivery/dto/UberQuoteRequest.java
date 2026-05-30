package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UberQuoteRequest {

    @JsonProperty("pickup_address")
    private String pickupAddress;

    @JsonProperty("dropoff_address")
    private String dropoffAddress;

    public UberQuoteRequest(String pickupAddress, String dropoffAddress) {
        this.pickupAddress = pickupAddress;
        this.dropoffAddress = dropoffAddress;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }
}
