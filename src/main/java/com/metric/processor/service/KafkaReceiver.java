package com.metric.processor.service;

import com.metric.processor.dto.TemperatureDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaReceiver.class);

    @Autowired
    private TemperatureProcessor processor;

    @KafkaListener(topics = "${spring.kafka.topic_in}")
    public void receive(ConsumerRecord<String, TemperatureDTO> consumerRecord) throws InterruptedException {
        LOGGER.debug("got metric: '{}'", consumerRecord);
        processor.process(consumerRecord.value());
    }
}
