# Flowable Setup Guide

## 🚀 Triển khai trên Kubernetes

### Yêu cầu hệ thống:
- Kubernetes cluster (v1.19+)
- kubectl configured
- Docker registry access
- NGINX Ingress Controller (optional)

### Architecture Options:

#### 🏗️ **Option 1: EMBEDDED SHARED (Development)**
```bash
# Flowable embedded trong Spring Boot, 1 DB chung
./deploy-k8s.sh default latest embedded-shared
```

**Kiến trúc:**
```
Spring Boot App (with embedded Flowable)
├── Business Logic
├── Flowable Engine (embedded)
└── Shared PostgreSQL DB
```

**Ưu điểm:**
- ✅ Đơn giản nhất
- ✅ Ít infrastructure
- ✅ Dễ develop/debug

**Nhược điểm:**
- ❌ Khó scale BPMN riêng
- ❌ App restart = Flowable restart

#### 🏗️ **Option 2: EMBEDDED SEPARATE (Production Basic)**
```bash
# Flowable embedded, nhưng 2 DB riêng
./deploy-k8s.sh production latest embedded-separate
```

**Kiến trúc:**
```
Spring Boot App (with embedded Flowable)
├── Business Logic + Flowable Engine
└── Separate DBs:
    ├── App PostgreSQL
    └── Flowable PostgreSQL
```

**Ưu điểm:**
- ✅ Balance simplicity & isolation
- ✅ Scale app = scale Flowable
- ✅ Good performance isolation

**Nhược điểm:**
- ❌ Still coupled deployment

#### 🏗️ **Option 3: SEPARATE SERVICES (Production Enterprise)**
```bash
# Flowable và App chạy độc lập, 2 DB riêng
./deploy-k8s.sh production latest separate-services
```

**Kiến trúc:**
```
Flowable Platform (Standalone)
├── Flowable UI
├── BPMN Engine
└── Flowable PostgreSQL

Interaction Service (Spring Boot)
├── Business Logic only
├── REST client to Flowable
└── App PostgreSQL

Shared Kafka
```

**Ưu điểm:**
- ✅ **Hoàn toàn độc lập scaling**
- ✅ **Zero downtime deployment**
- ✅ **Best isolation & security**
- ✅ **Microservices ready**

**Nhược điểm:**
- ❌ Phức tạp nhất
- ❌ Network latency
- ❌ Distributed transactions

### Quick Deploy Commands:
```bash
# Development (Embedded + Shared DB)
./deploy-k8s.sh dev latest embedded-shared

# Production Basic (Embedded + Separate DBs)
./deploy-k8s.sh prod v1.0.0 embedded-separate

# Production Enterprise (Separate Services)
./deploy-k8s.sh prod v1.0.0 separate-services
```

### K8s Resources được tạo:
- **PostgreSQL StatefulSet** - Database cho Flowable
- **Kafka Deployment** - Message broker
- **Interaction Service Deployment** - App với embedded Flowable
- **ConfigMaps & Secrets** - Cấu hình và credentials
- **Services & Ingress** - Network access
- **HPA & Network Policies** - Auto-scaling và security

### Truy cập ứng dụng:
```bash
# Port forward để test local
kubectl port-forward svc/interaction-service 8080:80

# Access via Ingress (nếu có)
curl http://interaction-service.local
```

---

## 🐳 Local Development Setups

### Option A: Quick Docker Setup (Embedded Flowable)
```bash
# Start all services with embedded Flowable
docker-compose -f flowable-docker-compose.yml up -d

# Access Flowable UI
open http://localhost:8080/flowable-task
```

### Option B: Separate Services Setup (Production-like)
```bash
# Start Flowable + App as separate services
./run-separate-services.sh

# Stop all services
./stop-separate-services.sh
```

**Separate Services Endpoints:**
- **Flowable UI**: http://localhost:8080/flowable-task
- **Flowable REST**: http://localhost:8080/flowable-rest
- **App Service**: http://localhost:8081

**Default credentials:**
- Username: `admin`
- Password: `test`

## 🏗️ Cấu trúc Docker Setup

```
flowable-docker-compose.yml
├── flowable-db (PostgreSQL 13)
│   ├── Database: flowable
│   ├── User: flowable
│   ├── Password: flowable
│   └── Port: 5432
└── flowable (Flowable BPM Platform 7.0.1)
    ├── Port: 8080
    ├── BPMN processes: ./bpmn-processes/
    └── Database: flowable-db
```

## 📁 BPMN Processes

BPMN processes được mount vào container tại `/opt/flowable/work/`
### Sample Process: `onboarding-process.bpmn`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:flowable="http://flowable.org/bpmn">
  <bpmn:process id="onboarding" name="Customer Onboarding Process" isExecutable="true">
    <!-- Process definition with Flowable extensions -->
  </bpmn:process>
</bpmn:definitions>
```

**Key Changes from Camunda:**
- `xmlns:camunda` → `xmlns:flowable`
- `camunda:class` → `flowable:class`
- `camunda:async` → `flowable:async`
- `camunda:decisionRef` → `flowable:decisionRef`

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
    url: jdbc:postgresql://localhost:5432/flowable
    username: flowable
    password: flowable
    driver-class-name: org.postgresql.Driver

flowable:
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

Flowable tự động tạo các bảng:

- `FLW_*` - Flowable tables (replaces ACT_*)
- `ACT_HI_*` - History tables (legacy)
- `ACT_RU_*` - Runtime tables (legacy)
- `ACT_RE_*` - Repository tables (legacy)

## 🎯 Next Steps

1. **Start Flowable**: `docker-compose -f flowable-docker-compose.yml up -d`
2. **Access Web UI**: http://localhost:8080/flowable-task
3. **Deploy Process**: Sử dụng REST API hoặc Spring Boot integration
4. **Monitor Processes**: Sử dụng Flowable Admin và Task
5. **Integrate with Cases**: Sử dụng CaseService BPMN methods

**Flowable đã sẵn sàng cho Onboarding Integration! 🚀**
