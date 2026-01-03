#!/bin/bash

# Flowable Migration Deployment Script for Kubernetes
# Usage: ./deploy-k8s.sh [namespace] [image-tag] [architecture]
# Architectures: embedded-shared (default) | embedded-separate | separate-services

set -e

NAMESPACE=${1:-default}
IMAGE_TAG=${2:-latest}
ARCHITECTURE=${3:-embedded-shared}  # embedded-shared | embedded-separate | separate-services

echo "🚀 Deploying Flowable Migration to Kubernetes"
echo "📦 Namespace: $NAMESPACE"
echo "🏷️  Image Tag: $IMAGE_TAG"
echo "🏗️  Architecture: $ARCHITECTURE"

# Validate architecture
if [[ "$ARCHITECTURE" != "embedded-shared" && "$ARCHITECTURE" != "embedded-separate" && "$ARCHITECTURE" != "separate-services" ]]; then
    echo "❌ Invalid architecture. Use 'embedded-shared', 'embedded-separate', or 'separate-services'"
    exit 1
fi

# Create namespace if it doesn't exist
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Build and push Docker image
echo "🐳 Building Docker image..."
docker build -t ngoctran/interaction-service:$IMAGE_TAG .

echo "📤 Pushing Docker image..."
docker push ngoctran/interaction-service:$IMAGE_TAG

# Encode BPMN file for ConfigMap
echo "📄 Encoding BPMN file..."
BPMN_B64=$(base64 -w 0 bpmn-processes/onboarding-process.bpmn)

# Create temporary ConfigMap with encoded BPMN
cat > k8s-deployment-temp.yaml << EOF
---
# BPMN Processes ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: bpmn-processes
  namespace: $NAMESPACE
binaryData:
  onboarding-process.bpmn: $BPMN_B64
EOF

# Deploy to Kubernetes based on architecture
echo "⚙️  Deploying to Kubernetes (Architecture: $ARCHITECTURE)..."

case $ARCHITECTURE in
    "embedded-shared")
        DEPLOYMENT_FILE="k8s-deployment.yaml"
        POSTGRES_LABEL="app=flowable-postgres"
        SERVICE_LABEL="app=interaction-service"
        ;;
    "embedded-separate")
        DEPLOYMENT_FILE="k8s-deployment-separate-db.yaml"
        POSTGRES_LABEL="app in (flowable-postgres,app-postgres)"
        SERVICE_LABEL="app=interaction-service"
        ;;
    "separate-services")
        DEPLOYMENT_FILE="k8s-separate-services.yaml"
        POSTGRES_LABEL="app in (flowable-postgres,app-postgres)"
        SERVICE_LABEL="app=interaction-service"
        FLOWABLE_LABEL="app=flowable-platform"
        ;;
    *)
        echo "❌ Unsupported architecture"
        exit 1
        ;;
esac

# Apply database and infrastructure first
kubectl apply -f $DEPLOYMENT_FILE -n $NAMESPACE

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
kubectl wait --for=condition=ready pod -l $POSTGRES_LABEL -n $NAMESPACE --timeout=300s

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
kubectl wait --for=condition=ready pod -l app=kafka -n $NAMESPACE --timeout=300s

# For separate services, wait for Flowable to be ready first
if [[ "$ARCHITECTURE" == "separate-services" ]]; then
    echo "⏳ Waiting for Flowable Platform to be ready..."
    kubectl wait --for=condition=ready pod -l $FLOWABLE_LABEL -n $NAMESPACE --timeout=300s
fi

# Apply BPMN ConfigMap
kubectl apply -f k8s-deployment-temp.yaml -n $NAMESPACE

# Deploy application
kubectl apply -f $DEPLOYMENT_FILE -n $NAMESPACE

# Wait for application to be ready
echo "⏳ Waiting for application to be ready..."
kubectl wait --for=condition=ready pod -l $SERVICE_LABEL -n $NAMESPACE --timeout=300s

# Get service information
echo "📊 Deployment Status:"
kubectl get pods -n $NAMESPACE
kubectl get services -n $NAMESPACE
kubectl get ingress -n $NAMESPACE

# Port forward for local access (optional)
echo "🔗 To access the service locally:"
echo "kubectl port-forward svc/interaction-service 8080:80 -n $NAMESPACE"

# Clean up temporary file
rm k8s-deployment-temp.yaml

echo "✅ Deployment completed successfully!"
echo "🌐 Service available at: http://interaction-service.local"
echo "📊 Check status: kubectl get all -n $NAMESPACE"
