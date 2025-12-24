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

Runs post-build to parse `testng-results.xml` and `automation.log`, masks sensitive data,
and generates a structured failure analysis report.

| Class | Role |
|---|---|
| `AiFailureAnalyzer` | Orchestrator — reads XML, extracts failures, filters logs, calls LLM, writes report |
| `FailureAnalysisService` | LangChain4j service interface — 7-section analysis prompt |
| `FailureAnalysisRunner` | CLI entry point (also invoked by Jenkins shared library) |
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
