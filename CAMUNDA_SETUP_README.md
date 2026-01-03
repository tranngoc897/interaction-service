# Camunda 7 Local Docker Setup
Truy cập Camunda Web Apps

- **Camunda Tasklist**: http://localhost:8080/camunda/app/tasklist/
- **Camunda Cockpit**: http://localhost:8080/camunda/app/cockpit/
- **Camunda Admin**: http://localhost:8080/camunda/app/admin/

**Default credentials:**
- Username: `demo`
- Password: `demo`

## 🏗️ Cấu trúc Docker Setup

```
camunda-docker-compose.yml
├── camunda-db (PostgreSQL 13)
│   ├── Database: camunda
│   ├── User: camunda
│   ├── Password: camunda
│   └── Port: 5432
└── camunda (Camunda BPM Platform 7.21.0)
    ├── Port: 8080
    ├── BPMN processes: ./bpmn-processes/
    └── Database: camunda-db
```

## 📁 BPMN Processes

BPMN processes được mount vào container tại `/camunda/conf/bpmn/`
### Sample Process: `onboarding-process.bpmn`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions ...>
  <bpmn:process id="onboarding-process" name="Customer Onboarding Process" isExecutable="true">
    <!-- Process definition -->
  </bpmn:process>
</bpmn:definitions>
```

### Quy trình Onboarding:
1. **Collect Personal Information** (User Task)
2. **Upload Documents** (User Task)
3. **AML/KYC Compliance Check** (Service Task)
4. **Manual Review** (nếu cần - User Task)
5. **Create Account** (Service Task)
6. **Send Welcome Email** (Service Task)

## 🔧 Sử dụng với Spring Boot App

### 1. Cấu hình Database
Trong `application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/camunda
    username: camunda
    password: camunda
    driver-class-name: org.postgresql.Driver

camunda:
  bpm:
    database:
      type: postgres
      schema-update: true
```
### 2. Deploy BPMN Process qua API
```bash
# Deploy onboarding process
curl -X POST "http://localhost:8081/api/bpmn/deploy?processKey=onboarding&processName=Onboarding" \
  -H "Content-Type: application/xml" \
  --data-binary @bpmn-processes/onboarding-process.bpmn
```
### 3. Start Process Instance

```bash
# Start onboarding process
curl -X POST "http://localhost:8081/api/bpmn/start?processDefinitionKey=onboarding&businessKey=customer-123" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "123",
    "customerType": "INDIVIDUAL",
    "productType": "SAVINGS_ACCOUNT"
  }'
```
### 4. Signal Process

```bash
# Signal document upload completion
curl -X POST "http://localhost:8081/api/bpmn/signal/9dcc031d-e861-11f0-bd72-7aad302a1eea?signalName=documentsUploaded" \
  -H "Content-Type: application/json" \
  -d '{
    "documentCount": 3,
    "verified": true
  }'
```

## 📊 Database Schema

Camunda tự động tạo các bảng:

- `ACT_HI_*` - History tables
- `ACT_RU_*` - Runtime tables
- `ACT_RE_*` - Repository tables
- `ACT_ID_*` - Identity tables

## 🎯 Next Steps

1. **Start Camunda**: `docker-compose -f camunda-docker-compose.yml up -d`
2. **Access Web UI**: http://localhost:8080/camunda
3. **Deploy Process**: Sử dụng REST API hoặc Spring Boot integration
4. **Monitor Processes**: Sử dụng Cockpit và Tasklist
5. **Integrate with Cases**: Sử dụng CaseService BPMN methods

**Camunda 7 đã sẵn sàng cho Onboarding Integration! 🚀**
