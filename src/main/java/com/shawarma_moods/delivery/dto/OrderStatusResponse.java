package com.shawarma_moods.delivery.dto;

public class OrderStatusResponse {

    private String woocommerceOrderId;
    private String type;
    private String status;
    private String trackingUrl;
    private String statusMessage;

    public static OrderStatusResponse notFound(String woocommerceOrderId) {
        OrderStatusResponse r = new OrderStatusResponse();
        r.woocommerceOrderId = woocommerceOrderId;
        r.statusMessage = "Order not found.";
        return r;
    }

    public String getWoocommerceOrderId() {
        return woocommerceOrderId;
    }
    public void setWoocommerceOrderId(String woocommerceOrderId) {
        this.woocommerceOrderId = woocommerceOrderId;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }
    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public String getStatusMessage() {
        return statusMessage;
    }
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
