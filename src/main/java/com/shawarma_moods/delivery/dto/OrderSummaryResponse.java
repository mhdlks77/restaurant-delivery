package com.shawarma_moods.delivery.dto;

import com.shawarma_moods.delivery.model.DeliveryOrder;
import com.shawarma_moods.delivery.model.PickupOrder;

import java.time.Instant;

public class OrderSummaryResponse {

    private String id;
    private String woocommerceOrderId;
    private String type;           // "DELIVERY" or "PICKUP"
    private String status;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String itemsDescription;
    private Integer totalCents;
    private Instant estimatedReadyAt; // only for pickup orders in PREPARING status
    private Instant createdAt;
    private Instant updatedAt;
    private String customerAddress; // only for delivery

    // --- Static factory methods ---

    public static OrderSummaryResponse fromDelivery(DeliveryOrder o) {
        OrderSummaryResponse r = new OrderSummaryResponse();
        r.id = "D-" + o.getId();
        r.woocommerceOrderId = o.getWoocommerceOrderId();
        r.type = "DELIVERY";
        r.status = o.getStatus().name();
        r.customerName = o.getCustomerName();
        r.customerPhone = o.getCustomerPhone();
        r.customerAddress = o.getCustomerAddress();
        r.itemsDescription = o.getItemsDescription();
        r.totalCents = o.getTotalCents();
        r.createdAt = o.getCreatedAt();
        r.updatedAt = o.getUpdatedAt();
        return r;
    }

    public static OrderSummaryResponse fromPickup(PickupOrder o) {
        OrderSummaryResponse r = new OrderSummaryResponse();
        r.id = "P-" + o.getId();
        r.woocommerceOrderId = o.getWoocommerceOrderId();
        r.type = "PICKUP";
        r.status = o.getStatus().name();
        r.customerName = o.getCustomerName();
        r.customerPhone = o.getCustomerPhone();
        r.customerEmail = o.getCustomerEmail();
        r.itemsDescription = o.getItemsDescription();
        r.totalCents = o.getTotalCents();
        r.estimatedReadyAt = o.getEstimatedReadyAt();
        r.createdAt = o.getCreatedAt();
        r.updatedAt = o.getUpdatedAt();
        return r;
    }

    // Getters
    public String getId()                 { return id; }
    public String getWoocommerceOrderId() { return woocommerceOrderId; }
    public String getType()               { return type; }
    public String getStatus()             { return status; }
    public String getCustomerName()       { return customerName; }
    public String getCustomerPhone()      { return customerPhone; }
    public String getCustomerEmail()      { return customerEmail; }
    public String getItemsDescription()   { return itemsDescription; }
    public Integer getTotalCents()        { return totalCents; }
    public Instant getEstimatedReadyAt()  { return estimatedReadyAt; }
    public Instant getCreatedAt()         { return createdAt; }
    public Instant getUpdatedAt()         { return updatedAt; }
    public String getCustomerAddress()    { return customerAddress; }
}
