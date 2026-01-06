# API Documentation

Complete API reference for the Custom Workflow Engine.

## Base URL

```
http://localhost:8080
```

## Authentication

All admin endpoints require authentication. Include the following header:

```
Authorization: Bearer <jwt-token>
X-Correlation-ID: <uuid>  # Optional, auto-generated if not provided
```

## Response Format

All responses follow this structure:

```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "requestId": "uuid",
    "correlationId": "uuid",
    "timestamp": "2024-01-06T12:00:00Z"
  }
}
```

Error responses:

```json
{
  "success": false,
  "error": {
    "code": "INVALID_STATE",
    "message": "Invalid state transition",
    "type": "BUSINESS_ERROR",
    "details": { ... }
  },
  "meta": { ... }
}
```

---

## User APIs

### Start Onboarding

Create a new onboarding workflow instance.

**Endpoint:** `POST /api/onboarding/start`

**Request:**
```json
{
  "userId": "user123",
  "flowVersion": "v1"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "instanceId": "550e8400-e29b-41d4-a716-446655440000",
    "currentState": "PHONE_ENTERED",
    "status": "ACTIVE"
  }
}
```

**Error Codes:**
- `INVALID_USER`: User already has active onboarding

### Get Onboarding Status

Retrieve current status and available actions.

**Endpoint:** `GET /api/onboarding/{instanceId}/status`

**Response:**
```json
{
  "success": true,
  "data": {
    "instanceId": "550e8400-e29b-41d4-a716-446655440000",
    "currentState": "OTP_VERIFIED",
    "uiStep": "otp",
    "progress": 25,
    "allowedActions": [
      {
        "action": "NEXT",
        "label": "Continue to Profile",
        "type": "PRIMARY"
      }
    ],
    "error": null,
    "retry": {
      "count": 0,
      "max": 3,
      "nextRetryAt": null
    }
  }
}
```

### Perform Action

Execute a workflow action.

**Endpoint:** `POST /api/onboarding/{instanceId}/action`

**Request:**
```json
{
  "action": "NEXT",
  "requestId": "uuid"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "instanceId": "550e8400-e29b-41d4-a716-446655440000",
    "previousState": "OTP_VERIFIED",
    "currentState": "PROFILE_COMPLETED",
    "action": "NEXT"
  }
}
```

**Error Codes:**
- `INVALID_TRANSITION`: Action not allowed in current state
- `INVALID_ARGUMENT`: Missing required parameters
- `DUPLICATE_REQUEST`: Request ID already processed

---

## Admin APIs

### Dashboard Overview

Get system-wide statistics and metrics.

**Endpoint:** `GET /admin/onboarding/dashboard`

**Response:**
```json
{
  "success": true,
  "data": {
    "totalInstances": 1250,
    "statusCounts": {
      "ACTIVE": 450,
      "COMPLETED": 780,
      "CANCELLED": 15,
      "FAILED": 5
    },
    "stateCounts": {
      "EKYC_PENDING": 120,
      "AML_PENDING": 80,
      "OTP_VERIFIED": 250
    },
    "activeRate": 36.0
  }
}
```

### List Instances

Get paginated list of onboarding instances with filtering.

**Endpoint:** `GET /admin/onboarding`

**Query Parameters:**
- `page` (int, default: 0): Page number
- `size` (int, default: 20): Page size
- `status` (string): Filter by status
- `state` (string): Filter by current state
- `userId` (string): Filter by user ID

**Response:**
```json
{
  "success": true,
  "data": {
    "instances": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "userId": "user123",
        "currentState": "EKYC_PENDING",
        "status": "ACTIVE",
        "flowVersion": "v1",
        "stateStartedAt": "2024-01-06T10:30:00Z",
        "createdAt": "2024-01-06T10:00:00Z"
      }
    ],
    "totalElements": 450,
    "totalPages": 23,
    "currentPage": 0,
    "size": 20
  }
}
```

### Get Instance Details

Get detailed information about a specific instance.

**Endpoint:** `GET /admin/onboarding/{instanceId}`

**Response:**
```json
{
  "success": true,
  "data": {
    "instance": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "userId": "user123",
      "currentState": "EKYC_PENDING",
      "status": "ACTIVE",
      "flowVersion": "v1",
      "version": 5,
      "stateStartedAt": "2024-01-06T10:30:00Z",
      "createdAt": "2024-01-06T10:00:00Z",
      "updatedAt": "2024-01-06T10:35:00Z"
    },
    "actions": [
      {
        "action": "RETRY",
        "label": "Retry eKYC",
        "type": "SECONDARY"
      },
      {
        "action": "TIMEOUT",
        "label": "Force Timeout",
        "type": "WARNING"
      }
    ]
  }
}
```

### Admin Action

Perform administrative actions on instances.

**Endpoint:** `POST /admin/onboarding/{instanceId}/action`

**Request:**
```json
{
  "action": "RETRY",
  "operator": "admin_user",
  "comment": "Manual retry due to vendor timeout"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "instanceId": "550e8400-e29b-41d4-a716-446655440000",
    "action": "RETRY",
    "operator": "admin_user",
    "currentState": "EKYC_PENDING",
    "message": "Admin action processed successfully"
  }
}
```

---

## Human Task APIs

### Get Tasks

Get human tasks for a user/role combination.

**Endpoint:** `GET /admin/human-tasks`

**Query Parameters:**
- `userId` (string): User ID
- `role` (string): User role (RISK_OFFICER, COMPLIANCE_OFFICER)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "taskId": "task-123",
      "instanceId": "550e8400-e29b-41d4-a716-446655440000",
      "taskType": "AML_REVIEW",
      "status": "OPEN",
      "priority": "HIGH",
      "assignedRole": "RISK_OFFICER",
      "assignedUser": null,
      "createdAt": "2024-01-06T10:30:00Z",
      "dueAt": "2024-01-06T11:30:00Z",
      "isOverdue": false
    }
  ]
}
```

### Get Task Details

Get detailed information about a specific task.

**Endpoint:** `GET /admin/human-tasks/{taskId}`

**Response:**
```json
{
  "success": true,
  "data": {
    "taskId": "task-123",
    "instanceId": "550e8400-e29b-41d4-a716-446655440000",
    "taskType": "AML_REVIEW",
    "status": "CLAIMED",
    "payload": {
      "userId": "user123",
      "riskScore": 0.75,
      "amlFlags": ["PEP_MATCH"]
    },
    "assignedUser": "risk_officer_1",
    "claimedAt": "2024-01-06T10:35:00Z"
  }
}
```

### Claim Task

Claim a task for processing.

**Endpoint:** `POST /admin/human-tasks/{taskId}/claim`

**Request:**
```json
{
  "userId": "risk_officer_1"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "taskId": "task-123",
    "status": "CLAIMED",
    "assignedUser": "risk_officer_1",
    "claimedAt": "2024-01-06T10:35:00Z",
    "message": "Task claimed successfully"
  }
}
```

### Complete Task

Complete a claimed task with a decision.

**Endpoint:** `POST /admin/human-tasks/{taskId}/complete`

**Request:**
```json
{
  "userId": "risk_officer_1",
  "result": "CLEAR",
  "comment": "AML check passed, no issues found"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "taskId": "task-123",
    "status": "COMPLETED",
    "result": "CLEAR",
    "completedAt": "2024-01-06T10:40:00Z",
    "message": "Task completed successfully"
  }
}
```

### Get Overdue Tasks

Get all overdue human tasks.

**Endpoint:** `GET /admin/human-tasks/overdue`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "taskId": "task-456",
      "instanceId": "550e8400-e29b-41d4-a716-446655440001",
      "taskType": "EKYC_REVIEW",
      "dueAt": "2024-01-06T09:00:00Z",
      "overdueByMinutes": 120
    }
  ]
}
```

### Human Task Dashboard

Get human task statistics.

**Endpoint:** `GET /admin/human-tasks/dashboard`

**Response:**
```json
{
  "success": true,
  "data": {
    "totalOpen": 25,
    "totalClaimed": 8,
    "totalCompleted": 150,
    "riskOfficerOpen": 15,
    "complianceOfficerOpen": 10,
    "overdueCount": 3
  }
}
```

---

## Monitoring APIs

### Health Check

**Endpoint:** `GET /actuator/health`

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "kafka": {
      "status": "UP"
    }
  }
}
```

### Metrics

**Endpoint:** `GET /actuator/metrics`

**Response:**
```json
{
  "names": [
    "workflow.state.transition",
    "workflow.step.execution",
    "workflow.error",
    "workflow.human_task",
    "workflow.incident"
  ]
}
```

### Prometheus Metrics

**Endpoint:** `GET /actuator/prometheus`

Returns metrics in Prometheus format for monitoring systems.

---

## Error Codes

### Common Error Codes

| Code | Type | Description |
|------|------|-------------|
| `INVALID_ARGUMENT` | CLIENT_ERROR | Missing or invalid request parameters |
| `INVALID_STATE` | BUSINESS_ERROR | Action not allowed in current state |
| `INVALID_TRANSITION` | BUSINESS_ERROR | Transition conditions not met |
| `DUPLICATE_REQUEST` | CLIENT_ERROR | Request ID already processed |
| `INSTANCE_NOT_FOUND` | CLIENT_ERROR | Workflow instance does not exist |
| `ACCESS_DENIED` | SECURITY_ERROR | Insufficient permissions |
| `INTERNAL_ERROR` | SYSTEM_ERROR | Unexpected system error |

### Workflow-Specific Errors

| Code | Type | Description |
|------|------|-------------|
| `STEP_EXECUTION_FAILED` | SYSTEM_ERROR | Business logic execution failed |
| `TIMEOUT_EXCEEDED` | BUSINESS_ERROR | Operation timed out |
| `RETRY_LIMIT_EXCEEDED` | BUSINESS_ERROR | Maximum retry attempts reached |
| `RULE_EVALUATION_FAILED` | SYSTEM_ERROR | Transition rule evaluation error |

### Human Task Errors

| Code | Type | Description |
|------|------|-------------|
| `TASK_NOT_FOUND` | CLIENT_ERROR | Human task does not exist |
| `TASK_NOT_CLAIMED` | BUSINESS_ERROR | Task must be claimed before completion |
| `TASK_ALREADY_CLAIMED` | BUSINESS_ERROR | Task already claimed by another user |

---

## Rate Limiting

- User APIs: 100 requests/minute per user
- Admin APIs: 1000 requests/minute per admin user
- Monitoring APIs: Unlimited (internal)

Rate limit headers are included in responses:

```
X-Rate-Limit-Limit: 100
X-Rate-Limit-Remaining: 95
X-Rate-Limit-Reset: 1640995200
```

---

## Webhooks

The system can send webhook notifications for important events:

### Configuration

```yaml
webhooks:
  enabled: true
  endpoints:
    - url: "https://api.company.com/webhooks/workflow"
      events: ["INSTANCE_COMPLETED", "ERROR_OCCURRED"]
      secret: "webhook-secret"
```

### Event Payload

```json
{
  "eventType": "INSTANCE_COMPLETED",
  "instanceId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user123",
  "timestamp": "2024-01-06T12:00:00Z",
  "data": {
    "finalState": "COMPLETED",
    "totalDuration": 1800000
  }
}
```

---

## SDKs and Libraries

### JavaScript SDK

```javascript
import { WorkflowClient } from '@company/workflow-sdk';

const client = new WorkflowClient({
  baseUrl: 'http://localhost:8080',
  apiKey: 'your-api-key'
});

// Start onboarding
const instance = await client.startOnboarding({
  userId: 'user123'
});

// Perform action
await client.performAction(instance.id, {
  action: 'NEXT',
  requestId: 'uuid'
});
```

### Java SDK

```java
WorkflowClient client = new WorkflowClient("http://localhost:8080");

OnboardingInstance instance = client.startOnboarding("user123");

ActionResult result = client.performAction(instance.getId(),
    ActionCommand.next("request-id"));
```

---

## Versioning

API versioning follows semantic versioning:

- **Breaking changes**: New major version (v2, v3, etc.)
- **Additions**: Minor version increments
- **Bug fixes**: Patch version increments

Current version: **v1**

### Backward Compatibility

- All v1 endpoints remain supported
- Deprecation warnings added 6 months before removal
- Migration guides provided for major version changes

---

## Support

### Getting Help

1. Check this documentation first
2. Review the [Architecture Guide](ARCHITECTURE.md)
3. Check the [Workflow Guide](WORKFLOW_GUIDE.md) for customization
4. Contact the development team

### Common Issues

#### High Error Rates
- Check database connectivity
- Verify Kafka broker status
- Review application logs for stack traces

#### Slow Performance
- Monitor database query performance
- Check Kafka consumer lag
- Review thread pool utilization

#### Stuck Workflows
- Use admin APIs to manually retry
- Check for failed external service calls
- Review incident logs for root causes
