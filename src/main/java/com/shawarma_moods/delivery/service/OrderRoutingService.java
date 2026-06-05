package com.shawarma_moods.delivery.service;

import com.shawarma_moods.delivery.dto.WooCommerceOrder;
import com.shawarma_moods.delivery.model.DeliveryOrder;
import com.shawarma_moods.delivery.model.DeliveryStatus;
import com.shawarma_moods.delivery.model.OrderType;
import com.shawarma_moods.delivery.model.PickupOrder;
import com.shawarma_moods.delivery.repository.DeliveryOrderRepository;
import com.shawarma_moods.delivery.repository.PickupOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderRoutingService {
    private static final Logger log = LoggerFactory.getLogger(OrderRoutingService.class);

    private final UberDeliveryService uberDeliveryService;
    private final PickupOrderRepository pickupOrderRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;

    public OrderRoutingService(
            UberDeliveryService uberDeliveryService,
            PickupOrderRepository pickupOrderRepository,
            DeliveryOrderRepository deliveryOrderRepository
    ) {
        this.uberDeliveryService = uberDeliveryService;
        this.pickupOrderRepository = pickupOrderRepository;
        this.deliveryOrderRepository = deliveryOrderRepository;
    }

    /**
     * Routes a WooCommerce order to the correct handler based on order_type metadata.
     * Delivery → Uber Direct
     * Pickup   → pickup_orders table (dashboard picks it up)
     */
    public void routeOrder(WooCommerceOrder order) {
        String orderType = order.getMetaValue("order_type");
        String wcOrderId = String.valueOf(order.getId());

        log.info("Routing WC order {} — type: {}", wcOrderId, orderType);

        if ("delivery".equalsIgnoreCase(orderType)) {
            handleDelivery(order, wcOrderId);
        } else if ("pickup".equalsIgnoreCase(orderType)) {
            handlePickup(order, wcOrderId);
        } else {
            log.warn("WC order {} has unknown or missing order_type: '{}' — skipping routing",
                    wcOrderId, orderType);
        }
    }

    private void handleDelivery(WooCommerceOrder order, String wcOrderId) {
        WooCommerceOrder.Address shipping = order.getShipping();
        WooCommerceOrder.Address billing  = order.getBilling();

        // Fall back to billing address if shipping is empty
        WooCommerceOrder.Address address =
                (shipping != null && shipping.getAddress1() != null && !shipping.getAddress1().isBlank())
                        ? shipping : billing;

        String customerName    = address.getFullName();
        String customerAddress = address.getFullAddress();
        String customerPhone   = billing.getPhone();
        String manifest        = order.buildManifestDescription();
        String itemsJson       = order.buildLineItemsJson();
        Integer totalCents     = order.getTotalCents();
        Integer taxCents       = order.getTaxCents();
        Integer shippingCents  = order.getShippingCents();

        log.info("Creating Uber delivery for WC order {}: {} → {}",
                wcOrderId, customerName, customerAddress);

        // Idempotency check — skip silently if already processed
        if (deliveryOrderRepository.findByWoocommerceOrderId(wcOrderId).isPresent()) {
            log.warn("Delivery order already exists for WC order {} — skipping (duplicate webhook)", wcOrderId);
            return;
        }

        try {
            uberDeliveryService.createDelivery(
                    wcOrderId,
                    customerName,
                    customerAddress,
                    customerPhone,
                    totalCents,
                    taxCents,
                    shippingCents,
                    manifest,
                    itemsJson
            );
        } catch (Exception e) {
            log.error(
                "UBER DISPATCH FAILED for WC order {} ({}): {} — order saved as FAILED. " +
                "Customer phone: {}. Manual follow-up required.",
                wcOrderId, customerName, e.getMessage(), customerPhone
            );
            persistFailedDelivery(wcOrderId, customerName, customerAddress,
                    customerPhone, totalCents, taxCents, shippingCents, itemsJson);
        }
    }

    /**
     * Saves a delivery order with FAILED status when Uber dispatch fails after payment.
     * This ensures the order is visible on the dashboard so staff can contact the customer.
     */
    private void persistFailedDelivery(
            String wcOrderId,
            String customerName,
            String customerAddress,
            String customerPhone,
            Integer totalCents,
            Integer taxCents,
            Integer shippingCents,
            String itemsJson
    ) {
        // Avoid duplicate failure records on webhook retries
        if (deliveryOrderRepository.findByWoocommerceOrderId(wcOrderId).isPresent()) {
            log.warn("Failed delivery record already exists for WC order {} — skipping", wcOrderId);
            return;
        }
        DeliveryOrder failed = new DeliveryOrder();
        failed.setWoocommerceOrderId(wcOrderId);
        failed.setStatus(DeliveryStatus.FAILED);
        failed.setOrderType(OrderType.DELIVERY);
        failed.setCustomerName(customerName);
        failed.setCustomerAddress(customerAddress);
        failed.setCustomerPhone(customerPhone);
        failed.setItemsDescription(itemsJson);
        failed.setTotalCents(totalCents);
        failed.setTaxCents(taxCents);
        failed.setShippingCents(shippingCents);
        deliveryOrderRepository.save(failed);
        log.info("Saved FAILED delivery order for WC order {}", wcOrderId);
    }

    private void handlePickup(WooCommerceOrder order, String wcOrderId) {
        // Idempotency check
        if (pickupOrderRepository.findByWoocommerceOrderId(wcOrderId).isPresent()) {
            log.warn("Pickup order already exists for WC order {} — skipping", wcOrderId);
            return;
        }

        WooCommerceOrder.Address billing = order.getBilling();

        PickupOrder pickupOrder = new PickupOrder();
        pickupOrder.setWoocommerceOrderId(wcOrderId);
        pickupOrder.setCustomerName(billing.getFullName());
        pickupOrder.setCustomerPhone(billing.getPhone());
        pickupOrder.setCustomerEmail(billing.getEmail());
        pickupOrder.setItemsDescription(order.buildLineItemsJson());
        pickupOrder.setTotalCents(order.getTotalCents());
        pickupOrder.setTaxCents(order.getTaxCents());
        pickupOrder.setShippingCents(order.getShippingCents());

        pickupOrderRepository.save(pickupOrder);

        log.info("Pickup order saved for WC order {}: {} — {}",
                wcOrderId, billing.getFullName(), order.buildManifestDescription());
    }
}
