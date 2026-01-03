# Architecture Overview

## Framework Architecture

```
┌─────────────────────────────────────────┐
│              Test Layer                 │  (Test Classes, Data Providers)
├─────────────────────────────────────────┤
│         AI Augmentation Layer           │  (Data Gen, Failure Analysis, Scenarios)
├─────────────────────────────────────────┤
│            Domain Layer                 │  (Page Objects)
├─────────────────────────────────────────┤
│         Infrastructure Layer            │  (Framework Core)
│  ┌─────────────┴─────────────┐          │
│  │    Driver Management      │          │
│  │    Wait Strategies        │          │
│  │    Listeners              │          │
│  │    Retry Mechanism        │          │
│  └─────────────┬─────────────┘          │
└─────────────────────────────────────────┘
```

## Core Components

### 1. Driver Management

- **Location**: `src/main/java/com/demo/flightbooking/utils/DriverManager.java`, `src/main/java/com/demo/flightbooking/factory/BrowserOptionsFactory.java`
- **Responsibility**: Manages WebDriver lifecycle
- **Key Features**:
  - Thread-safe WebDriver instances via `ThreadLocal<WebDriver>`
  - Support for Chrome, Firefox, and Edge
  - Remote WebDriver support for Selenium Grid
  - Grid URL construction using `URI.create().toURL()` (modern Java)

### 2. Page Objects

- **Location**: `src/main/java/com/demo/flightbooking/pages/`
- **Pattern**: Page Object Model (POM) with composition
- **Structure**:
  - `BasePage`: Common page functionality, centralized timeout resolution via `resolveTimeout()`
  - `HomePage`: Flight search functionality
  - `FlightSelectionPage`: Flight listing and selection, stream-based price extraction
  - `PurchasePage`: Passenger form filling and purchase submission
  - `ConfirmationPage`: Booking confirmation verification
- **Design**: Page Objects delegate all element interactions to `WebDriverUtils` instances (composition over inheritance). No assertions in Page Objects — they return state.

### 3. Test Data Management

- **Location**: `src/test/java/com/demo/flightbooking/utils/`
- **Components**:
  - `CsvDataProvider`: CSV file parsing and data provision via TestNG `@DataProvider`
  - `JsonDataProvider`: JSON file parsing via Gson and data provision via TestNG `@DataProvider`
- **AI-assisted Data Generation**: `src/test/java/com/demo/flightbooking/ai/AiDataGenerator.java` — LLM-based passenger data generation with provider-agnostic design (Ollama/OpenAI)
- **Test Data Location**: `src/test/resources/testdata/`
- **Supported Formats**:
  - JSON files for structured test data (`passengers.json`)
  - CSV files for tabular test data (`passenger-data.csv`, `routes.csv`)
  - DataFaker library for dynamic, seeded test data generation (used in negative tests)
  - AI-generated CSV data via LangChain4j (optional, requires LLM)
- **Features**:
  - TestNG data provider integration
  - Environment-specific data support
  - Deterministic randomization with fixed seeds for reproducible negative tests

### 4. Test Execution & Lifecycle

- **Framework**: TestNG
- **Location**: `src/test/java/com/demo/flightbooking/tests/`
- **Structure**:
  - `tests/base/BaseTest.java`: Setup/teardown, MDC context, ExtentReports lifecycle, failure summary collection
  - `tests/booking/EndToEndBookingTest.java`: E2E booking flow (JSON and CSV data-driven)
  - `tests/booking/PurchaseFormValidationTest.java`: Negative testing with DataFaker
- **Features**:
  - Cross-browser execution coordinated via CI pipeline (parallelism at Jenkins stage level)
  - Data providers for data-driven testing
  - Custom listeners for test lifecycle management
  - Retry mechanisms with configurable retry counts
  - MDC-based structured logging for per-thread context (suite, browser, test)

### 5. Test Listeners & Reporting

- **Location**: `src/test/java/com/demo/flightbooking/listeners/`
- **Components**:
  - `TestListener`: Custom TestNG listener with `instanceof` pattern matching for retry decisions, screenshot capture on final failures only
  - `RetryAnalyzer`: Configurable retry logic with `ConcurrentHashMap` for thread safety, retryable/non-retryable exception classification, cause-chain traversal. Retries are limited to transient infrastructure issues (e.g., `TimeoutException`, `StaleElementReferenceException`); application-level assertion failures are never retried to avoid masking real defects.
- **Features**:
  - Screenshot capture on final failure (intermediate retries suppressed)
  - ExtentReports integration
  - Test execution tracking
  - **CI/CD Integration**: Jenkins quality gates evaluate TestNG/Surefire XML results for pass/fail decisions; HTML reports are for human-readable diagnostics.

### 6. Infrastructure Utilities

- **Location**: `src/main/java/com/demo/flightbooking/utils/`
- **Components**:
  - `ConfigReader`: Priority-based configuration (System property → config file → default)
  - `DriverManager`: WebDriver lifecycle and thread-safe management via `ThreadLocal`
  - `ExtentManager`: Thread-safe ExtentReports management via `ThreadLocal`
  - `MaskingUtil`: Data masking for sensitive fields (card numbers, passwords) in logs
  - `ScreenshotUtils`: Thread-safe screenshot capture with thread ID in filenames
  - `WebDriverUtils`: Centralized element interaction with explicit wait strategies

WebDriverUtils follows an instance-based interaction model.
Page Objects delegate all waits and interactions to WebDriverUtils,
ensuring centralized timeout control via `BasePage.resolveTimeout()` → `config.properties`.

### 7. AI Augmentation Layer

- **Location**: `src/test/java/com/demo/flightbooking/ai/`
- **Design**: Provider-agnostic (Ollama for local development, OpenAI for production quality). All AI features are decoupled from the core test framework — tests run with or without LLM access.
- **Capabilities**:

| Capability | Generator | Service | Runner |
|---|---|---|---|
| Test Data Generation | `AiDataGenerator` | `PassengerDataService` | `AiDataRunner` |
| Failure Root Cause Analysis | `AiFailureAnalyzer` | `FailureAnalysisService` | `FailureAnalysisRunner` |
| Test Scenario Generation | `AiScenarioGenerator` | `ScenarioGeneratorService` | `ScenarioGeneratorRunner` |
| Test Summary Generation | `AiTestSummaryGenerator` | `TestSummaryService` | `TestSummaryRunner` |

- **Pattern**: Each capability follows a Generator → Service → Runner architecture:
  - **Generator**: Orchestrates the workflow (config, model selection, output)
  - **Service**: Contains the LLM prompt engineering and response parsing
  - **Runner**: CLI entry point for standalone execution
- **Unit Tests**: `AiDataGeneratorTest`, `AiFailureAnalyzerTest` — mock-based, no LLM dependency required

## Design Patterns

### 1. Page Object Model (POM) with Composition

- Encapsulates page details
- Delegates interactions to `WebDriverUtils` (composition)
- Improves maintainability

### 2. Factory Pattern

- Creates browser-specific capabilities via `BrowserOptionsFactory`

### 3. Singleton Pattern

- `ConfigReader`: Static block initialization with priority-based property lookup
- `ExtentManager`: Thread-safe report management

### 4. Strategy Pattern

- `RetryAnalyzer`: Classifies exceptions as retryable (infrastructure) vs non-retryable (application)

## Best Practices

### 1. Test Structure

- One test class per page/feature
- Descriptive test method names
- Arrange-Act-Assert pattern

### 2. Error Handling

- Meaningful error messages
- Screenshot on final failure only (intermediate retry failures suppressed)
- Sensitive data masking in all log output

### 3. Logging

- Log4j2 with MDC context (suite, browser, test name per thread)
- Appropriate log levels
- Structured, thread-safe logging

## Additional Components

### Enums

- **Location**: `src/main/java/com/demo/flightbooking/enums/`
- **Components**:
  - `BrowserType`: Defines supported browsers (CHROME, FIREFOX, EDGE)
  - `EnvironmentType`: Defines environments (QA, STAGING, PRODUCTION)

### Model

- **Location**: `src/main/java/com/demo/flightbooking/model/`
- **Components**:
  - `Passenger`: Immutable data model using Java Record with masked `toString()` for log security

## Dependencies

### Core Dependencies

- Selenium WebDriver 4.27.0
- TestNG 7.10.2
- WebDriverManager 6.1.0
- ExtentReports 5.1.2
- Log4j2 2.24.1
- SLF4J 2.0.13
- Commons IO 2.19.0
- Gson 2.10.1
- DataFaker 2.3.1

### AI Dependencies

- LangChain4j 1.12.2 (core, Ollama, OpenAI)
