package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UberWebhookMeta {

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("status")
    private String status;

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

}
