package com.metric.processor.service;

import com.metric.processor.config.ServerConfig;
import com.metric.processor.dto.EventDTO;
import com.metric.processor.dto.TemperatureDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;

@Service
public class TemperatureProcessor implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemperatureProcessor.class);

    @Autowired
    private ServerConfig config;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    @Autowired
    private KafkaSender sender;

    private RedisLockRegistry redisLockRegistry;

    /**
     * Process temperature event:
     * 1. put distributed lock for this uuid (to prevent resetting counter from other processor servers)
     * 2. reset counter if temperature is below critical ${processor.warning_temperature}
     * 3. if temperature is above critical - increment warnings counter for this uuid
     * 4. if number of warnings exceed ${warnings_for_event} - send event to kafka
     *
     * @param metric got from kafka
     */
    public void process(TemperatureDTO metric) throws InterruptedException {
        Lock lock = redisLockRegistry.obtain(metric.getSensorUuid());
        lock.lockInterruptibly();
        try {
            if (metric.getTemperature() >= config.getFireEventRate()) {
                countAndFire(metric);
            } else {
                resetCounter(metric.getSensorUuid());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void afterPropertiesSet() {
        redisLockRegistry = new RedisLockRegistry(connectionFactory, config.getRegistryKey());
    }

    private void resetCounter(String uuid) {
        LOGGER.debug("Reset counter for {}", uuid);
        redisTemplate.opsForValue().set(uuid, 0);
    }

    private void countAndFire(TemperatureDTO metric) {
        int counter = redisTemplate.opsForValue().increment(metric.getSensorUuid(), 1).intValue();
        if (counter == config.getEventsToFire()) {
            sender.send(new EventDTO(metric.getSensorUuid(), metric.getTemperature(), metric.getAt()));
        }
    }
}
