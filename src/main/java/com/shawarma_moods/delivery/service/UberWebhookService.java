package com.shawarma_moods.delivery.service;

import com.shawarma_moods.delivery.dto.UberWebhookEvent;
import com.shawarma_moods.delivery.model.DeliveryOrder;
import com.shawarma_moods.delivery.model.DeliveryStatus;
import com.shawarma_moods.delivery.repository.DeliveryOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UberWebhookService {

    private static Logger log = LoggerFactory.getLogger(UberWebhookService.class);

    private final DeliveryOrderRepository deliveryOrderRepository;

    public UberWebhookService(DeliveryOrderRepository deliveryOrderRepository) {
        this.deliveryOrderRepository = deliveryOrderRepository;
    }

    /**
     * Routes an incoming Uber webhook event to the appropriate handler.
     * All event types are logged. Unknown events are acknowledged but not processed.
     */
    public void handleEvent(UberWebhookEvent event) {
        String eventType = event.getEventType();
        String deliveryId = event.getMeta() != null ? event.getMeta().getResourceId() : null;

        log.info("Received Uber webhook: type={}, deliveryId={}, eventId={}",
                eventType, deliveryId, event.getEventId());

        if (deliveryId == null) {
            log.warn("Webhook event {} has no resource_id — skipping", eventType);
            return;
        }

        switch (eventType) {
            case "delivery.created"         -> updateStatus(deliveryId, DeliveryStatus.CREATED);
            case "delivery.pickup_imminent" -> updateStatus(deliveryId, DeliveryStatus.READY_FOR_PICKUP);
            case "delivery.pickup"          -> updateStatus(deliveryId, DeliveryStatus.PICKUP);
            case "delivery.dropoff"         -> updateStatus(deliveryId, DeliveryStatus.DROPOFF);
            case "delivery.delivered"-> updateStatus(deliveryId, DeliveryStatus.DELIVERED);
            case "delivery.failed"   -> updateStatus(deliveryId, DeliveryStatus.FAILED);
            case "delivery.returned" -> updateStatus(deliveryId, DeliveryStatus.RETURNED);
            case "delivery.cancelled"-> updateStatus(deliveryId, DeliveryStatus.CANCELLED);
            default -> log.warn("Unhandled Uber webhook event type: {}", eventType);
        }
    }

    private void updateStatus(String uberDeliveryId, DeliveryStatus newStatus) {
        Optional<DeliveryOrder> orderOpt = deliveryOrderRepository.findByUberDeliveryId(uberDeliveryId);

        if (orderOpt.isEmpty()) {
            log.warn("No delivery order found for uberDeliveryId={} — possible race condition or test event",
                    uberDeliveryId);
            return;
        }

        DeliveryOrder order = orderOpt.get();
        DeliveryStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);
        deliveryOrderRepository.save(order);

        log.info("Delivery {} status updated: {} → {} (WC order: {})",
                uberDeliveryId, previousStatus, newStatus, order.getWoocommerceOrderId());
    }
}
