# Metrics Processor [![Build Status](https://travis-ci.org/comtihon/metric_processor.svg?branch=master)](https://travis-ci.org/comtihon/metric_processor)
Service for creating events from metrics.  
It creates an event if temperature metrics exceed value, set in configuration several times.  
Number of times is also configurable.  
Processor doesn't spam with events for sensor. If one event was fired - no events were created
until temperature becomes normal again.  
This service saves it's state (warning counters) to Redis, so it can easily scale.

## Configuration
Configuration is stored in `application.properties`.  
`processor.warning_temperature` - temperature above this will be considered as warning.    
`processor.warnings_for_event` - this is number of warnings to generate event. 

## Run
Ensure that [Redis](https://redis.io/) and [Kafka](https://kafka.apache.org/) 
are accessible before running the service.  
Access urls are specified in application.properties `spring.kafka.bootstrap-servers` 
for kafka and `spring.redis.host`, 'spring.redis.port' for redis.

### In docker

    sudo ./gradlew build buildDocker
    sudo docker run -t com.metric.processor

### In OS

    ./gradlew bootRun
    
## Testing
    
    ./gradlew check