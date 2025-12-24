# AI Augmentation Module

This package provides AI-assisted capabilities for the test framework using LangChain4j.
The module is intentionally kept flat to reduce navigation overhead and improve discoverability,
rather than introducing artificial package nesting for a small, well-defined set of classes.

## Design Principles

- **Centralized Model Factory** — All AI models are instantiated via `AiModelFactory`,
  enabling seamless switching between local (Ollama) and cloud (OpenAI) providers.
- **Consistent 3-Class Pattern** — Each AI capability follows a consistent structure:
  Generator/Analyzer (orchestrator) + Service (LangChain4j prompt interface) + Runner (CLI entry point).
- **Independent Modules** — Each AI capability is isolated and can evolve independently
  without impacting other components.
- **Advisory-Only Outputs** — All AI-generated reports (summaries, analysis, scenarios)
  are advisory. An LLM failure or timeout never blocks the CI/CD pipeline.
- **Fail-Fast Configuration** — Provider misconfiguration is detected early during model
  initialization to avoid runtime surprises.

---

## AI Capabilities & Execution Context

This framework categorizes Generative AI into two distinct contexts: **Deterministic CI/CD Telemetry** and **Local Shift-Left Utilities**.

| Capability | Execution Context | Description |
|---|---|---|
| **Failure Root Cause Analysis** | `CI/CD Pipeline` | Runs automatically post-build in Jenkins. Highly constrained prompts extract stack traces and summarize failures. Advisory and non-blocking. |
| **Test Scenario Generation** | `Local / CLI` | Used by QA engineers during sprint planning to generate edge-case scenarios from PRDs. Kept out of CI/CD to prevent non-deterministic pipeline behavior. |
| **Test Data Generation** | `Local / CLI` | Used locally to seed complex passenger profiles (JSON/CSV) before committing test data to version control. |
| **Executive Test Summary** | `Local / CLI` | Run on-demand by QA leads to generate human-readable sprint summaries for non-technical stakeholders. |

> **Architectural Note:** Generative AI tools that create tests or data are deliberately kept out of the
> automated Jenkins pipeline. CI/CD requires strict determinism; auto-generating test scope dynamically
> introduces unacceptable flakiness. These tools are designed to augment the human QA locally (Shift-Left),
> while only the Failure Analyzer is trusted in the pipeline because it evaluates data that already exists.

---

## Execution Flow (Simplified)

Test Execution → Reports Generated → AI Analysis (Failure / Summary) → Output Artifacts

---

## Module Architecture

### Shared Infrastructure

| Class | Role |
|---|---|
| `AiModelFactory` | Centralized factory for ChatModel instances. Handles provider switching (Ollama/OpenAI), temperature, and JSON schema configuration. |

---

### Test Data Generation

Generates edge-case and boundary passenger data via LLM with strict JSON Schema adherence.
Outputs to CSV for consumption by TestNG DataProviders.

| Class | Role |
|---|---|
| `AiDataGenerator` | Orchestrator — calls LLM, sanitizes output, writes CSV |
| `PassengerDataService` | LangChain4j service interface — defines prompt and response schema |
| `AiDataRunner` | CLI entry point (`mvn exec:java`) |
| `AiDataGeneratorTest` | Unit tests with mock model |

---

### Root Cause Analysis

Runs post-build to dynamically discover all `testng-results.xml` files across parallel browser
directories (e.g., `target/chrome/surefire-reports/`, `target/firefox/surefire-reports/`) using
`Files.walk()`, aggregates test counts and failure blocks, filters `automation.log` to relevant
entries, masks sensitive data, and generates a structured failure analysis report.

| Class | Role |
|---|---|
| `AiFailureAnalyzer` | Orchestrator — walks workspace for all Surefire XMLs, aggregates failures across browsers, filters logs, calls LLM, writes report |
| `FailureAnalysisService` | LangChain4j service interface — 7-section analysis prompt |
| `FailureAnalysisRunner` | CLI entry point (also invoked by Jenkins shared library via `mvn exec:java`) |
| `AiFailureAnalyzerTest` | Unit tests with mock model |

---

### Test Scenario Design

Translates feature descriptions or PRDs into structured test scenarios.
Three modes: comprehensive, negative-only, and regression suite.

| Class | Role |
|---|---|
| `AiScenarioGenerator` | Orchestrator — calls LLM per mode, validates output format, writes Markdown |
| `ScenarioGeneratorService` | LangChain4j service interface — mode-specific prompts with anti-hallucination constraints |
| `ScenarioGeneratorRunner` | CLI entry point with mode selection |

---

### Executive Reporting

Generates human-readable, executive-level summaries of test execution results.

| Class | Role |
|---|---|
| `AiTestSummaryGenerator` | Orchestrator — reads Surefire XML, calls LLM, writes summary report |
| `TestSummaryService` | LangChain4j service interface — classification and observation quality rules |
| `TestSummaryRunner` | CLI entry point |

---

## File-to-Capability Quick Reference

| File | Capability |
|---|---|
| `AiModelFactory` | Shared Infrastructure |
| `AiDataGenerator` | Data Generation |
| `PassengerDataService` | Data Generation |
| `AiDataRunner` | Data Generation |
| `AiDataGeneratorTest` | Data Generation |
| `AiFailureAnalyzer` | Root Cause Analysis |
| `FailureAnalysisService` | Root Cause Analysis |
| `FailureAnalysisRunner` | Root Cause Analysis |
| `AiFailureAnalyzerTest` | Root Cause Analysis |
| `AiScenarioGenerator` | Scenario Design |
| `ScenarioGeneratorService` | Scenario Design |
| `ScenarioGeneratorRunner` | Scenario Design |
| `AiTestSummaryGenerator` | Executive Reporting |
| `TestSummaryService` | Executive Reporting |
| `TestSummaryRunner` | Executive Reporting |
