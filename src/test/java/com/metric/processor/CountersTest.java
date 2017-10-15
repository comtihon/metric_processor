package com.metric.processor;

import com.metric.processor.dto.TemperatureDTO;
import com.metric.processor.service.KafkaSender;
import com.metric.processor.service.TemperatureProcessor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CountersTest {
    private static RedisServer redisServer;
    @MockBean
    private KafkaSender sender;
    @Autowired
    private TemperatureProcessor processor;

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    @BeforeClass
    public static void setUp() throws Exception {
        redisServer = new RedisServer(60379);
        redisServer.start();
    }

    @AfterClass
    public static void tearDown() {
        redisServer.stop();
    }

    /**
     * Without any warnings counter should always be 0
     *
     * @throws InterruptedException from acquire lock
     */
    @Test
    public void checkNormal() throws InterruptedException {
        processor.process(new TemperatureDTO("uuid1", 18));
        processor.process(new TemperatureDTO("uuid1", 19));
        processor.process(new TemperatureDTO("uuid1", 20));
        Assert.assertEquals(0, redisTemplate.opsForValue().get("uuid1").intValue());
    }

    /**
     * Increasing warning counter should be done only if temperature exceeds maximum
     *
     * @throws InterruptedException from acquire lock
     */
    @Test
    public void checkIncreaseOne() throws InterruptedException {
        processor.process(new TemperatureDTO("uuid2", 18));
        processor.process(new TemperatureDTO("uuid2", 100));
        Assert.assertEquals(1, redisTemplate.opsForValue().get("uuid2").intValue());
    }

    /**
     * Should reset warning counter if got normal measurement
     *
     * @throws InterruptedException from acquire lock
     */
    @Test
    public void checkReset() throws InterruptedException {
        processor.process(new TemperatureDTO("uuid3", 100));
        processor.process(new TemperatureDTO("uuid3", 100));
        Assert.assertEquals(2, redisTemplate.opsForValue().get("uuid3").intValue());
        processor.process(new TemperatureDTO("uuid3", 18));
        Assert.assertEquals(0, redisTemplate.opsForValue().get("uuid3").intValue());
    }

    /**
     * Event should be fired if number of warnings exceed maximum
     *
     * @throws InterruptedException from acquire lock
     */
    @Test
    public void checkFireEvent() throws InterruptedException {
        processor.process(new TemperatureDTO("uuid4", 100));
        processor.process(new TemperatureDTO("uuid4", 100));
        processor.process(new TemperatureDTO("uuid4", 100));
        Assert.assertEquals(3, redisTemplate.opsForValue().get("uuid4").intValue());
        verify(sender, Mockito.times(1)).send(any());
    }

    /**
     * Should fire only one event per set of warnings.
     *
     * @throws InterruptedException from acquire lock
     */
    @Test
    public void checkFireOne() throws InterruptedException {
        for (int i = 0; i < 6; i++)
            processor.process(new TemperatureDTO("uuid5", 100));
        Assert.assertEquals(6, redisTemplate.opsForValue().get("uuid5").intValue());
        verify(sender, Mockito.times(1)).send(any());
    }

    /**
     * After receiving normal temperature should reset warning counter and
     * be able to send another warning event later, if counter exceed once more.
     *
     * @throws InterruptedException from acquire lock
     */
    @Test
    public void checkFireMultiple() throws InterruptedException {
        for (int i = 0; i < 3; i++)
            processor.process(new TemperatureDTO("uuid6", 100));
        processor.process(new TemperatureDTO("uuid6", 18));
        for (int i = 0; i < 3; i++)
            processor.process(new TemperatureDTO("uuid6", 100));
        Assert.assertEquals(3, redisTemplate.opsForValue().get("uuid6").intValue());
        verify(sender, Mockito.times(2)).send(any());
    }

}
