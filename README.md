# sentrix-core

Core backend service for SentriX.

SentriX Core collects runtime metrics from Prometheus/LGTM, transforms them into a common feature schema, runs anomaly diagnosis, stores diagnosis results, and exposes APIs for dashboard clients.

## Architecture

```text
Spring Boot Demo Server
        ↓
Prometheus / LGTM
        ↓
sentrix-core
        ↓
Rule-based Detector or ML Model Server
        ↓
Database
        ↓
Dashboard
```

## Responsibilities

* Collect runtime metrics from Prometheus
* Normalize metrics into SentriX common features
* Maintain sliding-window metric buffers
* Run anomaly diagnosis
* Store diagnosis results and evidence features
* Expose dashboard APIs
* Integrate with the ML model server

## Tech Stack

* Java 17
* Spring Boot 3
* Spring Web
* Spring Data JPA
* Spring Scheduler
* MySQL
* Prometheus / LGTM
* Swagger / OpenAPI
* Docker

## Repository Structure

```text
src/main/java/com/sentrix/core
├── common
├── metric
├── diagnosis
├── model
├── dashboard
├── replay
└── SentrixCoreApplication.java
```

## Getting Started

### Prerequisites

* Java 17+
* Docker
* MySQL
* Prometheus or LGTM stack
* Running `sentrix-demo-server`

### Run locally

```bash
./gradlew bootRun
```

### Run tests

```bash
./gradlew test
```

## Configuration

Create `application-local.yml` or set environment variables.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3307/sentrix
    username: root
    password: 1234

sentrix:
  prometheus:
    base-url: http://localhost:9090
  model-server:
    base-url: http://localhost:8000
  diagnosis:
    window-size-seconds: 300
    step-size-seconds: 30
```

## API Overview

### Metrics

| Method | Endpoint                     | Description                      |
| ------ | ---------------------------- | -------------------------------- |
| GET    | `/api/metrics/current`       | Get current runtime metrics      |
| GET    | `/api/metrics/buffer/status` | Get sliding-window buffer status |
| GET    | `/api/features/schema`       | Get active feature schema        |

### Diagnosis

| Method | Endpoint                 | Description                 |
| ------ | ------------------------ | --------------------------- |
| POST   | `/api/diagnosis/run`     | Run diagnosis manually      |
| GET    | `/api/diagnosis/latest`  | Get latest diagnosis result |
| GET    | `/api/diagnosis/history` | Get diagnosis history       |

### Dashboard

| Method | Endpoint                             | Description                |
| ------ | ------------------------------------ | -------------------------- |
| GET    | `/api/dashboard/summary`             | Get dashboard summary      |
| GET    | `/api/dashboard/timeline`            | Get anomaly score timeline |
| GET    | `/api/dashboard/faults/distribution` | Get fault distribution     |

### Model Server

| Method | Endpoint              | Description                             |
| ------ | --------------------- | --------------------------------------- |
| GET    | `/api/model/status`   | Check model server status               |
| POST   | `/api/model/diagnose` | Proxy diagnosis request to model server |

## Feature Schema

SentriX Core converts raw Prometheus metrics into a shared feature schema.

Initial features:

```text
request_rate
latency_p95
latency_p99
error_rate
process_cpu_usage
jvm_memory_used
jvm_memory_max_ratio
db_connections_active
hikaricp_pending_connections
jvm_threads_live
```

Window statistics:

```text
mean
std
max
min
slope
```

Example generated features:

```text
latency_p99_mean
latency_p99_max
latency_p99_slope
process_cpu_usage_mean
hikaricp_pending_connections_max
```

## Diagnosis Modes

### Rule-based diagnosis

Used during initial development and as a fallback.

Example:

```text
latency_p99 ↑ + hikaricp_pending_connections ↑ → DB_DELAY
process_cpu_usage ↑ → HIGH_CPU
error_rate ↑ → ERROR_SPIKE
```

### ML-based diagnosis

When `sentrix-model-server` is available, SentriX Core sends feature vectors to the model server and stores the returned diagnosis result.

## Fault Types

```text
NORMAL
DB_DELAY
HIGH_CPU
MEMORY_PRESSURE
HIGH_LOAD
ERROR_SPIKE
```

## Related Repositories

* `sentrix-demo-server` — target Spring Boot application used to generate normal and fault scenarios
* `sentrix-core` — backend service for metric collection, diagnosis orchestration, persistence, and dashboard APIs
* `sentrix-model-server` — ML inference server
* `sentrix-dashboard` — dashboard frontend

## Development Status

Initial target:

```http
GET /api/metrics/current
```

This endpoint collects metrics from Prometheus and returns them as SentriX features.
