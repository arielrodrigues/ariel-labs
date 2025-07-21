# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Clojure smart mirror application that provides weather forecasts, time information across timezones, and Google Calendar integration. The application uses Pedestal for HTTP services and Stuart Sierra's Component library for dependency management.

## Development Commands

**Run tests:**
```bash
clojure -M:test
```

**Run tests for a specific namespace:**
```bash
clojure -M:test -n smart-mirror.logic-test
```

**Run a single test:**
```bash
clojure -M:test -v smart-mirror.logic-test/specific-test-name
```

**Start development environment with Flow Storm debugging:**
```bash
clojure -M:dev
```

**Start the server:**
```bash
clojure -M -m smart-mirror.server
```

## Architecture

### Component System
- Uses Stuart Sierra's Component library for dependency injection and lifecycle management
- System components are defined in `src/smart_mirror/system.clj` and `src/common/system.clj`
- Base system includes: `:routes`, `:http-server`, `:http-client`, `:config`
- Test system uses mock versions of HTTP server and client components

### Core Namespaces
- **smart-mirror.system**: System configuration and component wiring
- **smart-mirror.controller**: Business logic for weather, time, and calendar operations  
- **smart-mirror.http-in**: HTTP input adapters and route definitions
- **smart-mirror.http-out**: HTTP output adapters for external API calls
- **common.system**: Base system management utilities
- **common.routes**: Route configuration with dependency injection

### Testing Framework
- Uses custom `defflow` macro for integration tests with state-flow
- `defflow-quickcheck` macro combines state-flow with property-based testing  
- Integration test setup in `test/smart_mirror/integration/setup.clj`
- Test system automatically started/stopped for each test flow
- Regular unit tests use standard `clojure.test/deftest`
- Property-based tests use `test.check` with custom `defspec` wrapper

### Configuration
- Config managed through `config.edn` file (see `config.example.edn` for template)
- Config component implements protocols defined in `common.protocols.config`
- Requires Google Calendar API credentials (client ID, secret, refresh token)

### Data Flow
- HTTP requests → `smart-mirror.http-in` → `smart-mirror.controller` → `smart-mirror.http-out` → External APIs
- Coordinates extracted from IP geolocation for weather forecasting
- Google Calendar OAuth tokens refreshed automatically when expired

## Key Features
- Weather forecasting using IP-based location detection
- Multi-timezone time display 
- Google Calendar event integration with OAuth token refresh
- HTTP API with Pedestal interceptors

## External Dependencies
- Google Auth libraries for Calendar API access
- HTTP-kit for HTTP client operations
- Pedestal for web services
- Clojure.java-time for temporal operations