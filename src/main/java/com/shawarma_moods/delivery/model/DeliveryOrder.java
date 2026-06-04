package com.shawarma_moods.delivery.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "delivery_orders")
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "woocommerce_order_id")
    private String woocommerceOrderId;

    @Column(name = "uber_delivery_id")
    private String uberDeliveryId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_address")
    private String customerAddress;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "order_type")
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Column(name = "tracking_url")
    private String trackingUrl;

    @Column(name = "items_description", columnDefinition = "TEXT")
    private String itemsDescription;

    @Column(name = "total_cents")
    private Integer totalCents;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist(){
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getWoocommerceOrderId() {
        return woocommerceOrderId;
    }
    public void setWoocommerceOrderId(String woocommerceOrderId) {
        this.woocommerceOrderId = woocommerceOrderId;
    }

    public String getUberDeliveryId() {
        return uberDeliveryId;
    }
    public void setUberDeliveryId(String uberDeliveryId) {
        this.uberDeliveryId = uberDeliveryId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }
    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }
    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public OrderType getOrderType() {
        return orderType;
    }
    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }
    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public String getItemsDescription() {
        return itemsDescription;
    }
    public void setItemsDescription(String itemsDescription) {
        this.itemsDescription = itemsDescription;
    }

    public Integer getTotalCents() {
        return totalCents;
    }
    public void setTotalCents(Integer totalCents) {
        this.totalCents = totalCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
