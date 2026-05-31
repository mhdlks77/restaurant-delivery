package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UberWebhookEvent {

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("created")
    private Long created;

    @JsonProperty("meta")
    private UberWebhookMeta meta;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }

    public UberWebhookMeta getMeta() { return meta; }
    public void setMeta(UberWebhookMeta meta) { this.meta = meta; }

}
