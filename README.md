# ChaosBot

**Advanced automation tool for Chaos Conquest game** 🎮

ChaosBot leverages computer vision (OpenCV) and OCR (Tess4J) to automate repetitive tasks in Chaos Conquest, allowing players to optimize their gameplay efficiency.

## 📚 Documentation

- **[BUILD.md](BUILD.md)** - Build system, CI/CD workflows, and release process
- **[FEATURES.md](FEATURES.md)** - Complete software features and capabilities

## 🚀 Quick Start

### Prerequisites
- **JDK 21** or higher
- **Maven 3.6+** (for development)
- **Chaos Conquest** game running on BlueStacks or Steam

### Download & Run
```bash
# Download latest release from GitHub
# Run the JAR file
java -jar chaos-conquest-bot-0.0.2.jar --general.windowsNames[0]=YourGameWindow
```

### Development Build
```bash
# Clone repository
git clone https://github.com/rossilorenzo83/ChaosBot.git
cd ChaosBot

# Build project
mvn clean package

# Run with custom parameters
java -jar target/chaos-conquest-bot-0.0.2-SNAPSHOT.jar \
  --general.windowsNames[0]=YourGameWindow \
  --general.actionType=RSS_FARMING \
  --farm.marchesAvailable=3
```

## ⚙️ Command Line Configuration

### Essential Parameters
```bash
# Basic usage
java -jar chaos-conquest-bot.jar \
  --general.windowsNames[0]=YourGameWindow \
  --general.actionType=RSS_FARMING

# Advanced configuration
java -jar chaos-conquest-bot.jar \
  --general.windowsNames[0]=GameWindow1 \
  --general.windowsNames[1]=GameWindow2 \
  --general.actionType=ARMY_FARMING \
  --general.actionIntervalMs=3000 \
  --farm.marchesAvailable=5 \
  --farm.targetArmyLevel=8 \
  --farm.targetRssLevel=ALL
```

### Configuration Properties

#### Process & Window Detection
```bash
# Process name (default: BlueStacks_nxt)
--general.pidName=BlueStacks_nxt
--general.pidName=Warhammer

# Game window names (required)
--general.windowsNames[0]=Your Game Window
--general.windowsNames[1]=Second Account
```

#### Automation Settings
```bash
# Action type (default: RSS_FARMING)
--general.actionType=RSS_FARMING
--general.actionType=ARMY_FARMING
--general.actionType=CHALLENGE_STATS
--general.actionType=DONORS_STATS

# Timing (default: 3000ms)
--general.actionIntervalMs=3000

# Language (default: fr)
--general.gameLanguage=fr
--general.gameLanguage=en
```

#### March Management
```bash
# Available marches (default: 1)
--farm.marchesAvailable=3

# March interval in minutes (default: 15)
--farm.marchesIntervalMins=30

# Target levels
--farm.targetArmyLevel=8
--farm.targetRssLevel=ALL
--farm.rssType=IRON
```

#### Discord Integration
```bash
# Discord bot configuration
--discord.channelId=YOUR_CHANNEL_ID
--discord.botAuthToken=YOUR_BOT_TOKEN
--general.postDryRun=false
```

## 🎯 Supported Actions

### Resource Farming (RSS_FARMING)
```bash
# Farm all resource types
--general.actionType=RSS_FARMING --farm.targetRssLevel=ALL

# Farm specific resource
--general.actionType=RSS_FARMING --farm.rssType=IRON --farm.targetRssLevel=10
```

### Army Farming (ARMY_FARMING)
```bash
# Train armies at level 8
--general.actionType=ARMY_FARMING --farm.targetArmyLevel=8

# Use specific army preset
--general.actionType=ARMY_FARMING --farm.targetArmyLevel=8
```

### Challenge Statistics (CHALLENGE_STATS)
```bash
# Collect challenge data for Discord
--general.actionType=CHALLENGE_STATS
```

### Donor Statistics (DONORS_STATS)
```bash
# Track alliance donations
--general.actionType=DONORS_STATS
```

## 🔧 Development

### Build Commands
```bash
# Clean build
mvn clean compile

# Run tests
mvn test

# Package application
mvn clean package

# Run with Spring Boot
mvn spring-boot:run
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CoreMechanicsTest

# Run with debug logging
mvn test -Dspring.profiles.active=debug
```

## 🛠️ Troubleshooting

### Common Issues
1. **Window not found**: Verify window name matches exactly
2. **Process not detected**: Check `general.pidName` setting
3. **Image recognition fails**: Adjust `general.imageQualityLowerBound` (default: 0.7)

### Debug Mode
```bash
# Enable debug logging
java -jar chaos-conquest-bot.jar \
  --logging.level.com.lr=DEBUG \
  --general.windowsNames[0]=YourGameWindow
```

## 📦 Release Information

- **Current Version**: 0.0.2-SNAPSHOT
- **Java Version**: JDK 21
- **Spring Boot**: 3.2.2
- **Platform**: Windows (BlueStacks, Steam)

## 🔗 Links

- **[GitHub Repository](https://github.com/rossilorenzo83/ChaosBot)**
- **[Releases](https://github.com/rossilorenzo83/ChaosBot/releases)**
- **[Issues](https://github.com/rossilorenzo83/ChaosBot/issues)**

---

**For detailed information about features, build system, and development guidelines, see [FEATURES.md](FEATURES.md) and [BUILD.md](BUILD.md).**
