package com.shawarma_moods.delivery.model;

public enum DeliveryStatus {
    PENDING,          // unused — reserved
    CREATED,          // order received, awaiting staff acceptance
    PREPARING,        // staff accepted — kitchen is preparing the food
    READY_FOR_PICKUP, // kitchen done — awaiting Uber driver
    PICKUP,           // Uber driver at restaurant (delivery.pickup webhook)
    DROPOFF,          // Uber driver en route to customer (delivery.dropoff webhook)
    DELIVERED,        // delivered to customer (delivery.delivered webhook)
    FAILED,           // delivery failed (delivery.failed webhook)
    RETURNED,         // order returned to restaurant (delivery.returned webhook)
    CANCELLED         // cancelled (delivery.cancelled webhook)
}
