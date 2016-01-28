jtier-service-core
==================

# Overview
JTier service core currently builds on top on the dropwizard core. The goal is to provide
the essentials for a dropwizard based service to run in the Groupon ecosystem. Current functionality includes

* [heartbeat](https://wiki.groupondev.com/Heartbeat)
* status endpoint that provides the following
    * Git information(commit,branch etc.)
    * Build information(buildtime,built by etc.)
    * deploy time.

# Using JTier service core
## HealthBundle
* Service core currently exposes a health bundle which is pre initialized in 

```java
@Override
public void initialize(final Bootstrap<JTierConfiguration> bootstrap) {
    bootstrap.addBundle(new HealthBundle<>());
}
```

 Application should respond to the following endpoints

```
 /grpn/healthcheck
```

```
 /status
```
### Configuration
#### Defaults
By default JTier is providing this configuration:

```yaml
---
jtier:
  defaultConfigLoaded: true
logging:
  level: INFO
  appenders:
    - type: console
    - type: Steno
      threshold: INFO
      currentLogFilename: /var/groupon/jtier/logs/jtier.steno.log
      archivedLogFilenamePattern: /var/groupon/jtier/logs/jtier.steno.-%d{yyyy-MM-dd}-%i.log.gz
      archivedFileCount: 7
      timeZone: UTC
      maxFileSize: 10MB
```

#### Override 
*  If you want to override the base configuration, you needs to extends the JTierConfiguration class *HealthConfigurationProvider* interface

```java
@JsonTypeName("health")
public interface HealthConfigurationProvider {

    HealthConfiguration getHealthConfiguration();

}
```

e.g implementation for the configuration would look like

```java
package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.groupon.jtier.service.core.health.HealthConfiguration;
import com.groupon.jtier.service.core.health.HealthConfigurationProvider;

import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;

public class JTierConfiguration extends Configuration implements HealthConfigurationProvider {

    @NotNull
    private HealthConfiguration healthConfiguration;

    @JsonProperty("health")
    @Override
    public HealthConfiguration getHealthConfiguration() {
        return healthConfiguration;
    }
    // TODO: implement service configuration
}
```

And include the following in the application.yml

```
health:
  heartbeatPath: /var/groupon/jtier/heartbeat.txt
```

## MetricsBundle
* Currently the MetricsBundle gives you automatic metrics for the following as described below. (is pre initialized)

```java
@Override
public void initialize(final Bootstrap<JTierConfiguration> bootstrap) {
    bootstrap.addBundle(new MetricsBundle<>());
}
```

* Counters:
counters follow the following naming convention: $servicePrefix_$HTTP-VERB_$path_request_status_$STATUS-CODE
Below are metrics that your application will generate for method : POST /locale/:locale/id/:id
 * jtier_POST_locale_id_request_status_1xx
 * jtier_POST_locale_id_request_status_200
 * jtier_POST_locale_id_request_status_2xx
 * jtier_POST_locale_id_request_status_3xx
 * jtier_POST_locale_id_request_status_400
 * jtier_POST_locale_id_request_status_403
 * jtier_POST_locale_id_request_status_404
 * jtier_POST_locale_id_request_status_4xx
 * jtier_POST_locale_id_request_status_420
 * jtier_POST_locale_id_request_status_500
 * jtier_POST_locale_id_request_status_502
 * jtier_POST_locale_id_request_status_503
 * jtier_POST_locale_id_request_status_504
 * jtier_POST_locale_id_request_status_5xx
 * jtier_POST_locale_id_request_status_xxx
 * Each endpoint in your application will generate all the above counters.

* Timers:
timers follow the naming convention: $servicePrefix_$HTTP-VERB_$path
Below are metrics that your application will generate for method : POST /locale/:locale/id/:id
 * jtier_POST_locale_id
 * Each endpoint in your application will generate all the above timers.

### Configuration
* Your application needs to implement *MetricsConfigurationProvider* interface

```java
@JsonTypeName("jtierMetrics")
public interface MetricsConfigurationProvider {

    MetricsConfiguration getMetricsConfiguration();

}
```

e.g implementation for the configuration would look like

```java
package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.groupon.jtier.service.core.health.MetricsConfiguration;
import com.groupon.jtier.service.core.health.MetricsConfigurationProvider;

import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;

public class JTierConfiguration extends Configuration implements MetricsConfigurationProvider {

    @NotNull
    private MetricsConfiguration metricsConfiguration;

    @JsonProperty("jtierMetrics")
    @Override
    public MetricsConfiguration getMetricsConfiguration() {
        return metricsConfiguration;
    }
    // TODO: implement service configuration
}
```

And include the following in the application.yml

```
jtierMetrics:
  path: "/var/groupon/jtier/logs"
  name: "query"
```

## DebugBundle
* The DebugBundle currently checks the existence of "X-Request-Id" header in all
incoming requests. If the header is missing a new UUID is added to the requests and
added to the thread context ensuring all application logs will have this as one of the keys.

* The X-Request-Id is allow added to the response.

```java
@Override
public void initialize(final Bootstrap<JTierConfiguration> bootstrap) {
    bootstrap.addBundle(new DebugBundle<>());
}
```
## Under the covers

* Read [Dropwizard Interals](https://dropwizard.github.io/dropwizard/manual/internals.html) to learn more about dropwizard bundles.
