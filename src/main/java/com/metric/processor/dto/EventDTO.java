package com.metric.processor.dto;

import java.util.Date;

public class EventDTO {
    private String sensorUuid;
    private double value;
    private Date at = new Date();
    private EventType type = EventType.TEMPERATURE_EXCEEDED;

    public EventDTO() {
    }

    public EventDTO(String sensorUuid, double value, Date date) {
        this.sensorUuid = sensorUuid;
        this.value = value;
        this.at = date;
    }

    public String getSensorUuid() {
        return sensorUuid;
    }

    public double getValue() {
        return value;
    }
}
