package com.shawarma_moods.delivery.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pickup_orders")
public class PickupOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "woocommerce_order_id", unique = true)
    private String woocommerceOrderId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "items_description", length = 1000)
    private String itemsDescription;

    @Column(name = "total_cents")
    private Integer totalCents;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PickupStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = PickupStatus.RECEIVED;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getWoocommerceOrderId() { return woocommerceOrderId; }
    public void setWoocommerceOrderId(String v) { this.woocommerceOrderId = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String v) { this.customerPhone = v; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String v) { this.customerEmail = v; }
    public String getItemsDescription() { return itemsDescription; }
    public void setItemsDescription(String v) { this.itemsDescription = v; }
    public Integer getTotalCents() { return totalCents; }
    public void setTotalCents(Integer v) { this.totalCents = v; }
    public PickupStatus getStatus() { return status; }
    public void setStatus(PickupStatus v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
