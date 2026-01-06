# Deployment Guide

Complete guide for deploying the Custom Workflow Engine in production environments.

## Prerequisites

### Infrastructure Requirements

- **Kubernetes**: 1.19+ (recommended)
- **PostgreSQL**: 13+ with PostGIS extension
- **Kafka**: 2.8+ with KRaft mode (recommended)
- **Redis**: 6+ (optional, for caching)
- **JVM**: OpenJDK 17+

### Resource Requirements

#### Minimum (Development)
```yaml
requests:
  memory: "512Mi"
  cpu: "250m"
limits:
  memory: "1Gi"
  cpu: "500m"
```

#### Recommended (Production)
```yaml
requests:
  memory: "2Gi"
  cpu: "1000m"
limits:
  memory: "4Gi"
  cpu: "2000m"
```

### Network Requirements

- **Inbound**: HTTP/HTTPS (8080), Health checks (8081)
- **Outbound**: PostgreSQL (5432), Kafka (9092), Redis (6379)
- **Internal**: Service mesh communication

## Database Setup

### PostgreSQL Configuration

```sql
-- Create database
CREATE DATABASE onboarding_workflow;
GRANT ALL PRIVILEGES ON DATABASE onboarding_workflow TO onboarding_user;

-- Enable required extensions
\c onboarding_workflow;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
```

### Connection Pool Settings

```yaml
# application.yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/onboarding_workflow
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

### Database Migration

```bash
# Run migrations
./mvnw flyway:migrate -Dflyway.configFiles=src/main/resources/application.yaml

# Validate migration
./mvnw flyway:validate
```

## Kafka Setup

### Topic Configuration

```bash
# Create required topics
kafka-topics --create --topic onboarding-command --partitions 6 --replication-factor 3 --bootstrap-server kafka:9092
kafka-topics --create --topic ekyc-callback --partitions 6 --replication-factor 3 --bootstrap-server kafka:9092
kafka-topics --create --topic aml-callback --partitions 6 --replication-factor 3 --bootstrap-server kafka:9092
kafka-topics --create --topic onboarding-event --partitions 3 --replication-factor 3 --bootstrap-server kafka:9092
kafka-topics --create --topic onboarding-dlq --partitions 3 --replication-factor 3 --bootstrap-server kafka:9092
```

### Consumer Group Configuration

```yaml
spring:
  kafka:
    consumer:
      bootstrap-servers: kafka:9092
      group-id: onboarding-engine
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 10
      fetch-min-size: 1MB
      fetch-max-wait: 500ms

    producer:
      bootstrap-servers: kafka:9092
      acks: all
      retries: 3
      batch-size: 16384
      linger-ms: 5
      buffer-memory: 33554432
```

## Application Configuration

### Environment Variables

```bash
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=onboarding_workflow
DB_USER=onboarding
DB_PASSWORD=secure-password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN
KAFKA_SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="password";

# Redis (optional)
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=secure-redis-password

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
MANAGEMENT_SERVER_PORT=8081

# Workflow settings
WORKFLOW_RETRY_MAX_ATTEMPTS=5
WORKFLOW_TIMEOUT_DEFAULT_SECONDS=300
OUTBOX_PUBLISH_INTERVAL=5000
```

### Production Configuration

```yaml
# application-prod.yaml
spring:
  profiles: prod

  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      validation-timeout: 5000
      connection-test-query: SELECT 1

  kafka:
    consumer:
      max-poll-records: 50
      fetch-max-wait: 1000ms
    producer:
      compression-type: gzip
      batch-size: 32768

workflow:
  retry:
    max-attempts: 5
    backoff-multiplier: 2.0
  timeout:
    default-seconds: 600

logging:
  level:
    com.ngoctran.interactionservice: INFO
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId:-}] %logger{36} - %msg%n"

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
      base-path: /actuator
  metrics:
    export:
      prometheus:
        enabled: true
```

## Kubernetes Deployment

### Namespace Setup

```bash
# Create namespace
kubectl create namespace onboarding

# Create secrets
kubectl create secret generic onboarding-db-secret \
  --from-literal=username=onboarding \
  --from-literal=password=secure-password \
  -n onboarding

kubectl create secret generic onboarding-kafka-secret \
  --from-literal=username=kafka-user \
  --from-literal=password=kafka-password \
  -n onboarding
```

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: onboarding-config
  namespace: onboarding
data:
  application.yaml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres:5432/onboarding_workflow
      kafka:
        bootstrap-servers: kafka:9092
    workflow:
      retry:
        max-attempts: 5
      timeout:
        default-seconds: 300
```

### Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: onboarding-workflow
  namespace: onboarding
  labels:
    app: onboarding-workflow
spec:
  replicas: 3
  selector:
    matchLabels:
      app: onboarding-workflow
  template:
    metadata:
      labels:
        app: onboarding-workflow
    spec:
      containers:
      - name: workflow-engine
        image: your-registry/onboarding-workflow:latest
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 8081
          name: management
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_USER
          valueFrom:
            secretKeyRef:
              name: onboarding-db-secret
              key: username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: onboarding-db-secret
              key: password
        - name: KAFKA_USER
          valueFrom:
            secretKeyRef:
              name: onboarding-kafka-secret
              key: username
        - name: KAFKA_PASSWORD
          valueFrom:
            secretKeyRef:
              name: onboarding-kafka-secret
              key: password
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: management
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: management
          initialDelaySeconds: 30
          periodSeconds: 10
        startupProbe:
          httpGet:
            path: /actuator/health
            port: management
          initialDelaySeconds: 10
          periodSeconds: 5
          failureThreshold: 30
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        readOnlyRootFilesystem: true
      volumes:
      - name: tmp-volume
        emptyDir: {}
```

### Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: onboarding-workflow
  namespace: onboarding
spec:
  selector:
    app: onboarding-workflow
  ports:
  - name: http
    port: 80
    targetPort: 8080
  - name: management
    port: 8081
    targetPort: 8081
  type: ClusterIP
```

### Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: onboarding-workflow
  namespace: onboarding
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - api.yourcompany.com
    secretName: onboarding-tls
  rules:
  - host: api.yourcompany.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: onboarding-workflow
            port:
              number: 80
      - path: /admin
        pathType: Prefix
        backend:
          service:
            name: onboarding-workflow
            port:
              number: 80
```

## Monitoring Setup

### Prometheus Configuration

```yaml
# prometheus.yaml
scrape_configs:
  - job_name: 'onboarding-workflow'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - onboarding
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: onboarding-workflow
        action: keep
      - source_labels: [__meta_kubernetes_pod_container_port_number]
        regex: 8081
        action: keep
    metrics_path: /actuator/prometheus
```

### Grafana Dashboards

Key metrics to monitor:

```json
{
  "dashboard": {
    "title": "Workflow Engine Overview",
    "panels": [
      {
        "title": "Active Instances",
        "type": "stat",
        "targets": [
          {
            "expr": "workflow_instances_active",
            "legendFormat": "Active"
          }
        ]
      },
      {
        "title": "State Transitions",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(workflow_state_transition_total[5m])",
            "legendFormat": "{{from_state}} → {{to_state}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(workflow_error_total[5m])",
            "legendFormat": "{{error_code}}"
          }
        ]
      }
    ]
  }
}
```

### Alert Rules

```yaml
# alert_rules.yaml
groups:
- name: workflow.alerts
  rules:
  - alert: HighErrorRate
    expr: rate(workflow_error_total[5m]) > 0.1
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "High workflow error rate"
      description: "Workflow error rate is {{ $value }} errors per second"

  - alert: InstanceStuck
    expr: workflow_instances_active > 1000
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "High number of active instances"
      description: "There are {{ $value }} active workflow instances"

  - alert: OutboxLag
    expr: outbox_event_pending_total > 100
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "Outbox events pending"
      description: "{{ $value }} events are pending in outbox"
```

## Scaling Configuration

### Horizontal Pod Autoscaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: onboarding-workflow-hpa
  namespace: onboarding
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: onboarding-workflow
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Kafka Consumer Scaling

```yaml
# Increase partitions for high throughput
kafka-topics --alter --topic onboarding-command --partitions 12 --bootstrap-server kafka:9092

# Update consumer configuration
spring:
  kafka:
    consumer:
      concurrency: 6  # Match partition count
```

### Database Scaling

```sql
-- Add read replicas for reporting queries
-- Partition history tables by month
CREATE TABLE onboarding_history_y2024m01 PARTITION OF onboarding_history
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

## Security Configuration

### TLS Configuration

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    trust-store: classpath:truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
```

### Network Policies

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: onboarding-workflow-netpol
  namespace: onboarding
spec:
  podSelector:
    matchLabels:
      app: onboarding-workflow
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: kafka
    ports:
    - protocol: TCP
      port: 9092
```

## Backup and Recovery

### Database Backup

```bash
# Daily backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -h postgres -U onboarding -d onboarding_workflow > /backup/onboarding_workflow_$DATE.sql

# Upload to S3
aws s3 cp /backup/onboarding_workflow_$DATE.sql s3://backups/onboarding/
```

### Disaster Recovery

```yaml
# disaster-recovery.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: onboarding-restore
spec:
  template:
    spec:
      containers:
      - name: restore
        image: postgres:13
        command:
        - /bin/bash
        - -c
        - |
          psql -h postgres -U onboarding -d onboarding_workflow < /backup/backup.sql
        volumeMounts:
        - name: backup-volume
          mountPath: /backup
      volumes:
      - name: backup-volume
        persistentVolumeClaim:
          claimName: backup-pvc
      restartPolicy: Never
```

## Troubleshooting

### Common Issues

#### Pod CrashLoopBackOff

```bash
# Check pod logs
kubectl logs -f deployment/onboarding-workflow -n onboarding

# Check events
kubectl get events -n onboarding --sort-by=.metadata.creationTimestamp

# Check resource usage
kubectl top pods -n onboarding
```

#### Database Connection Issues

```bash
# Test database connectivity
kubectl exec -it deployment/onboarding-workflow -n onboarding -- bash
curl -f http://postgres:5432/

# Check connection pool
curl http://localhost:8081/actuator/metrics | jq '.names[] | select(contains("hikaricp"))'
```

#### Kafka Consumer Lag

```bash
# Check consumer lag
kafka-consumer-groups --bootstrap-server kafka:9092 --group onboarding-engine --describe

# Reset consumer offset if needed
kafka-consumer-groups --bootstrap-server kafka:9092 --group onboarding-engine --topic onboarding-command --reset-offsets --to-earliest --execute
```

#### High Memory Usage

```bash
# Generate heap dump
jcmd <pid> GC.heap_dump /tmp/heapdump.hprof

# Analyze with jhat or Eclipse MAT
jhat /tmp/heapdump.hprof
```

### Performance Tuning

#### JVM Tuning

```yaml
# Deployment environment variables
env:
- name: JAVA_OPTS
  value: >
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:G1HeapRegionSize=16m
    -XX:MinHeapFreeRatio=20
    -XX:MaxHeapFreeRatio=40
    -Xms2g
    -Xmx4g
    -XX:+PrintGCDetails
    -XX:+PrintGCTimeStamps
```

#### Database Tuning

```sql
-- PostgreSQL configuration
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET work_mem = '4MB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
ALTER SYSTEM SET wal_buffers = '16MB';
ALTER SYSTEM SET default_statistics_target = 100;
```

This deployment guide provides everything needed to deploy the Custom Workflow Engine in production with high availability, scalability, and observability.
