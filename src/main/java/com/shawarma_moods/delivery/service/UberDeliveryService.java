package com.shawarma_moods.delivery.service;

import com.shawarma_moods.delivery.config.RestaurantProperties;
import com.shawarma_moods.delivery.config.UberProperties;
import com.shawarma_moods.delivery.dto.*;
import com.shawarma_moods.delivery.model.DeliveryOrder;
import com.shawarma_moods.delivery.model.DeliveryStatus;
import com.shawarma_moods.delivery.model.OrderType;
import com.shawarma_moods.delivery.repository.DeliveryOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class UberDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(UberDeliveryService.class);

    private final UberAuthService uberAuthService;
    private final UberProperties uberProperties;
    private final RestaurantProperties restaurantProperties;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private  final RestTemplate restTemplate;

    public UberDeliveryService(
            UberAuthService uberAuthService,
            UberProperties uberProperties,
            RestaurantProperties restaurantProperties,
            DeliveryOrderRepository deliveryOrderRepository,
            RestTemplate restTemplate
    ) {
        this.uberAuthService = uberAuthService;
        this.uberProperties = uberProperties;
        this.restaurantProperties = restaurantProperties;
        this.deliveryOrderRepository = deliveryOrderRepository;
        this.restTemplate = restTemplate;
    }

    // Request a delivery quote
    // Call this before creating a delivery to get fee and time estimates.

    public UberQuoteResponse getQuote(String dropoffAddress) {
        log.info("Requesting delivery quote for dropoff: {}", dropoffAddress);

        String url = uberProperties.getBaseUrl()
                + "/v1/customers/" + uberProperties.getCustomerId()
                + "/delivery_quotes";

        UberQuoteRequest quoteRequest = new UberQuoteRequest(
                restaurantProperties.getAddress(),
                dropoffAddress
        );

        HttpEntity<UberQuoteRequest> request = new HttpEntity<>(quoteRequest, buildAuthHeaders());

        try {
            ResponseEntity<UberQuoteResponse> response = restTemplate.postForEntity(
                    url, request, UberQuoteResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Quote received: fee={} {}", response.getBody().getFee(), response.getBody().getCurrency());
                return response.getBody();
            }
            throw new RuntimeException("Unexpected response from Uber quote: " + response.getStatusCode());
        } catch (Exception e){
            log.error("Failed to get delivery quote: {}", e.getMessage());
            throw new RuntimeException("Could not get delivery quote from Uber Direct", e);
        }
    }

    // Create a delivery with Uber Direct and persist it to the DB
    // Includes idempotency check - will not create a duplicate delivery
    // If one already exists for the given WooCommerce order ID.

    public UberDeliveryResponse createDelivery(
            String woocommerceOrderId,
            String customerName,
            String customerAddress,
            String customerPhone,
            Integer orderValueCents,
            Integer taxCents,
            Integer shippingCents,
            String manifestDescription,
            String itemsJson
    ) {
        // Idempotency check - never create two deliveries for the same order
        deliveryOrderRepository.findByWoocommerceOrderId(woocommerceOrderId).ifPresent(existing -> {
            log.warn("Delivery already exists for order {}, skipping creation", woocommerceOrderId);
            throw new RuntimeException("Delivery already exists for order: " + woocommerceOrderId);
        });

        log.info("Creating Uber delivery for Woocommerce order {}", woocommerceOrderId);

        // step 1: get a quote first
        UberQuoteResponse quoteResponse = getQuote(customerAddress);

        // step 2: build the delivery request
        String url = uberProperties.getBaseUrl()
                +"/v1/customers/" + uberProperties.getCustomerId()
                +"/deliveries";

        UberDeliveryRequest deliveryRequest = new UberDeliveryRequest();
        deliveryRequest.setQuoteId(quoteResponse.getId());
        deliveryRequest.setPickupName(restaurantProperties.getName());
        deliveryRequest.setPickupAddress(restaurantProperties.getAddress());
        deliveryRequest.setPickupPhoneNumber(restaurantProperties.getPhone());
        deliveryRequest.setDropoffName(customerName);
        deliveryRequest.setDropoffAddress(customerAddress);
        deliveryRequest.setDropoffPhoneNumber(customerPhone);
        deliveryRequest.setManifestTotalValue(orderValueCents);
        deliveryRequest.setManifestReference(woocommerceOrderId);
        deliveryRequest.setManifestItems(List.of(
                new UberManifestItem(
                        manifestDescription,   // real WC line items
                        1,              // quantity
                        "medium",       // size
                        orderValueCents // price in cents
                )
        ));

        HttpEntity<UberDeliveryRequest> request = new HttpEntity<>(deliveryRequest, buildAuthHeaders());

        try {
            ResponseEntity<UberDeliveryResponse> response = restTemplate.postForEntity(
                    url, request, UberDeliveryResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                UberDeliveryResponse deliveryResponse = response.getBody();
                log.info("Delivery created successfully: uberDeliveryId={}", deliveryResponse.getId());

                //step 3 : persist to database
                persistDelivery(woocommerceOrderId, customerName, customerAddress, customerPhone, orderValueCents, taxCents, shippingCents, itemsJson, deliveryResponse);

                return deliveryResponse;
            }
            throw new RuntimeException("Unexpected response from Uber create delivery: " + response.getStatusCode());

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create delivery: {}", e.getMessage());
            throw new RuntimeException("Could not create delivery with Uber Direct", e);
        }
    }

    private void persistDelivery(
            String woocommerceOrderId,
            String customerName,
            String customerAddress,
            String customerPhone,
            Integer totalCents,
            Integer taxCents,
            Integer shippingCents,
            String itemsJson,
            UberDeliveryResponse deliveryResponse
    ) {
        DeliveryOrder order = new DeliveryOrder();
        order.setWoocommerceOrderId(woocommerceOrderId);
        order.setUberDeliveryId(deliveryResponse.getId());
        order.setStatus(DeliveryStatus.CREATED);
        order.setCustomerName(customerName);
        order.setCustomerAddress(customerAddress);
        order.setCustomerPhone(customerPhone);
        order.setOrderType(OrderType.DELIVERY);
        order.setTrackingUrl(deliveryResponse.getTrackingUrl());
        order.setItemsDescription(itemsJson);
        order.setTotalCents(totalCents);
        order.setTaxCents(taxCents);
        order.setShippingCents(shippingCents);
        deliveryOrderRepository.save(order);
        log.info("Delivery order persisted to DB for Woocommerce order {}", woocommerceOrderId);
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(uberAuthService.getAccessToken());
        return headers;
    }


}
