# CI/CD Architecture – Selenium Automation Framework

## 1. Overview

This project uses a **Dockerized CI/CD setup with configuration-as-code** to execute Selenium + TestNG automation reliably across environments and browsers.

Key principles:

* **Configuration as code** (pipeline, Jenkins config, Docker)
* **Reproducible builds** using Docker
* **Optimized execution** via pre-warmed build agents
* **Parallel cross-browser execution** using Selenium Grid
* **Structured reporting & notifications**

## 2. Quick Start

```bash
# 1. Local Jenkins Setup

For local development and validation, Jenkins is started using Docker Compose
with a JCasC-enabled configuration:

docker compose up -d

This starts a fully configured Jenkins controller using **Configuration as Code (JCasC)**,
including plugins, credentials, jobs, and system settings — with no manual UI setup.

This local setup exists to:

* Validate full reproducibility
* Ensure consistent Jenkins behavior across environments
* Validate CI/CD behavior before pushing changes

# 2. Build agents
docker build -f cicd/Dockerfile -t flight-booking-agent:latest .
docker build -f cicd/Dockerfile-prewarmed -t flight-booking-agent-prewarmed:latest .


# 3. Push code and trigger pipeline
Pipeline handles Selenium Grid, parallel execution, reporting
```

## 3. High-Level Architecture

```
Developer Commit
      ↓
Git Repository (Multibranch)
      ↓
Jenkins (Dockerized, JCasC)
      ↓
Pre-warmed Docker Agent
      ↓
Selenium Grid (Docker Compose)
      ↓
Test Execution (Chrome / Firefox in parallel)
      ↓
Reports + Dashboards + Notifications
```

## 4. Jenkins Controller (Infrastructure as Code)

### Key Characteristics

* Runs as a **Docker container**
* Fully configured using **Jenkins Configuration as Code (JCasC)**
* Zero manual UI setup after container startup

### What is configured via JCasC

* Security realm (admin user)
* Authorization strategy
* Global shared libraries
* SMTP / Email extension
* Credentials (Qase token, email lists)
* Jenkins URL & global settings

**Benefits**

* Reproducible Jenkins setup
* Easy migration to another machine/environment
* No configuration drift

---

## 5. Jenkins Pipeline (Pipeline as Code)

### Style

* Declarative pipeline
* Uses a **custom shared library**
* No logic duplicated across Jenkinsfiles

### Pipeline Responsibilities

* Determine suite to run (smoke / regression)
* Apply branch-based execution rules
* Start and validate Selenium Grid
* Run tests in **parallel browsers**
* Collect, merge, and publish reports
* Notify stakeholders
* Update test management (Qase)

---

## 6. Jenkins Shared Library

Reusable pipeline logic is extracted into shared library functions:

**Versioning Strategy:**
- Production Jenkinsfile uses: `@Library('jenkins-pipeline-library@v1.3.2') _`
- Feature branches can test latest: `@Library('jenkins-pipeline-library@main') _`
- Shared library is versioned using semantic versioning (vMAJOR.MINOR.PATCH)
- Tags ensure deterministic builds and instant rollback capability

| Function                  | Responsibility                                  |
| ------------------------- | ----------------------------------------------- |
| `getBranchConfig()`       | Branch policies (main, feature, prod-candidate) |
| `determineTestSuite()`    | Decide smoke vs regression                      |
| `printBuildMetadata()`    | Log build metadata (suite, branch, trigger)     |
| `startDockerGrid()`       | Start + health-check Selenium Grid              |
| `stopDockerGrid()`        | Safe cleanup                                    |
| `checkTestFailures()`     | Quality gate - parse test results, enforce thresholds |
| `generateDashboard()`     | Consolidated HTML dashboard                     |
| `archiveAndPublishReports()` | Archive artifacts and publish HTML reports   |
| `sendBuildSummaryEmail()` | Email notifications                             |
| `updateQase()`            | Push results to Qase                            |
| `analyzeFailuresWithAi()` | AI-powered failure root cause analysis (branch-gated, advisory-only) |

AI analysis is advisory and non-blocking — it runs after the quality gate and does not affect pipeline success or failure.

**Why Shared Library**

* Enforces standards
* Reduces Jenkinsfile complexity
* Scales to multiple projects

---

## 7. Pre-Warmed Build Agents

### Purpose

Reduce build time and eliminate flaky dependency downloads. Pre-warming also removes network dependencies during test execution, ensuring CI stability is not affected by repository availability.

### Strategy

Two layered Docker images:

1. **Base agent**

   * Java 21
   * Maven
   * Docker CLI
   * Docker Compose
2. **Pre-warmed agent**

   * Project source code
   * All Maven dependencies pre-downloaded
   * Uses custom `settings.xml`

### Maven Caching Strategy

* `force-local-cache` Maven profile
* `updatePolicy=never`
* Snapshot downloads disabled

**Outcome**

* Deterministic builds
* Faster pipelines
* No surprise dependency changes

---

## 8. Selenium Grid Orchestration

### Execution Model

* Selenium Grid runs via Docker Compose
* Separate containers:

  * `selenium-hub`
  * `chrome` node
  * `firefox` node

### Design Highlights

* Unique Docker network per pipeline run
* Network lifecycle is deterministic: old containers and networks are torn down before each run to prevent stale connectivity issues
* Controlled max sessions per browser
* `/dev/shm` mounting to prevent browser crashes
* Health checks before test execution

**Result**

* Reliable parallel execution
* No cross-build interference
* Easy horizontal scaling (add nodes)

---

## 9. Test Execution Flow

1. Jenkins spins up pre-warmed agent
2. Selenium Grid is started and validated
3. Tests execute in parallel:

   * Chrome
   * Firefox

   Parallelism is at the Jenkins pipeline stage level — each browser runs in its own Docker container simultaneously.

4. Each browser:

   * Uses its own workspace
   * Generates isolated reports
5. Results are stashed and merged

---

## 10. Reporting & Dashboards

### Reporting Mechanisms

* **ExtentReports** (per browser)
* Consolidated **HTML dashboard**
* Jenkins archived artifacts

### Failure Aggregation

* Failures collected across threads
* Single failure summary file
* Used for dashboards and email alerts

---

## 11. Notifications & Test Management

### Email Notifications

* HTML summary
* Links to reports
* Branch-specific recipient lists

	* Feature branches → Development team
	* Protected branches (main / release) → Release stakeholders

### Qase Integration

* Suite-based configuration (`cicd/qase_config.json`)
* Automatic publishing of results
* Optional override via Jenkins parameter

---

## 12. Environment Strategy

| Aspect          | Strategy                      |
| --------------- | ----------------------------- |
| Configuration   | Environment variables         |
| Secrets         | Externalized (not in repo)    |
| Environments    | QA / Staging / Production     |
| Branch behavior | Controlled via shared library |
| Dependency updates | Dependabot (patch-only, weekly) — bot branches are implicitly excluded from pipeline via branch allowlist |

---

## 13. Design Principles & Benefits

* **Scalable:** Parallel browsers with isolated containers and networks
* **Reliable:** Thread-safe execution with deterministic dependency resolution
* **Maintainable:** Shared libraries and clear separation between pipeline orchestration and logic
* **Production-grade:** Configuration-driven and modular CI/CD design
* **Extensible:** Architecture supports evolution toward cloud-hosted agents or container orchestration without rewriting pipeline logic

---

## 14. Quality Gates

### Purpose

Automated validation of test results to prevent broken code from progressing through the pipeline.

### Implementation

* **Threshold-based enforcement**: Configurable via `QUALITY_GATE_THRESHOLD` Jenkins parameter
* **Dual-format XML parsing**: Supports both JUnit (surefire) and TestNG reports
* **Parallel result aggregation**: Combines Chrome + Firefox test results
* **Branch-specific policies**:
  * Feature branches → UNSTABLE (allows continued development)
  * Protected branches → FAILURE (prevents merges)

### Key Features

* Type-safe parameter conversion
* Handles "no tests found" scenarios
* Clear console logging with emojis for visibility
* Smart email notifications (only on status changes)

---

## 15. Future Evolution (Roadmap)

* Kubernetes-based Selenium Grid for large-scale parallel execution
* Jenkins agent autoscaling to optimize CI resource usage
* Quality gate enhancements (severity-aware thresholds, failure trend analysis)

