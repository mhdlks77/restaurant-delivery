package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WooCommerceOrder {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("status")
    private String status;

    @JsonProperty("billing")
    private Address billing;

    @JsonProperty("shipping")
    private Address shipping;

    @JsonProperty("line_items")
    private List<LineItem> lineItems;

    @JsonProperty("meta_data")
    private List<MetaData> metaData;

    @JsonProperty("total")
    private String total;

    // --- Nested classes ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        @JsonProperty("first_name") private String firstName;
        @JsonProperty("last_name")  private String lastName;
        @JsonProperty("address_1")  private String address1;
        @JsonProperty("address_2")  private String address2;
        @JsonProperty("city")       private String city;
        @JsonProperty("postcode")   private String postcode;
        @JsonProperty("country")    private String country;
        @JsonProperty("phone")      private String phone;
        @JsonProperty("email")      private String email;

        public String getFullName() { return firstName + " " + lastName; }
        public String getFullAddress() {
            String addr = address1;
            if (address2 != null && !address2.isBlank()) addr += " " + address2;
            return addr + ", " + city + ", " + postcode + ", " + country;
        }

        public String getFirstName() { return firstName; }
        public String getLastName()  { return lastName; }
        public String getAddress1()  { return address1; }
        public String getCity()      { return city; }
        public String getPostcode()  { return postcode; }
        public String getPhone()     { return phone; }
        public String getEmail()     { return email; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineItem {
        @JsonProperty("name")     private String name;
        @JsonProperty("quantity") private Integer quantity;
        @JsonProperty("price")    private String price;

        public String getName()     { return name; }
        public Integer getQuantity(){ return quantity; }
        public String getPrice()    { return price; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaData {
        @JsonProperty("key")   private String key;
        @JsonProperty("value") private Object value;

        public String getKey()   { return key; }
        public String getValue() { return value != null ? value.toString() : null; }
    }

    // --- Helpers ---

    /**
     * Extracts a metadata value by key.
     * Used to read order_type and location_id set by the React frontend.
     */
    public String getMetaValue(String key) {
        if (metaData == null) return null;
        return metaData.stream()
                .filter(m -> key.equals(m.getKey()))
                .map(MetaData::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Builds a human-readable manifest string from line items.
     * Used as the Uber Direct manifest description.
     * Example: "2x Burger, 1x Fries, 3x Coke"
     */
    public String buildManifestDescription() {
        if (lineItems == null || lineItems.isEmpty()) return "Food order";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineItems.size(); i++) {
            LineItem item = lineItems.get(i);
            sb.append(item.getQuantity()).append("x ").append(item.getName());
            if (i < lineItems.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    /**
     * Converts total string to cents (integer).
     * WooCommerce sends total as "25.00", Uber expects 2500.
     */
    public Integer getTotalCents() {
        try {
            return (int) (Double.parseDouble(total) * 100);
        } catch (Exception e) {
            return 0;
        }
    }

    public Long getId()                  { return id; }
    public String getStatus()            { return status; }
    public Address getBilling()          { return billing; }
    public Address getShipping()         { return shipping; }
    public List<LineItem> getLineItems() { return lineItems; }
    public String getTotal()             { return total; }
}
