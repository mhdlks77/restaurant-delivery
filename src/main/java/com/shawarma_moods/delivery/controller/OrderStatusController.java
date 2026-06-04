package com.shawarma_moods.delivery.controller;

import com.shawarma_moods.delivery.dto.OrderStatusResponse;
import com.shawarma_moods.delivery.model.DeliveryOrder;
import com.shawarma_moods.delivery.model.DeliveryStatus;
import com.shawarma_moods.delivery.model.PickupOrder;
import com.shawarma_moods.delivery.model.PickupStatus;
import com.shawarma_moods.delivery.repository.DeliveryOrderRepository;
import com.shawarma_moods.delivery.repository.PickupOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderStatusController {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final PickupOrderRepository pickupOrderRepository;

    public OrderStatusController(DeliveryOrderRepository deliveryOrderRepository,
                                 PickupOrderRepository pickupOrderRepository) {
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.pickupOrderRepository = pickupOrderRepository;
    }

    // Public endpoint - polled by the customer status page every 10 seconds
    // Looks up by wooCommerce ID across bothe delivery and pickup orders tables

    @GetMapping("/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> getOrdrrStatus(@PathVariable String orderId) {

        //Check delivery orders
        Optional<DeliveryOrder> deliveryOrder = deliveryOrderRepository
                .findByWoocommerceOrderId(orderId);

        if(deliveryOrder.isPresent()){
            return ResponseEntity.ok(mapDeliveryStatus(deliveryOrder.get()));
        }

        //Check pickup orders
        Optional<PickupOrder> pickupOrder = pickupOrderRepository
                .findByWoocommerceOrderId(orderId);

        if (pickupOrder.isPresent()){
            return ResponseEntity.ok(mapPickupStatus(pickupOrder.get()));
        }

        return ResponseEntity.ok(OrderStatusResponse.notFound(orderId));
    }

    private OrderStatusResponse mapDeliveryStatus(DeliveryOrder order){
        OrderStatusResponse r = new OrderStatusResponse();
        r.setWoocommerceOrderId(order.getWoocommerceOrderId());
        r.setType("DELIVERY");
        r.setStatus(order.getStatus().name());
        r.setTrackingUrl(order.getTrackingUrl());
        r.setStatusMessage(deliveryStatusMessage(order.getStatus()));
        r.setEstimatedReadyAt(order.getEstimatedReadyAt());
        return r;
    }

    private OrderStatusResponse mapPickupStatus(PickupOrder order) {
        OrderStatusResponse r = new OrderStatusResponse();
        r.setWoocommerceOrderId(order.getWoocommerceOrderId());
        r.setType("PICKUP");
        r.setStatus(order.getStatus().name());
        r.setStatusMessage(pickupStatusMessage(order.getStatus()));
        r.setEstimatedReadyAt(order.getEstimatedReadyAt());
        return r;
    }

    private String deliveryStatusMessage(DeliveryStatus status) {
        return switch (status) {
            case PENDING          -> "Your order has been received.";
            case CREATED          -> "Your order is confirmed — we're finding a driver.";
            case PREPARING        -> "Your order is being prepared! 🍳";
            case READY_FOR_PICKUP -> "Your order is ready and waiting for the driver.";
            case PICKUP           -> "Your order is on its way! 🚗";
            case DROPOFF          -> "Your driver is nearby!";
            case DELIVERED        -> "Your order has been delivered. Enjoy! 🎉";
            case FAILED           -> "Unfortunately your delivery failed. Please contact us.";
            case RETURNED         -> "Your order was returned to the restaurant. Please contact us.";
            case CANCELLED        -> "Your order was cancelled.";
        };
    }

    private String pickupStatusMessage(PickupStatus status) {
        return switch (status) {
            case RECEIVED   -> "Your order has been received.";
            case PREPARING  -> "We're preparing your order! 🍳";
            case READY      -> "Your order is ready for pickup! 🎉";
            case COMPLETED  -> "Order completed. Thanks for visiting!";
            case CANCELLED  -> "Your order was cancelled.";
        };
    }
}
