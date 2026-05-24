package com.fms.hr_crm.common.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lifecycle bean that handles registration, heartbeat, and deregistration
 * for the owning service.
 *
 * <p>Created automatically by {@link ServiceRegistryAutoConfiguration} when
 * {@code registry.enabled=true}. Uses a daemon {@link ScheduledExecutorService}
 * for heartbeats so it does not prevent JVM shutdown.
 */
@Slf4j
public class ServiceRegistrar implements InitializingBean, DisposableBean {

    private static final long HEARTBEAT_INTERVAL_SECONDS = 30;

    private final ServiceRegistryClient client;
    private final RegistryProperties props;
    private final ScheduledExecutorService executor;

    public ServiceRegistrar(ServiceRegistryClient client, RegistryProperties props) {
        this.client = client;
        this.props = props;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "registry-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void afterPropertiesSet() {
        log.info("[Registry] Registering {} at {}", props.serviceName(), props.serviceUrl());
        client.register(props.serviceName(), props.serviceUrl());
        executor.scheduleAtFixedRate(
                () -> client.heartbeat(props.serviceName()),
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        log.info("[Registry] Heartbeat scheduled every {}s", HEARTBEAT_INTERVAL_SECONDS);
    }

    @Override
    public void destroy() {
        log.info("[Registry] Deregistering {} on shutdown", props.serviceName());
        client.deregister(props.serviceName());
        executor.shutdown();
    }
}