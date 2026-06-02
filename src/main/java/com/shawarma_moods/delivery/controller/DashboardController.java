package com.shawarma_moods.delivery.controller;

import com.shawarma_moods.delivery.dto.OrderSummaryResponse;
import com.shawarma_moods.delivery.model.DeliveryStatus;
import com.shawarma_moods.delivery.model.PickupOrder;
import com.shawarma_moods.delivery.model.PickupStatus;
import com.shawarma_moods.delivery.repository.DeliveryOrderRepository;
import com.shawarma_moods.delivery.repository.PickupOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PickupOrderRepository pickupOrderRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;

    public DashboardController(PickupOrderRepository pickupOrderRepository,
                               DeliveryOrderRepository deliveryOrderRepository) {
        this.pickupOrderRepository = pickupOrderRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderSummaryResponse>> getOrders() {
        List<OrderSummaryResponse> deliveries = deliveryOrderRepository
                .findAll()
                .stream()
                .map(OrderSummaryResponse::fromDelivery)
                .toList();

        List<OrderSummaryResponse> pickups = pickupOrderRepository
                .findAll()
                .stream()
                .map(OrderSummaryResponse::fromPickup)
                .toList();

        List<OrderSummaryResponse> all = Stream
                .concat(deliveries.stream(), pickups.stream())
                .sorted(Comparator.comparing(OrderSummaryResponse::getCreatedAt).reversed())
                .toList();

        return ResponseEntity.ok(all);
    }

    /**
     * Accepts an order — sets status to PREPARING with an estimated ready time.
     * Works for both delivery and pickup orders.
     */
    @PostMapping("/orders/{orderId}/accept")
    public ResponseEntity<Void> acceptOrder(
            @PathVariable String orderId,
            @RequestParam int estimatedMinutes
    ) {
        if (orderId.startsWith("P-")) {
            Long id = Long.parseLong(orderId.substring(2));
            pickupOrderRepository.findById(id).ifPresent(order -> {
                order.setStatus(PickupStatus.PREPARING);
                pickupOrderRepository.save(order);
            });
        } else if (orderId.startsWith("D-")) {
            Long id = Long.parseLong(orderId.substring(2));
            deliveryOrderRepository.findById(id).ifPresent(order -> {
                order.setStatus(DeliveryStatus.PICKUP);
                deliveryOrderRepository.save(order);
            });
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Marks an order as ready.
     * Pickup → READY (customer can come collect)
     * Delivery → READY_FOR_PICKUP (waiting for Uber driver)
     */
    @PostMapping("/orders/{orderId}/ready")
    public ResponseEntity<Void> markReady(@PathVariable String orderId) {
        if (orderId.startsWith("P-")) {
            Long id = Long.parseLong(orderId.substring(2));
            pickupOrderRepository.findById(id).ifPresent(order -> {
                order.setStatus(PickupStatus.READY);
                pickupOrderRepository.save(order);
            });
        } else if (orderId.startsWith("D-")) {
            Long id = Long.parseLong(orderId.substring(2));
            deliveryOrderRepository.findById(id).ifPresent(order -> {
                order.setStatus(DeliveryStatus.READY_FOR_PICKUP);
                deliveryOrderRepository.save(order);
            });
        }
        return ResponseEntity.ok().build();
    }


}
