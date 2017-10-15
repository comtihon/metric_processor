package com.metric.processor.service;

import com.metric.processor.config.KafkaConfig;
import com.metric.processor.dto.EventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaSender.class);

    @Autowired
    private KafkaTemplate<String, EventDTO> kafkaTemplate;

    @Autowired
    private KafkaConfig config;

    public void send(EventDTO event) {
        LOGGER.debug("Fire event: '{}'", event);
        kafkaTemplate.send(config.getEventsTopic(), event);
    }
}
