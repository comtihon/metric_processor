package com.metric.processor;

import com.metric.processor.dto.EventDTO;
import com.metric.processor.dto.TemperatureDTO;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MeasurementsTests {
    @ClassRule
    public static KafkaEmbedded embeddedKafka =
            new KafkaEmbedded(1, true, "temperature", "events");
    private RedisServer redisServer;

    @Autowired
    private KafkaConfiguration kafkaConfiguration;
    private KafkaTemplate<String, TemperatureDTO> template;
    private KafkaMessageListenerContainer<String, EventDTO> container;
    private CountDownLatch latch;
    private List<EventDTO> events = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        ProducerFactory<String, TemperatureDTO> producerFactory =
                kafkaConfiguration.producerFactory(embeddedKafka);

        template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic("temperature");
        DefaultKafkaConsumerFactory<String, EventDTO> consumerFactory =
                kafkaConfiguration.consumerFactory(embeddedKafka);
        ContainerProperties containerProperties = new ContainerProperties("events");

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, EventDTO>) record -> {
            events.add(record.value());
            latch.countDown();
        });
        container.start();

        redisServer = new RedisServer(60379);
        redisServer.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
    }

    @After
    public void tearDown() {
        container.stop();
        redisServer.stop();
    }

    @Test
    public void generateEventTest() throws InterruptedException {
        latch = new CountDownLatch(1);
        for (int i = 0; i < 3; i++)
            template.sendDefault(new TemperatureDTO("uuid6", 100));
        latch.await(2000, TimeUnit.MILLISECONDS);
        Assert.assertEquals(0, latch.getCount());
        Assert.assertEquals(1, events.size());
        EventDTO event = events.get(0);
        Assert.assertEquals("uuid6", event.getSensorUuid());
        Assert.assertEquals(100, event.getTemperature(), 0.1);
    }

}
