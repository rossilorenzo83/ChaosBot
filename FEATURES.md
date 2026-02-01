# ChaosBot Software Features

## 🎮 **Overview**

ChaosBot is an advanced automation tool designed specifically for the **Chaos Conquest** mobile game. It leverages computer vision (OpenCV) and OCR (Tess4J) technologies to automate repetitive tasks, allowing players to optimize their gameplay efficiency.

## 🚀 **Core Automation Features**

### **1. Resource Farming (RSS_FARMING)**
- **Automated Resource Collection**: Automatically finds and farms resources on the game map
- **Multi-Resource Support**: 
  - 🪨 **Stone** - Building materials
  - 🥩 **Food** - Army sustenance  
  - 🪵 **Wood** - Construction resources
  - ⛓️ **Iron** - Military equipment
  - 🎯 **Lead** - Strategic materials
  - 💎 **Warpstone** - Advanced magical resources (requires custom scrolling logic)
- **Smart Level Targeting**: Configurable resource level selection (6-35)
- **Random Resource Selection**: Can cycle through different resource types automatically
- **Efficient Pathfinding**: Optimizes march routes to resource locations
- **Custom Scrolling Logic**: Special handling for warpstone resources that require scrolling to find in the resource list

### **1.1. Warpstone Gathering (Special Feature)**
- **Advanced Resource Type**: Warpstone is a special magical resource requiring unique handling
- **Custom Scrolling Implementation**: 
  - Automatically scrolls through resource list to locate warpstone icon
  - Smart scrolling with maximum 5 scroll attempts to prevent infinite loops
  - Real-time screen capture and image recognition during scrolling
- **Screen Limitation Handling**: Addresses UI limitations where warpstone is not immediately visible
- **Intelligent Error Recovery**: Graceful handling when warpstone cannot be found after maximum scrolls
- **Seamless Integration**: Works within existing RSS_FARMING framework without additional action types
- **Configuration Support**: 
  ```properties
  farm.rssType=WARPSTONE
  farm.targetRssLevel=ALL
  ```

### **2. Army Farming (ARMY_FARMING)**
- **Automated Army Training**: Trains armies automatically using available marches
- **Level-Based Targeting**: Configurable army level targeting (default: level 8)
- **Preset Management**: Supports multiple army presets (1-4)
- **Encampment Detection**: Automatically detects and utilizes encampments when available
- **Hero Integration**: Uses heroes strategically for low-quantity targets
- **Conflict Avoidance**: Handles army power conflicts intelligently

### **3. Challenge Statistics (CHALLENGE_STATS)**
- **Multi-Faction Support**: 
  - 🏰 **Alliance** challenges
  - ⚔️ **Horde** challenges  
  - 🛡️ **Legion** challenges
- **Historical Data Collection**: Gathers past challenge statistics
- **Discord Integration**: Automatically posts challenge data to Discord channels
- **Multi-Language Support**: French and English UI recognition

### **4. Donor Statistics (DONORS_STATS)**
- **Resource Donation Tracking**: Monitors received resources from alliance members
- **Real-time Reporting**: Tracks donation quantities and sources
- **Discord Notifications**: Posts donation statistics to Discord
- **Automated Data Collection**: Continuously monitors donation activities

## 🎯 **Advanced Capabilities**

### **Multi-Window Support** ⭐ NEW: Enhanced Thread Safety
- **Parallel Processing**: Handles multiple game instances simultaneously
- **Window Detection**: Automatically finds game windows by process name
- **BlueStacks Integration**: Optimized for BlueStacks emulator
- **Steam Compatibility**: Configurable for Steam client
- **Multi-Account Management**: Supports multiple accounts on same machine
- **Thread-Safe Architecture**:
  - Dedicated `WindowAutomationWorker` per game window
  - Isolated Robot instances to prevent mouse/keyboard interference
  - Thread-safe coordinate map initialization with double-checked locking
  - Graceful shutdown mechanism with interrupt handling
  - Concurrent execution via fixed thread pool sized to window count
- **Robot Isolation**: Each worker owns its own AWT Robot instance for conflict-free automation
- **Comprehensive Test Coverage**: 14 integration tests validating multithread functionality

### **Computer Vision & OCR**
- **Image Recognition**: Uses OpenCV for precise UI element detection
- **Text Recognition**: Tess4J OCR for reading game text and numbers
- **Multi-Language OCR**: French and English text recognition
- **Quality Thresholds**: Configurable image matching accuracy (default: 70%)
- **Dynamic UI Adaptation**: Handles UI variations and updates

### **Intelligent Automation**
- **Smart Timing**: Configurable intervals between actions (default: 3 seconds)
- **March Management**: Automatic march allocation and cooldown handling
- **Error Recovery**: Graceful handling of failed operations
- **Resource Optimization**: Efficient use of available marches
- **Conflict Resolution**: Handles game conflicts and errors

## ⚙️ **Configuration & Customization**

### **General Configuration**
```properties
# Process Detection
general.pidName = BlueStacks_nxt
general.windowsNames[0] = Game Window Name

# Automation Settings  
general.actionIntervalMs = 3000
general.actionType = RSS_FARMING
general.gameLanguage = fr
general.imageQualityLowerBound = 0.7

# Discord Integration
discord.channelId = YOUR_CHANNEL_ID
discord.botAuthToken = YOUR_BOT_TOKEN
```

### **March Configuration**
```properties
# March Management
farm.marchesAvailable = 1
farm.marchesIntervalMins = 15

# Targeting Settings
farm.targetArmyLevel = 8
farm.targetRssLevel = ALL
farm.rssType = IRON  # Supported: IRON, STONE, FOOD, LEAD, WOOD, WARPSTONE
```

### **Supported Action Types**
- `RSS_FARMING` - Resource collection automation
- `ARMY_FARMING` - Army training automation  
- `CHALLENGE_STATS` - Challenge data collection
- `DONORS_STATS` - Donation tracking

## 🔧 **Technical Features**

### **Spring Boot Integration**
- **Modern Framework**: Built on Spring Boot 3.2.2
- **Configuration Management**: Type-safe configuration properties
- **Dependency Injection**: Clean architecture with proper DI
- **Web Client**: Reactive web client for Discord integration
- **Async Processing**: Multi-threaded execution for parallel operations

### **Image Processing Pipeline**
- **Screen Capture**: Automatic game window screenshot capture
- **Template Matching**: Precise UI element location detection
- **Quality Assurance**: Configurable matching thresholds
- **Error Handling**: Robust exception handling for failed matches

### **OCR Capabilities**
- **Text Extraction**: Reads game text, numbers, and statistics
- **Language Support**: French and English OCR training data
- **Number Parsing**: Extracts quantities and levels from game UI
- **Data Validation**: Ensures extracted data accuracy

## 🌐 **Integration Features**

### **Discord Bot Integration**
- **Automated Reporting**: Posts game statistics to Discord channels
- **Real-time Updates**: Live data streaming to Discord
- **Rich Formatting**: Structured message formatting
- **Channel Management**: Configurable Discord channel targeting

### **Multi-Platform Support**
- **Windows Compatibility**: Optimized for Windows environments
- **Emulator Support**: BlueStacks, Steam, and other clients
- **Process Detection**: Automatic game process identification
- **Window Management**: Intelligent window handling

## 📊 **Monitoring & Analytics**

### **Real-time Statistics**
- **March Tracking**: Monitors march status and availability
- **Resource Monitoring**: Tracks collected resources
- **Performance Metrics**: Measures automation efficiency
- **Error Logging**: Comprehensive error tracking and reporting

### **Data Collection**
- **Challenge Statistics**: Historical challenge performance data
- **Donation Tracking**: Alliance donation patterns
- **Resource Analytics**: Resource collection efficiency metrics
- **Performance Optimization**: Data-driven automation improvements

## 🔒 **Security & Reliability**

### **Safe Automation**
- **Non-Invasive**: Uses only screen capture and mouse/keyboard simulation
- **Error Recovery**: Graceful handling of unexpected game states
- **Timeout Protection**: Prevents infinite loops and hangs
- **Resource Management**: Efficient memory and CPU usage

### **Configuration Security**
- **Environment Variables**: Secure credential management
- **Token Protection**: Secure Discord bot token handling
- **Process Isolation**: Safe multi-process execution
- **Error Boundaries**: Robust error handling and recovery

## 🚀 **Future Roadmap**

### **Planned Features**
- **FOE_FARMING** - Enemy player targeting automation
- **TRAIN_ARMY** - Advanced army training features
- **BUILD** - Building construction automation
- **Enhanced AI** - Machine learning for better decision making
- **Mobile Support** - Direct mobile device integration

### **Performance Enhancements**
- **Faster Processing** - Optimized image recognition algorithms
- **Better Accuracy** - Improved OCR and template matching
- **Reduced Resource Usage** - More efficient automation cycles
- **Enhanced Reliability** - Better error handling and recovery

---

**Last Updated**: August 2025  
**Version**: 0.0.2-SNAPSHOT  
**Compatibility**: Chaos Conquest Game  
**Supported Platforms**: Windows (BlueStacks, Steam) 