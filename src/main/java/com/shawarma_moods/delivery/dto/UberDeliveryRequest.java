package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UberDeliveryRequest {

    @JsonProperty("quote_id")
    private String quoteId;

    @JsonProperty("pickup_name")
    private String pickupName;

    @JsonProperty("pickup_address")
    private String pickupAddress;

    @JsonProperty("pickup_phone_number")
    private String pickupPhoneNumber;

    @JsonProperty("dropoff_name")
    private String dropoffName;

    @JsonProperty("dropoff_address")
    private String dropoffAddress;

    @JsonProperty("dropoff_phone_number")
    private String dropoffPhoneNumber;

    @JsonProperty("manifest_items")
    private List<UberManifestItem> manifestItems;

    @JsonProperty("manifest_total_value")
    private Integer manifestTotalValue;

    @JsonProperty("manifest_reference")
    private String manifestReference;

    @JsonProperty("dropoff_notes")
    private String dropoffNotes;

    // Getters and setters
    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }

    public String getPickupName() { return pickupName; }
    public void setPickupName(String pickupName) { this.pickupName = pickupName; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getPickupPhoneNumber() { return pickupPhoneNumber; }
    public void setPickupPhoneNumber(String pickupPhoneNumber) { this.pickupPhoneNumber = pickupPhoneNumber; }

    public String getDropoffName() { return dropoffName; }
    public void setDropoffName(String dropoffName) { this.dropoffName = dropoffName; }

    public String getDropoffAddress() { return dropoffAddress; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }

    public String getDropoffPhoneNumber() { return dropoffPhoneNumber; }
    public void setDropoffPhoneNumber(String dropoffPhoneNumber) { this.dropoffPhoneNumber = dropoffPhoneNumber; }

    public List<UberManifestItem> getManifestItems() {
        return manifestItems;
    }
    public void setManifestItems(List<UberManifestItem> manifestItems) {
        this.manifestItems = manifestItems;
    }

    public Integer getManifestTotalValue() { return manifestTotalValue; }
    public void setManifestTotalValue(Integer manifestTotalValue) { this.manifestTotalValue = manifestTotalValue; }

    public String getManifestReference() { return manifestReference; }
    public void setManifestReference(String manifestReference) { this.manifestReference = manifestReference; }

    public String getDropoffNotes() { return dropoffNotes; }
    public void setDropoffNotes(String dropoffNotes) { this.dropoffNotes = dropoffNotes; }
}
