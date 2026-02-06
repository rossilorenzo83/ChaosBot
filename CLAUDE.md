# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build & Test
```bash
mvn clean verify              # Full build with all tests
mvn test                      # Unit tests only
mvn verify -DskipUnitTests    # Integration tests only
mvn test -Dtest=CoreMechanicsTest  # Single test class
mvn spring-boot:run           # Run application
mvn spotbugs:check            # Static analysis
mvn dependency-check:check    # Security analysis
mvn clean test jacoco:report  # Code coverage
```

### Test Categories
- Unit Tests: `**/*Test.java` (excluding `*IT.java`)
- Integration Tests: `**/*IT.java`, `**/*IntegrationTest.java`
- Tests run in non-headless mode (`-Djava.awt.headless=false`) for AWT Robot testing

### Run Application
```bash
java -jar target/chaos-conquest-bot-0.0.2-SNAPSHOT.jar \
  --general.windowsNames[0]=YourGameWindow \
  --general.actionType=RSS_FARMING
```

## Architecture Overview

### Core Components
- **ChaosBot.java**: Spring Boot entry point, creates workers and manages ExecutorService
- **CoreMechanics.java**: Business logic for game automation (computer vision, OCR, Discord)
- **WindowAutomationWorker.java**: Per-window worker with isolated Robot instance for thread-safe automation
- **Configuration**: `GeneralConfig`, `MarchConfig` for type-safe `@ConfigurationProperties` binding
- **Utilities**: `WinUtils` (Windows API via JNA), `ScreenUtils` (image processing)

### Technology Stack
- Spring Boot 4.0.2 with Java 21
- OpenCV 4.9.0 (image recognition/template matching)
- Tess4J 5.18.0 (OCR)
- JNA 5.18.1 (Windows native API)
- Spring WebFlux (Discord integration)

### Business Logic Flow
1. `WinUtils` detects game windows by process name
2. `ChaosBot` creates a `WindowAutomationWorker` per window
3. Each worker has its own `Robot` instance (prevents thread interference)
4. `ScreenUtils.takeScreenCapture(robot)` captures window screenshots
5. `CoreMechanics` uses OpenCV template matching to locate UI elements
6. Actions execute based on detected UI state
7. Fixed ExecutorService thread pool manages concurrent windows

### Action Types
- `RSS_FARMING` - Resource collection (IRON, STONE, FOOD, LEAD, WOOD, WARPSTONE)
- `ARMY_FARMING` - Automated army training (supports `farm.isSkelly` flag)
- `CHALLENGE_STATS` - Challenge data collection for Discord
- `DONORS_STATS` - Alliance donation tracking

### Configuration Properties
```properties
general.pidName=BlueStacks_nxt    # Process name
general.windowsNames[0]=GameWindow # Window titles
general.actionType=RSS_FARMING
general.actionIntervalMs=3000
farm.marchesAvailable=1
farm.targetArmyLevel=8
farm.targetRssLevel=ALL
discord.channelId=...
discord.botAuthToken=...
```

### Thread Safety Design
- Each `WindowAutomationWorker` owns its own `Robot` instance
- `CoreMechanics.mainMapButtonsCoordsMap` uses `ConcurrentHashMap`
- Double-checked locking in coordinate registration
- Graceful shutdown via volatile `shutdownRequested` flag + interrupt handling

### Special Cases
- **Warpstone/Relic**: Event resources require scrolling to find in resource list (`findScrollableRssIconWithScrolling()`)
- RSS type options: `ALL` (all types), `ALL_WO_RELIC` (excludes relic), `ALL_WO_EVENTS` (standard only)

## Development Notes

### Prerequisites
- JDK 21+
- Maven 3.6+
- Windows platform (JNA native API calls)

### Key Methods
- `findCoordsOnScreenFlexible()` - Primary image matching with quality thresholds
- `takeScreenCapture(Robot)` - All overloads require Robot parameter for thread safety
- `armyFarming()` - Supports `isSkelly` and `firstRun` optimization