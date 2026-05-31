# sentrix-core

Core backend service for SentriX.

SentriX Core collects runtime metrics from Prometheus/LGTM, transforms them into a common feature schema, runs anomaly diagnosis via ML model server, stores diagnosis results, and exposes APIs for dashboard clients.

## Architecture

```text
Spring Boot Demo Server
        ↓
Prometheus / LGTM
        ↓
sentrix-core
  PrometheusMetricCollector
  SlidingWindowBuffer
  FeatureExtractor
        ↓
sentrix-model-server (FastAPI)
        ↓
Database
        ↓
Dashboard
```

## Responsibilities

* Collect runtime metrics from Prometheus
* Normalize metrics into SentriX Common Feature Schema
* Maintain sliding-window metric buffers
* Run anomaly diagnosis via ML model server
* Store diagnosis results and evidence features
* Expose dashboard APIs
* Integrate with the ML model server

## Tech Stack

* Java 21
* Spring Boot 3.5.14
* Gradle 8.14.5
* Spring Web
* Spring Data JPA
* Spring Scheduler
* Spring WebFlux (WebClient)
* MySQL 8.0
* Prometheus / LGTM
* Swagger / OpenAPI (springdoc 2.8.9)
* Docker

## Repository Structure

```text
src/main/java/com/sentrix/core
├── common/
├── config/
├── metric/
├── diagnosis/
├── model/
├── dashboard/
├── replay/
└── CoreApplication.java
```

## Getting Started

### Prerequisites

* Java 21
* Docker
* sentrix-core-db 컨테이너 실행 중
* sentrix-model-server 실행 중 (port 8000)
* sentrix-demo-server 실행 중 (port 8080)
* LGTM 스택 실행 중 (Prometheus port 9090)

### 1. DB 실행

```bash
docker compose up -d
```

`compose.yaml`이 `sentrix-core-db` 컨테이너를 **3308** 포트로 실행한다.

### 2. 로컬 실행

```bash
./gradlew bootRun
```

서버가 **8081** 포트로 실행된다.

### 3. 빌드

```bash
# 테스트 제외 빌드
./gradlew build -x test

# 테스트 포함 빌드
./gradlew build
```

### 4. Swagger UI

서버 실행 후 접속:

```
http://localhost:8081/swagger-ui.html
```

## Port 정리

| Service | Port |
|---|---|
| sentrix-demo-server | 8080 |
| sentrix-core (this) | 8081 |
| sentrix-model-server | 8000 |
| sentrix-core-db (MySQL) | 3308 |
| sdemo-db (MySQL) | 3307 |
| LGTM (Grafana) | 3000 |
| LGTM (Prometheus) | 9090 |

## Configuration

`src/main/resources/application.yml` 주요 설정:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3308/sentrix
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
    threshold: 0.2716
    schema-version: v1
```

## API Overview

### Metrics

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/metrics/current` | Prometheus에서 현재 raw feature 수집 |
| GET | `/api/metrics/buffer/status` | 슬라이딩 윈도우 버퍼 상태 조회 |
| GET | `/api/features/schema` | Common Feature Schema 조회 |
| POST | `/api/metrics/collect` | Prometheus metric 수동 수집 후 buffer 저장 |

### Diagnosis

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/diagnosis/run` | 현재 window 기반 수동 진단 실행 |
| GET | `/api/diagnosis/latest` | 최근 진단 결과 조회 |
| GET | `/api/diagnosis/history` | 진단 이력 조회 |
| GET | `/api/diagnosis/{diagnosisId}` | 특정 진단 결과 상세 조회 |

### Dashboard

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/dashboard/summary` | 대시보드 요약 정보 |
| GET | `/api/dashboard/timeline` | anomaly score 타임라인 |
| GET | `/api/dashboard/faults/distribution` | fault type 분포 |
| GET | `/api/dashboard/evidence/latest` | 최신 판단 근거 feature |

### Model Server

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/model/status` | ML Model Server 연결 상태 확인 |
| POST | `/api/model/diagnose` | Model Server `/diagnose` 호출 테스트 |
| POST | `/api/model/mock-diagnose` | ML 서버 미완성 시 mock 진단 결과 반환 |

## Feature Schema

SentriX Core converts raw Prometheus metrics into a shared feature schema.

### Raw Features (11)

| Feature | Unit | PromQL | Related Fault |
|---|---|---|---|
| `request_rate` | req/sec | `rate(http_server_requests_seconds_count[1m])` | - |
| `latency_p95` | seconds | `histogram_quantile(0.95, ...)` | LATENCY_SPIKE |
| `latency_p99` | seconds | `histogram_quantile(0.99, ...)` | LATENCY_SPIKE |
| `error_rate` | ratio | 5xx / total | ERROR_SPIKE |
| `process_cpu_usage` | ratio | `process_cpu_usage` | HIGH_CPU |
| `system_cpu_usage` | ratio | `system_cpu_usage` | HIGH_CPU |
| `jvm_memory_used` | bytes | `sum(jvm_memory_used_bytes{area="heap"})` | MEMORY_PRESSURE |
| `jvm_memory_max_ratio` | ratio | heap used / heap max | MEMORY_PRESSURE |
| `hikaricp_active` | count | `hikaricp_connections_active` | - |
| `hikaricp_pending` | count | `hikaricp_connections_pending` | - |
| `executor_active_threads` | count | `executor_active_threads` | - |

### Window Settings

```text
window_size : 300s
step_size   : 30s
statistics  : mean, std, max, min, slope
→ 11 × 5 = 55 model features
```

### Feature Naming Convention

```text
{raw_feature}_{statistic}

Examples:
  latency_p99_mean
  latency_p99_max
  process_cpu_usage_slope
```

## Fault Types

```text
NORMAL
HIGH_CPU
MEMORY_PRESSURE
LATENCY_SPIKE
ERROR_SPIKE
```

## Diagnosis Flow

```text
1. DiagnosisScheduler (30s interval)
2. PrometheusMetricCollector → raw feature 수집
3. SlidingWindowBuffer → 300s window 유지
4. FeatureExtractor → 55개 window feature 생성
5. ModelServerClient → POST /diagnose 호출
6. DiagnosisResultService → DB 저장
```

## Related Repositories

* `sentrix-model-server` — https://github.com/SentriXlab/sentrix-model-server (FastAPI ML inference server)
* `sentrix-demo-server` — https://github.com/SentriXlab/sentrix-demo-server (Target Spring Boot application for demo scenarios)
