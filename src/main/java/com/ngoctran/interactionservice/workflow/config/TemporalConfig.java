package com.ngoctran.interactionservice.workflow.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class TemporalConfig {

    @Value("${temporal.server.host:localhost}")
    private String temporalHost;

    @Value("${temporal.server.port:7233}")
    private int temporalPort;

    @Value("${temporal.namespace:default}")
    private String namespace;

    @Value("${temporal.connection.timeout:10s}")
    private Duration connectionTimeout;

    /**
     * Create Temporal Service Stubs (connection to Temporal Server)
     */
    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("Connecting to Temporal Server at {}:{}", temporalHost, temporalPort);
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalHost + ":" + temporalPort)
                .build();
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(options);
        log.info("Successfully connected to Temporal Server");
        return service;
    }

    /**
     * Create Workflow Client for starting and querying workflows
     */
    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
        log.info("Creating Workflow Client for namespace: {}", namespace);
        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .setNamespace(namespace)
                .build();

        WorkflowClient client = WorkflowClient.newInstance(serviceStubs, options);

        log.info("Workflow Client created successfully");
        return client;
    }

    /**
     * Create Schedule Client for managing scheduled workflows
     */
    @Bean
    public ScheduleClient scheduleClient(WorkflowServiceStubs serviceStubs) {
        log.info("Creating Schedule Client");
        ScheduleClientOptions options = ScheduleClientOptions.newBuilder()
                .setNamespace(namespace)
                .build();
        return ScheduleClient.newInstance(serviceStubs, options);
    }

    /**
     * Create Worker Factory for executing workflows and activities
     */
    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        log.info("Creating Worker Factory");

        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);

        log.info("Worker Factory created successfully");
        return factory;
    }

    @Bean
    public TemporalShutdownHook temporalShutdownHook(WorkflowServiceStubs serviceStubs,
                                                     WorkerFactory workerFactory) {
        return new TemporalShutdownHook(serviceStubs, workerFactory);
    }

    public static class TemporalShutdownHook {
        private final WorkflowServiceStubs serviceStubs;
        private final WorkerFactory workerFactory;

        public TemporalShutdownHook(WorkflowServiceStubs serviceStubs, WorkerFactory workerFactory) {
            this.serviceStubs = serviceStubs;
            this.workerFactory = workerFactory;

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }
        private void shutdown() {
            log.info("Shutting down Temporal connections...");

            try {
                workerFactory.shutdown();
                log.info("Worker Factory shut down");
            } catch (Exception e) {
                log.error("Error shutting down Worker Factory", e);
            }

            try {
                serviceStubs.shutdown();
                log.info("Service Stubs shut down");
            } catch (Exception e) {
                log.error("Error shutting down Service Stubs", e);
            }

            log.info("Temporal shutdown complete");
        }
    }
}
