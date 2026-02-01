# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build & Test Commands
```bash
# Clean build with full test suite
mvn clean verify

# Package application for distribution
mvn clean package

# Run unit tests only
mvn test

# Run integration tests only  
mvn verify -DskipUnitTests

# Run specific test class
mvn test -Dtest=CoreMechanicsTest

# Spring Boot development run
mvn spring-boot:run

# Run with custom parameters
java -jar target/chaos-conquest-bot-0.0.2-SNAPSHOT.jar \
  --general.windowsNames[0]=YourGameWindow \
  --general.actionType=RSS_FARMING

# Static analysis
mvn spotbugs:check

# Security analysis
mvn dependency-check:check

# Code coverage report
mvn clean test jacoco:report
```

### Test Categories
- Unit Tests: `**/*UnitTest.java`, `**/*Test.java` (excluding IT)
- Integration Tests: `**/*IT.java`, `**/*IntegrationTest.java`

## Architecture Overview

### Core Components
- **ChaosBot.java**: Main Spring Boot application entry point and coordination logic
- **CoreMechanics.java**: Primary business logic for game automation (computer vision, OCR, Discord integration)
- **WindowAutomationWorker.java**: Dedicated worker class for single-window automation with isolated Robot instance (NEW)
- **Configuration Classes**: `GeneralConfig`, `MarchConfig` for type-safe application properties
- **Utility Classes**: `WinUtils` (Windows API integration), `ScreenUtils` (image processing)

### Technology Stack
- **Framework**: Spring Boot 3.5.5 with Java 21 (updated from 3.5.4)
- **Computer Vision**: OpenCV 4.9.0 for image recognition and template matching
- **OCR**: Tess4J 5.16.0 for text extraction from game UI
- **Windows Integration**: JNA for native Windows API calls
- **Discord Integration**: Spring WebFlux reactive WebClient
- **Testing**: JUnit 5, Mockito, TestContainers
- **Static Analysis**: SpotBugs 4.9.4.0, OWASP Dependency Check 12.1.3
- **Build Tools**: Maven 3.6+, JaCoCo 0.8.13 for code coverage

### Core Business Logic Flow
1. **Window Detection**: Uses WinUtils to find game windows by process name
2. **Worker Creation**: Creates dedicated WindowAutomationWorker per game window
3. **Robot Isolation**: Each worker gets its own Robot instance to prevent thread interference
4. **Screen Capture**: Takes screenshots of game windows for analysis (Robot passed as parameter)
5. **Template Matching**: Uses OpenCV to locate UI elements with configurable quality thresholds
6. **Action Execution**: Performs automated actions based on detected UI state
7. **Multi-threading**: Handles multiple game instances concurrently via fixed ExecutorService thread pool

### Key Design Patterns
- **Spring Configuration Properties**: Type-safe configuration binding with `@ConfigurationProperties`
- **Strategy Pattern**: Different action types (RSS_FARMING, ARMY_FARMING, CHALLENGE_STATS, DONORS_STATS)
- **Template Matching System**: Flexible image recognition with quality bounds
- **Multi-window Support**: Concurrent processing of multiple game instances
- **Worker Pattern**: Dedicated `WindowAutomationWorker` encapsulates per-window state and Robot instance
- **Double-Checked Locking**: Thread-safe ConcurrentHashMap initialization in coordinate registration
- **Graceful Shutdown**: Volatile flag + interrupt handling for clean worker termination

### Resource Types & Special Handling
- Standard resources: IRON, STONE, FOOD, LEAD, WOOD
- **Warpstone**: Special magical resource requiring custom scrolling logic in UI
  - Uses `findWarpstoneIconWithScrolling()` method in CoreMechanics:612
  - Scrolls up to 5 times to locate warpstone icon in resource list
  - Dedicated image resources: wp_icon.PNG, wp_source_map.PNG, wp_collect_map.PNG
- Random resource selection with "ALL" or "ALL_WO_WS" (exclude warpstone) options

### Configuration Structure
Application uses Spring Boot's configuration property binding:
- `general.*` properties for core app behavior
- `farm.*` properties for automation targeting (includes new `farm.isSkelly` boolean property)
- `discord.*` properties for Discord bot integration
- Multi-window support via `general.windowsNames[]` array
- **Army Farming Enhancements**: Support for Skelly army targeting with `farm.isSkelly` configuration

### Testing Strategy
- **Unit Tests**: Business logic with proper mocking (CoreMechanicsUnitTest)
- **Integration Tests**: Full Spring context testing
- **Multithread Tests** (NEW):
  - **WindowAutomationWorkerIT**: 7 tests validating worker lifecycle, Robot isolation, graceful shutdown
  - **CoreMechanicsThreadSafetyIT**: 7 tests validating concurrent map access, coordinate registration
  - Tests use CountDownLatch and ExecutorService to simulate concurrent scenarios
  - Validates no deadlocks, race conditions, or Robot interference
- **Specialized Tests**:
  - Warpstone feature testing (CoreMechanicsWarpstoneTest)
  - Configuration validation
  - Image path validation for new resources
- **GUI Mode**: Tests run in non-headless mode for UI component testing
- **Test Coverage**: 127 total tests (68 unit + 59 integration) - all passing

### Multithread Architecture (November 2025)
**Major Enhancement**: Refactored from shared Robot to per-worker isolation

#### Thread Safety Improvements
1. **Robot Instance Isolation**:
   - Each `WindowAutomationWorker` owns its own `Robot` instance
   - Prevents mouse/keyboard interference between concurrent windows
   - Fixed critical bug where shared Robot caused thread conflicts

2. **Screen Capture Enhancement**:
   - `ScreenUtils.takeScreenCapture()` now accepts `Robot` as parameter
   - Eliminates new Robot creation on every screenshot (performance + thread safety)
   - Updated 30+ call sites in CoreMechanics to pass Robot parameter

3. **Thread-Safe Coordinate Management**:
   - `CoreMechanics.mainMapButtonsCoordsMap` uses `ConcurrentHashMap`
   - Double-checked locking pattern in `WindowAutomationWorker.registerWindowCoordinates()`
   - Dedicated `coordsMapInitLock` prevents initialization race conditions

4. **Graceful Shutdown**:
   - `WindowAutomationWorker` supports graceful shutdown via `requestShutdown()` method
   - Dual exit conditions: volatile `shutdownRequested` flag + `Thread.isInterrupted()`
   - Proper cleanup in finally blocks with Robot instance release

#### Architecture Changes
- **Before**: Single mainLogic() method with shared Robot → thread interference
- **After**: WindowAutomationWorker pattern with isolated Robot → conflict-free parallel execution
- **ChaosBot.run()**: Now creates workers and submits to ExecutorService with proper awaitTermination
- **ExecutorService**: Fixed thread pool sized to window count for optimal resource usage

### Key Method Updates
- **findCoordsOnScreenFlexible()**: Enhanced image matching method used throughout CoreMechanics
- **armyFarming()**: Now supports `isSkelly` parameter and `firstRun` optimization logic
- **findWarpstoneIconWithScrolling()**: New method specifically for warpstone resource detection
- **computeGoIconForSpecificArmy()**: Added for precise army selection in search results
- **takeScreenCapture()**: All overloads now accept Robot parameter for thread safety

## Important Development Notes

### Prerequisites
- **JDK 21** or higher (project uses Java 21 language features)
- **Maven 3.6+** for dependency management and build lifecycle
- **Windows Platform**: Application is Windows-specific using JNA for native API calls

### Game Integration
- **Target Application**: Chaos Conquest game automation
- **Supported Platforms**: BlueStacks emulator, Steam client
- **Process Detection**: Uses Windows process names (`BlueStacks_nxt`, `Warhammer`)
- **Multi-Instance**: Supports concurrent automation of multiple game windows