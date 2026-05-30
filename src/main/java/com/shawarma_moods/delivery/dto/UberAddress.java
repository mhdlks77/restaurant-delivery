package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UberAddress {

    @JsonProperty("street_address")
    private String streetAddress;

    @JsonProperty("city")
    private String city;

    @JsonProperty("state")
    private String state;

    @JsonProperty("zip_code")
    private String zipcode;

    @JsonProperty("country")
    private String country;

    public UberAddress() {}

    public UberAddress(String streetAddress, String city, String state, String zipcode, String country){
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
    }

    public String getStreetAddress() {
        return streetAddress;
    }
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }

    public String getZipcode() {
        return zipcode;
    }
    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
}
