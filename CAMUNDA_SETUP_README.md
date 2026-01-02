# Camunda 7 Local Docker Setup

Hướng dẫn cài đặt và sử dụng Camunda 7 với Docker cho Onboarding Integration.

## 📋 Yêu cầu hệ thống

- Docker & Docker Compose
- Java 8+ (cho Spring Boot app)
- PostgreSQL (tự động tạo qua Docker)

## 🚀 Cài đặt và chạy Camunda 7

### 1. Khởi động Camunda với Docker Compose

```bash
# Từ thư mục project
cd /Users/ngoctran/Coding/workflow/interaction-service

# Khởi động Camunda và PostgreSQL
docker-compose -f camunda-docker-compose.yml up -d
```

### 2. Kiểm tra trạng thái

```bash
# Kiểm tra containers đang chạy
docker-compose -f camunda-docker-compose.yml ps

# Xem logs
docker-compose -f camunda-docker-compose.yml logs -f camunda
```

### 3. Truy cập Camunda Web Apps

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
curl -X POST "http://localhost:8080/api/bpmn/deploy?processKey=onboarding&processName=Onboarding" \
  -H "Content-Type: application/xml" \
  --data-binary @bpmn-processes/onboarding-process.bpmn
```

### 3. Start Process Instance

```bash
# Start onboarding process
curl -X POST "http://localhost:8080/api/bpmn/start?processDefinitionKey=onboarding&businessKey=customer-123" \
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
curl -X POST "http://localhost:8080/api/bpmn/signal/process-instance-456?signalName=documentsUploaded" \
  -H "Content-Type: application/json" \
  -d '{
    "documentCount": 3,
    "verified": true
  }'
```

## 🛠️ Development Workflow

### 1. BPMN Process Design

- Sử dụng **Camunda Modeler** để design BPMN processes
- Lưu files vào `bpmn-processes/` directory
- Docker sẽ tự động mount và deploy

### 2. Java Delegates

Tạo Java delegates cho Service Tasks:

```java
@Component
public class ComplianceDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // AML/KYC compliance logic
        String customerId = (String) execution.getVariable("customerId");
        // ... compliance check logic
        execution.setVariable("complianceStatus", "PASSED");
    }
}
```

### 3. Process Monitoring

- **Cockpit**: Monitor process instances, performance metrics
- **Tasklist**: Complete user tasks
- **Admin**: User/role management

## 📊 Database Schema

Camunda tự động tạo các bảng:

- `ACT_HI_*` - History tables
- `ACT_RU_*` - Runtime tables
- `ACT_RE_*` - Repository tables
- `ACT_ID_*` - Identity tables

## 🔄 Integration với Case Management

### CaseService BPMN Methods:

```java
// Start BPMN process for case
caseService.startBpmnProcess(caseId, "onboarding-process", variables);

// Signal BPMN process
caseService.signalBpmnProcess(caseId, "documentsUploaded", signalData);

// Update process variables
caseService.updateBpmnVariables(caseId, Map.of("riskLevel", "LOW"));

// Check process status
boolean active = caseService.isBpmnProcessActive(caseId);
```

## 🐛 Troubleshooting

### Container không start được

```bash
# Check logs
docker-compose -f camunda-docker-compose.yml logs camunda-db
docker-compose -f camunda-docker-compose.yml logs camunda

# Restart services
docker-compose -f camunda-docker-compose.yml restart
```

### Database connection issues

```bash
# Check PostgreSQL
docker exec -it camunda-postgres pg_isready -U camunda -d camunda

# Reset database
docker-compose -f camunda-docker-compose.yml down -v
docker-compose -f camunda-docker-compose.yml up -d
```

### BPMN deployment fails

```bash
# Check BPMN XML syntax
xmllint --noout bpmn-processes/onboarding-process.bpmn

# Check Camunda logs
docker-compose -f camunda-docker-compose.yml logs camunda
```

## 🛑 Dừng và dọn dẹp

```bash
# Stop services
docker-compose -f camunda-docker-compose.yml down

# Stop and remove volumes (xóa data)
docker-compose -f camunda-docker-compose.yml down -v

# Remove images
docker-compose -f camunda-docker-compose.yml down --rmi all
```

## 📚 Tài liệu tham khảo

- [Camunda BPM Documentation](https://docs.camunda.org/)
- [Camunda Docker Images](https://hub.docker.com/r/camunda/camunda-bpm-platform/)
- [BPMN 2.0 Specification](https://www.omg.org/spec/BPMN/2.0/)

## 🎯 Next Steps

1. **Start Camunda**: `docker-compose -f camunda-docker-compose.yml up -d`
2. **Access Web UI**: http://localhost:8080/camunda
3. **Deploy Process**: Sử dụng REST API hoặc Spring Boot integration
4. **Monitor Processes**: Sử dụng Cockpit và Tasklist
5. **Integrate with Cases**: Sử dụng CaseService BPMN methods

**Camunda 7 đã sẵn sàng cho Onboarding Integration! 🚀**
