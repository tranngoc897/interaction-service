# Workflow Guide

How to extend, customize, and maintain the Custom Workflow Engine for different business processes.

## Overview

The workflow engine is designed to be highly extensible. You can:

- Add new workflow states and transitions
- Implement custom business logic
- Define complex rule conditions
- Integrate with external systems
- Customize error handling and retry logic

## Adding New Workflow States

### 1. Define the State

Add the new state to your workflow definition. For example, adding a "DOCUMENT_VERIFICATION" state:

```sql
-- Add to transition table
INSERT INTO onboarding_transition (flow_version, from_state, action, to_state, is_async, source_service, allowed_actors, max_retry, conditions_json) VALUES
('v1', 'DOC_UPLOADED', 'NEXT', 'DOCUMENT_VERIFICATION', true, 'UI', ARRAY['USER'], 3, '[]'),
('v1', 'DOCUMENT_VERIFICATION', 'VERIFICATION_OK', 'EKYC_PENDING', false, 'SYSTEM', ARRAY['SYSTEM'], 3, '[]'),
('v1', 'DOCUMENT_VERIFICATION', 'VERIFICATION_FAILED', 'DOC_REJECTED', false, 'SYSTEM', ARRAY['SYSTEM'], 3, '[]');
```

### 2. Implement Step Handler

Create a new StepHandler for the business logic:

```java
@Component("DOCUMENT_VERIFICATION")
public class DocumentVerificationHandler implements StepHandler {

    private final DocumentVerificationService verificationService;

    @Override
    public StepResult execute(StepContext context) {
        try {
            VerificationResult result = verificationService.verify(
                context.instanceId(),
                context.state()
            );

            if (result.isApproved()) {
                return StepResult.success();
            } else {
                return StepResult.failure(
                    new StepError("DOCUMENT_REJECTED", ErrorType.BUSINESS,
                        "Document verification failed: " + result.getReason())
                );
            }

        } catch (Exception ex) {
            return StepResult.failure(
                new StepError("VERIFICATION_ERROR", ErrorType.SYSTEM,
                    "Document verification service error: " + ex.getMessage())
            );
        }
    }
}
```

### 3. Update State Context

If your step needs to store data for rule evaluation:

```java
// In your StepHandler
@Override
public StepResult execute(StepContext context) {
    // ... business logic ...

    // Store result in context for rule evaluation
    Map<String, Object> contextData = Map.of(
        "document_verified", true,
        "verification_score", 0.95,
        "verified_at", Instant.now()
    );

    stateContextService.updateContext(context.instanceId(), contextData);

    return StepResult.success();
}
```

## Custom Rule Conditions

### Simple Conditions

The RuleEngine supports simple conditions like:

```json
["otp_status == SUCCESS"]
["ekyc_score >= 0.8"]
["user_age > 18"]
["risk_level != HIGH"]
```

### Complex Conditions

For more complex rules, extend the RuleEngine:

```java
@Service
public class ExtendedRuleEngine extends RuleEngine {

    public boolean evaluateComplexRule(String rule, Map<String, Object> context) {
        // Parse complex rules like:
        // "user_age >= 18 AND risk_score < 0.7 AND nationality IN ['US', 'CA']"
        return evaluateCompoundRule(rule, context);
    }

    private boolean evaluateCompoundRule(String rule, Map<String, Object> context) {
        // Implement AND, OR, IN operators
        // Use expression parser or custom logic
    }
}
```

### Custom Rule Functions

Add domain-specific rule functions:

```java
@Service
public class BankingRuleEngine extends RuleEngine {

    public boolean isValidCreditScore(Map<String, Object> context) {
        Double score = (Double) context.get("credit_score");
        String country = (String) context.get("country");

        if ("US".equals(country)) {
            return score >= 650;
        } else if ("EU".equals(country)) {
            return score >= 600;
        }

        return score >= 500; // Default
    }

    public boolean hasValidDocuments(Map<String, Object> context) {
        List<String> documents = (List<String>) context.get("uploaded_documents");
        return documents != null &&
               documents.contains("ID") &&
               documents.contains("PROOF_OF_ADDRESS");
    }
}
```

## Custom Step Handlers

### Async Step Handler

For steps that call external services:

```java
@Component("EXTERNAL_API_CALL")
public class ExternalApiHandler implements StepHandler {

    private final OutboxService outboxService;
    private final RestTemplate restTemplate;

    @Override
    public StepResult execute(StepContext context) {
        // For async steps, just initiate the call
        // The actual processing happens via Kafka callback

        try {
            // Store event in outbox for guaranteed delivery
            outboxService.storeEvent(
                UUID.randomUUID().toString(),
                "external-api-request",
                context.instanceId().toString(),
                Map.of(
                    "instanceId", context.instanceId(),
                    "action", "verify_user"
                ),
                "EXTERNAL_API_REQUEST"
            );

            return StepResult.success(); // Async accepted

        } catch (Exception ex) {
            return StepResult.failure(
                new StepError("EXTERNAL_API_ERROR", ErrorType.SYSTEM,
                    "Failed to initiate external API call: " + ex.getMessage())
            );
        }
    }
}
```

### Retry-Aware Handler

For steps that need custom retry logic:

```java
@Component("PAYMENT_PROCESSING")
public class PaymentProcessingHandler implements StepHandler {

    @Override
    public StepResult execute(StepContext context) {
        // Get retry count from context or database
        int retryCount = getRetryCount(context);

        try {
            PaymentResult result = paymentService.process(context.instanceId());

            if (result.isSuccess()) {
                return StepResult.success();
            } else if (result.isRetryable() && retryCount < 3) {
                // Return failure to trigger retry
                return StepResult.failure(
                    new StepError("PAYMENT_RETRY", ErrorType.TRANSIENT,
                        "Payment failed, will retry: " + result.getMessage())
                );
            } else {
                // Permanent failure
                return StepResult.failure(
                    new StepError("PAYMENT_FAILED", ErrorType.BUSINESS,
                        "Payment permanently failed: " + result.getMessage())
                );
            }

        } catch (Exception ex) {
            return StepResult.failure(
                new StepError("PAYMENT_ERROR", ErrorType.SYSTEM,
                    "Payment service error: " + ex.getMessage())
            );
        }
    }
}
```

## Custom Error Handling

### Domain-Specific Exceptions

```java
public class WorkflowBusinessException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;

    public WorkflowBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WorkflowBusinessException(String errorCode, String message,
                                   Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}
```

### Custom Error Handler

```java
@Component
public class BusinessErrorHandler {

    public StepResult handleBusinessError(WorkflowBusinessException ex,
                                        StepContext context) {
        // Log business error
        log.warn("Business error in {} for instance {}: {}",
                context.state(), context.instanceId(), ex.getMessage());

        // Create incident for manual review
        incidentService.createIncident(
            context.instanceId(),
            context.state(),
            ex.getErrorCode(),
            "HIGH",
            ex.getMessage()
        );

        return StepResult.failure(
            new StepError(ex.getErrorCode(), ErrorType.BUSINESS, ex.getMessage())
        );
    }
}
```

## Integration Patterns

### External Service Integration

#### Synchronous Call

```java
@Service
public class ExternalServiceClient {

    public StepResult callExternalService(StepContext context) {
        try {
            ResponseEntity<ExternalResponse> response = restTemplate.postForEntity(
                externalServiceUrl,
                createRequest(context),
                ExternalResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return StepResult.success();
            } else {
                return StepResult.failure(
                    new StepError("EXTERNAL_SERVICE_ERROR", ErrorType.TRANSIENT,
                        "External service returned: " + response.getStatusCode())
                );
            }

        } catch (ResourceAccessException ex) {
            return StepResult.failure(
                new StepError("EXTERNAL_SERVICE_TIMEOUT", ErrorType.TRANSIENT,
                    "External service timeout: " + ex.getMessage())
            );
        } catch (Exception ex) {
            return StepResult.failure(
                new StepError("EXTERNAL_SERVICE_ERROR", ErrorType.SYSTEM,
                    "External service error: " + ex.getMessage())
            );
        }
    }
}
```

#### Asynchronous Call with Callback

```java
@Service
public class AsyncExternalServiceClient {

    private final OutboxService outboxService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public StepResult callExternalServiceAsync(StepContext context) {
        try {
            // Send request via outbox
            outboxService.storeEvent(
                UUID.randomUUID().toString(),
                "external-service-request",
                context.instanceId().toString(),
                Map.of(
                    "instanceId", context.instanceId(),
                    "callbackTopic", "external-service-callback"
                ),
                "EXTERNAL_REQUEST"
            );

            return StepResult.success(); // Will be completed via callback

        } catch (Exception ex) {
            return StepResult.failure(
                new StepError("ASYNC_CALL_ERROR", ErrorType.SYSTEM,
                    "Failed to initiate async call: " + ex.getMessage())
            );
        }
    }
}
```

### Database Integration

#### Custom Repository Methods

```java
@Repository
public interface CustomInstanceRepository extends JpaRepository<OnboardingInstance, UUID> {

    @Query("SELECT i FROM OnboardingInstance i WHERE i.status = :status AND i.updatedAt < :cutoff")
    List<OnboardingInstance> findStaleInstances(@Param("status") String status,
                                               @Param("cutoff") Instant cutoff);

    @Query("SELECT COUNT(i) FROM OnboardingInstance i WHERE i.currentState = :state")
    long countByCurrentState(@Param("state") String state);

    @Modifying
    @Query("UPDATE OnboardingInstance i SET i.status = 'CANCELLED' WHERE i.id = :id")
    int cancelInstance(@Param("id") UUID id);
}
```

#### Custom State Transitions

```java
@Service
public class CustomTransitionService {

    @Transactional
    public void performBulkTransition(List<UUID> instanceIds, String newState) {
        for (UUID instanceId : instanceIds) {
            try {
                ActionCommand command = ActionCommand.admin(
                    instanceId,
                    "FORCE_TRANSITION",
                    UUID.randomUUID().toString(),
                    "system"
                );

                // Update state directly (bypassing normal validation for admin operations)
                OnboardingInstance instance = instanceRepository.findById(instanceId)
                    .orElseThrow();
                instance.setCurrentState(newState);
                instanceRepository.save(instance);

                // Log the transition
                historyService.logTransition(instanceId, instance.getCurrentState(),
                                           newState, "FORCE_TRANSITION", "SUCCESS");

            } catch (Exception ex) {
                log.error("Failed to transition instance {} to {}: {}",
                         instanceId, newState, ex.getMessage());
            }
        }
    }
}
```

## Monitoring and Observability

### Custom Metrics

```java
@Service
public class CustomMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordBusinessMetric(String metricName, double value, Map<String, String> tags) {
        Counter.builder(metricName)
                .tags(tags)
                .register(meterRegistry)
                .increment(value);
    }

    public void recordPerformanceMetric(String operation, long durationMs) {
        Timer.builder("custom.operation.duration")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMs));
    }
}
```

### Custom Health Indicators

```java
@Component
public class WorkflowHealthIndicator implements HealthIndicator {

    private final OnboardingInstanceRepository instanceRepository;

    @Override
    public Health health() {
        try {
            long activeInstances = instanceRepository.countByStatus("ACTIVE");
            long failedInstances = instanceRepository.countByStatus("FAILED");

            if (failedInstances > activeInstances * 0.1) { // More than 10% failed
                return Health.down()
                        .withDetail("failedInstances", failedInstances)
                        .withDetail("activeInstances", activeInstances)
                        .build();
            }

            return Health.up()
                    .withDetail("activeInstances", activeInstances)
                    .withDetail("failedInstances", failedInstances)
                    .build();

        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
```

## Testing Strategies

### Unit Testing Step Handlers

```java
@SpringBootTest
public class DocumentVerificationHandlerTest {

    @MockBean
    private DocumentVerificationService verificationService;

    @Autowired
    private DocumentVerificationHandler handler;

    @Test
    public void shouldReturnSuccessWhenVerificationPasses() {
        // Given
        StepContext context = new StepContext(UUID.randomUUID(), "DOCUMENT_VERIFICATION", "v1");
        when(verificationService.verify(any(), any())).thenReturn(VerificationResult.approved());

        // When
        StepResult result = handler.execute(context);

        // Then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    public void shouldReturnFailureWhenVerificationFails() {
        // Given
        StepContext context = new StepContext(UUID.randomUUID(), "DOCUMENT_VERIFICATION", "v1");
        when(verificationService.verify(any(), any()))
            .thenReturn(VerificationResult.rejected("Invalid document"));

        // When
        StepResult result = handler.execute(context);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError().getCode()).isEqualTo("DOCUMENT_REJECTED");
    }
}
```

### Integration Testing Workflows

```java
@SpringBootTest
@Testcontainers
public class WorkflowIntegrationTest {

    @Autowired
    private OnboardingEngine onboardingEngine;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldCompleteFullOnboardingWorkflow() {
        // Start onboarding
        ResponseEntity<Map> startResponse = restTemplate.postForEntity(
            "/api/onboarding/start",
            Map.of("userId", "test-user"),
            Map.class
        );

        UUID instanceId = UUID.fromString((String) startResponse.getBody().get("instanceId"));

        // Perform actions through the workflow
        performAction(instanceId, "NEXT"); // PHONE_ENTERED -> OTP_VERIFIED
        performAction(instanceId, "NEXT"); // OTP_VERIFIED -> PROFILE_COMPLETED
        // ... continue through all states

        // Verify completion
        OnboardingInstance instance = instanceRepository.findById(instanceId).orElseThrow();
        assertThat(instance.getCurrentState()).isEqualTo("COMPLETED");
        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    }

    private void performAction(UUID instanceId, String action) {
        restTemplate.postForEntity(
            "/api/onboarding/" + instanceId + "/action",
            Map.of("action", action, "requestId", UUID.randomUUID().toString()),
            Map.class
        );
    }
}
```

## Performance Optimization

### Database Indexes

```sql
-- Critical indexes for performance
CREATE INDEX CONCURRENTLY idx_onboarding_state_time ON onboarding_instance(current_state, state_started_at);
CREATE INDEX CONCURRENTLY idx_history_instance_time ON onboarding_history(instance_id, created_at DESC);
CREATE INDEX CONCURRENTLY idx_step_retry ON step_execution(status, next_retry_at) WHERE status = 'FAILED';
CREATE INDEX CONCURRENTLY idx_transition_lookup ON onboarding_transition(flow_version, from_state, action);
```

### Caching Strategies

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10)))
                .build();
    }
}

@Service
public class CachedTransitionService {

    @Cacheable("transitions")
    public Transition getTransition(String flowVersion, String fromState, String action) {
        return transitionRepository.findById(...).orElse(null);
    }
}
```

### Connection Pool Tuning

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

## Deployment Considerations

### Environment-Specific Configuration

```yaml
# application-prod.yaml
workflow:
  retry:
    max-attempts: 5  # Higher in prod
  timeout:
    default-seconds: 600  # Longer timeouts

outbox:
  publish-interval: 5000  # More frequent in prod

# application-dev.yaml
workflow:
  retry:
    max-attempts: 1  # Faster feedback in dev
  timeout:
    default-seconds: 60

logging:
  level:
    com.ngoctran.interactionservice: DEBUG
```

### Feature Flags

```java
@Service
public class FeatureFlagService {

    private final FeatureFlagClient featureFlagClient;

    public boolean isNewWorkflowEnabled() {
        return featureFlagClient.isEnabled("new-workflow-engine");
    }

    public boolean isAdvancedRetryEnabled() {
        return featureFlagClient.isEnabled("advanced-retry-logic");
    }
}
```

## Troubleshooting

### Common Issues

#### Workflow Stuck in State

```java
// Diagnostic query
SELECT instance_id, current_state, state_started_at,
       EXTRACT(EPOCH FROM (now() - state_started_at))/60 as minutes_stuck
FROM onboarding_instance
WHERE current_state NOT IN ('COMPLETED', 'CANCELLED')
  AND state_started_at < now() - interval '1 hour'
ORDER BY state_started_at;
```

#### High Error Rates

```java
// Check error patterns
SELECT error_code, COUNT(*) as count,
       AVG(EXTRACT(EPOCH FROM (created_at - state_started_at))) as avg_duration
FROM onboarding_history
WHERE result = 'FAILED'
  AND created_at >= now() - interval '1 hour'
GROUP BY error_code
ORDER BY count DESC;
```

#### Performance Monitoring

```java
// Slow transitions
SELECT from_state, to_state, action,
       AVG(EXTRACT(EPOCH FROM (created_at - lag(created_at) OVER (PARTITION BY instance_id ORDER BY created_at)))) as avg_transition_time
FROM onboarding_history
WHERE created_at >= now() - interval '1 hour'
GROUP BY from_state, to_state, action;
```

This guide provides the foundation for extending and customizing the workflow engine to meet your specific business requirements while maintaining reliability and performance.
