# Flight Booking Pipeline Guide

## 1. Overview

This document outlines the policies, behaviors, and operational procedures for the Flight Booking test automation pipeline. It serves as the single source of truth for understanding and working with the CI/CD process effectively.

## 2. Core Concepts & Policies

### Branching Strategy

- **main**: Production branch. All code must pass review and regression tests before merging.
- **enhancements**: A pre-production branch for integrating major features.
- **feature/***: Standard branches for developing new features.

### Build Failure Conditions

- **FAILURE (Red)**: The build is marked as failed. This occurs when:
  - A critical infrastructure or script error occurs in any stage (build stops immediately).
  - Test failures exceed the quality gate threshold on a protected branch (main, enhancements), after all tests complete.
- **UNSTABLE (Yellow)**: The build completes all stages, but test failures exceed the configured quality gate threshold on feature branches. This allows development to continue while clearly surfacing test issues.

### Test Execution Strategy

- **smoke suite**: A small set of critical-path tests. Runs automatically on pushes to all branches.
- **regression suite**: A comprehensive suite of all tests. Intended to be run on protected branches before a release.
- **Parallel Execution**: Cross-browser execution is achieved via Jenkins parallel stages, where each browser runs in a separate Docker container connected to the same Selenium Grid.

### Pipeline Execution Flow

1. **Determine Suite** — Selects smoke or regression based on branch and parameters
2. **Initialize & Start Grid** — Creates isolated Docker network, starts Selenium Grid, validates health
3. **Approval Gate** — Optional manual approval for regression on protected branches
4. **Parallel Tests** — Chrome and Firefox execute simultaneously in separate containers
5. **Report Aggregation** — Results from both browsers are merged and published
6. **Quality Gate** — Evaluates aggregated test results against threshold
7. **AI Analysis** — Optional, non-blocking failure analysis (branch-gated)
8. **Notifications** — Email sent on status changes; Qase results published
9. **Cleanup** — Grid shutdown, container removal, workspace cleanup (guaranteed)

## 3. Pipeline Features

### Manual Triggers & Parameters

The following parameters are available when running a build manually in Jenkins:

| Parameter | Options | Description |
|-----------|---------|-------------|
| SUITE_NAME | smoke, regression | Selects the TestNG suite to execute. |
| TARGET_ENVIRONMENT | QA, STAGING, PRODUCTION | Specifies the test environment configuration to use. |
| MANUAL_APPROVAL | true, false | If true, pauses the pipeline for manual approval before running regression tests on protected branches. |
| QASE_TEST_CASE_IDS | String of IDs | (Optional) A comma-separated list of Qase Test Case IDs to update, overriding the defaults. |
| QUALITY_GATE_THRESHOLD | 0, 1, 2, 5 | Maximum number of test failures allowed before the quality gate fails. Default is 0 (zero tolerance). |
| FAIL_ON_NO_TESTS | true, false | If true, marks the build as UNSTABLE when no test results are found. Default is true. |

### Quality Gate Enforcement

The pipeline includes an automated quality gate that validates test results after every run:

- **Threshold-Based Enforcement**: Configurable via `QUALITY_GATE_THRESHOLD` parameter (0, 1, 2, or 5 failures)
- **Dual-Format Support**: Parses both JUnit (surefire) and TestNG XML reports accurately
- **Parallel Aggregation**: Combines results from Chrome + Firefox executions
- **Branch-Specific Policies**:
  - **Feature branches**: Exceeding threshold marks build as `UNSTABLE` (allows continued development)
  - **Protected branches** (main, enhancements): Exceeding threshold marks build as `FAILURE` (prevents bad merges)

**Quality Gate Output Example:**

```
Quality Gate: 4/39 failures (threshold: 0)
Quality Gate: Build marked UNSTABLE due to 4 failures
```

Retry logic handles transient test failures during execution, while the quality gate evaluates final aggregated results to enforce build stability policies.

### AI Failure Analysis (Optional)

After the quality gate runs, the pipeline optionally invokes AI-assisted failure root cause analysis using `analyzeFailuresWithAi()` from the shared library. This feature:

- **Branch-Gated**: Only runs on branches configured in `getBranchConfig().aiAnalysisBranches`
- **Advisory Only**: Never blocks the build — runs after the quality gate decision
- **LLM-Powered**: Uses LangChain4j with a configurable LLM provider (Ollama/OpenAI)

When disabled for a branch, the pipeline logs: `AI Failure Analysis: Disabled for branch '<branch-name>'`

### Notifications

- **Email**: Sent when build status changes (e.g., SUCCESS → UNSTABLE). Includes quality gate summary and test failure details.
- **Qase**: Test run results are automatically published to your Qase project.

### Guaranteed Cleanup

The pipeline ensures that all resources are properly shut down after every run, regardless of the build's outcome. This includes:

- Selenium Grid shutdown
- Docker container removal
- Jenkins workspace cleanup

## 4. Performance Optimizations

The pipeline includes several performance enhancements to balance speed and reliability:

### Pipeline Durability

**Conditional durability settings:**

- **Main branch:** `SURVIVABLE_NONATOMIC` - Balance of crash recovery and performance
- **Feature branches:** `PERFORMANCE_OPTIMIZED` - Maximum speed, minimal metadata overhead

**Trade-off:** Feature builds cannot resume after crash but execute 20-30% faster. Acceptable since re-runs are cheap for CI tests.

### Build Retention

**Automatic cleanup:**

- Keeps last 5 builds per branch
- Archives 0 artifacts (reports published to Jenkins UI only)
- HTML reports: `keepAll=false` (only latest retained)

**Result:** Build metadata stays under 1MB per build (down from 4-5MB), preventing UI slowdown.

### Other Optimizations

- **disableConcurrentBuilds**: Prevents resource conflicts with Selenium Grid
- **JVM heap sizing**: 3GB with G1GC for stable performance with ~400 plugins

## 5. The Build Environment

### Docker Agent & Caching

The pipeline executes inside a custom Docker container (`flight-booking-agent-prewarmed:latest`) to ensure a consistent and clean build environment. This image contains all necessary tools (Java, Maven, Docker CLI) and a pre-warmed cache of all Maven dependencies.

Dependency caching is enforced by a custom `settings.xml` file with an `<updatePolicy>never</updatePolicy>`, which forces Maven to use the local cache and prevents any downloads during the build.

### When to Rebuild the Pre-warmed Image

The pre-warmed Docker image must be rebuilt to keep the cache up to date. Rebuild the image after:

1. Adding, removing, or updating any dependencies in the `pom.xml`.
2. Changing system-level tools in the base Dockerfile.
3. Updating the base Maven/Java image version.

## 6. Local Development

### Running Tests Locally

```bash
# Run the default (smoke) test suite on default chrome browser
mvn clean test -Dselenium.grid.enabled=false

# Run the regression suite by activating its profile
mvn clean test -P regression -Dselenium.grid.enabled=false

# Run a specific TestNG group (e.g., 'smoke')
mvn clean test -Dgroups=smoke -Dselenium.grid.enabled=false

# Run with a specific browser
mvn clean test -Dbrowser=firefox -Dselenium.grid.enabled=false
```

## 7. Dependencies & Prerequisites

### Shared Library

The pipeline relies on a Jenkins Shared Library for reusable functions.

- **Name**: `jenkins-pipeline-library`

### Required Jenkins Credentials

The following credential IDs must be available in Jenkins for the pipeline to function correctly
(these are provisioned centrally via Jenkins Configuration as Code):

- `qase-api-token`: API token for publishing test results to Qase.
- `recipient-email-list`: Distribution list used for pipeline notifications.

## 8. Troubleshooting

### Common Issues

#### 1. Maven 'BUILD SUCCESS' vs. Jenkins 'UNSTABLE' Status

- **Symptom**: The Maven console output shows `BUILD SUCCESS`, but the Jenkins build is marked as `UNSTABLE`.
- **Explanation**: This is intended. Maven is configured with `<testFailureIgnore>true</testFailureIgnore>` to ensure it always finishes and generates reports. Jenkins then reads these reports and correctly marks the build `UNSTABLE` if it finds test failures.
- **Action**: No action needed. This indicates the pipeline worked, but tests failed.

#### 2. Dependency Resolution Issues

- **Symptom**: The build fails with a "dependency not found" error.
- **Explanation**: The `pom.xml` was likely updated with a new dependency, but the pre-warmed Docker image was not rebuilt.
- **Solution**: Rebuild the `flight-booking-agent-prewarmed:latest` image and re-run the build.

#### 3. Test Failures on Protected Branches

- **Symptom**: The build is marked as `FAILURE` when tests fail on `main` or `enhancements`.
- **Explanation**: This is by design to enforce quality on protected branches.
- **Action**: Fix the failing tests in feature branch before merging.

#### 4. Docker/Network Errors

- **Symptom**: The build fails with a connection error or a Docker daemon error.
- **Explanation**: This can occur due to stale Docker networks or containers from previous runs. The pipeline enforces deterministic network lifecycle (teardown → create → connect) via `startDockerGrid()` to prevent this, but manual intervention may be needed if the cleanup stage was interrupted.
- **Action**: Run `docker network prune` and `docker container prune` on the build agent, then re-run the pipeline.

#### 5. Grid Connection Issues

- **Symptom**: Tests fail to connect to Selenium Grid.
- **Check**: Verify Grid containers are running using `docker ps`.
- **Action**: Check Docker logs (`docker-compose logs`) and restart the Grid if needed.

### Monitoring

- **Test Results**: View ExtentReports in build artifacts.
- **Build History**: Check Jenkins build history.
- **Logs**: Access detailed execution logs in Jenkins.

## 9. Best Practices

1. **Keep the Pre-warmed Image Up to Date**: Always rebuild the image after `pom.xml` dependency changes.
2. **Run Tests Locally First**: Catch issues early before pushing code.
3. **Rebase Feature Branches Regularly**: Keep branches updated to avoid large merge conflicts.
4. **Monitor Test Flakiness**: Investigate and fix tests that fail intermittently.
5. **Use Meaningful Commit Messages**: Clearly explain the "what" and the "why" of changes.
6. **Document Significant Changes**: Update this guide when making major changes to the pipeline or framework.
