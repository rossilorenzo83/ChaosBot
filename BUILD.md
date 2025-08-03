# ChaosBot Build System Documentation

## Overview

ChaosBot is a Java-based automation tool for the Chaos Conquest game, built with Maven and Spring Boot. This document provides comprehensive information about the build system, CI/CD pipelines, and release process.

## Build System Architecture

### Technology Stack
- **Build Tool**: Apache Maven 3.x
- **Java Version**: JDK 21 (source/target compatibility)
- **Framework**: Spring Boot 3.2.2
- **CI/CD**: GitHub Actions
- **Release Management**: Maven Release Plugin

### Project Structure
```
ChaosBot/
├── pom.xml                    # Maven project configuration
├── release.properties         # Release plugin configuration
├── .github/workflows/         # CI/CD workflows
│   ├── maven.yml             # Build and test workflow
│   └── release.yml           # Release workflow
├── src/main/java/            # Java source code
├── src/main/resources/       # Application resources
└── target/                   # Build output directory
```

## Maven Configuration

### Core Dependencies
- **OpenCV**: 4.9.0-0 (Computer vision for image recognition)
- **Tess4J**: 5.8.0 (OCR for text recognition)
- **Spring Boot**: 3.2.2 (Application framework)
- **Lombok**: 1.18.30 (Code generation)

### Testing Dependencies
- **Spring Boot Test**: 3.2.2 (Testing framework)
- **JUnit Jupiter**: 5.x (Unit testing)
- **Mockito**: 5.x (Mocking framework)
- **TestContainers**: 1.x (Integration testing)
- **SpotBugs Annotations**: 4.8.3 (Static analysis)

### Build Plugins
1. **maven-compiler-plugin**: Java 21 compilation
2. **spring-boot-maven-plugin**: Executable JAR creation
3. **maven-release-plugin**: Release management
4. **maven-surefire-plugin**: Unit test execution
5. **maven-failsafe-plugin**: Integration test execution
6. **jacoco-maven-plugin**: Code coverage reporting
7. **spotbugs-maven-plugin**: Static code analysis

### Build Commands

#### Development Build
```bash
# Clean and compile
mvn clean compile

# Run unit tests only
mvn test

# Run integration tests only
mvn verify -DskipUnitTests

# Run all tests (unit + integration)
mvn clean verify

# Package without tests
mvn package -DskipTests

# Full build with comprehensive testing
mvn clean verify
```

#### Testing Commands
```bash
# Run specific test class
mvn test -Dtest=ChaosBotTest

# Run tests with debug logging
mvn test -Dspring.profiles.active=test

# Run tests with coverage report
mvn clean test jacoco:report

# Run static analysis
mvn spotbugs:check

# Run dependency analysis
mvn dependency:analyze
```

#### Production Build
```bash
# Create executable JAR
mvn clean package

# Build with Spring Boot repackaging
mvn spring-boot:repackage
```

#### Dependency Management
```bash
# Update dependencies
mvn versions:use-latest-versions

# Check for dependency updates
mvn versions:display-dependency-updates

# Analyze dependencies
mvn dependency:analyze
```

## CI/CD Workflows

### Build Workflow (`.github/workflows/maven.yml`)

**Triggers**: Push to any branch, Pull requests to any branch
**Environment**: Ubuntu latest, JDK 21

**Jobs**:
1. **Build and Test**
   - Checkout source code with full history
   - Setup JDK 21 (Temurin distribution)
   - Cache Maven dependencies
   - Validate POM
   - Compile source code
   - Run unit tests
   - Run integration tests
   - Build package
   - Test Spring Boot repackaging
   - Upload test results and build artifacts

2. **Code Quality Analysis**
   - Static analysis with SpotBugs
   - Dependency analysis
   - Check for dependency updates

3. **Security Scan**
   - OWASP dependency check
   - Update dependency graph for security alerts

### Release Workflow (`.github/workflows/release.yml`)

**Triggers**: Manual workflow dispatch
**Environment**: Ubuntu latest, JDK 21

**Inputs**:
- `branch`: Source branch (default: master)
- `releaseVersion`: Version to release (default: X.Y.Z)
- `developmentVersion`: Next dev version (default: X.Y.Z-SNAPSHOT)
- `dryRun`: Test run flag (default: false)

**Steps**:
1. Checkout specified branch
2. Setup JDK 17
3. Configure Git user
4. Build with tests: `mvn -B clean verify`
5. Prepare release: `mvn release:prepare`
6. Perform release: `mvn release:perform`
7. Create GitHub release
8. Upload release assets

## Release Process

### Version Management
- **Current Version**: 0.0.2-SNAPSHOT
- **Version Format**: Semantic versioning (MAJOR.MINOR.PATCH)
- **SNAPSHOT Suffix**: Development versions

### Release Steps

#### 1. Pre-release Preparation
```bash
# Ensure clean working directory
git status

# Update version in pom.xml if needed
mvn versions:set -DnewVersion=X.Y.Z

# Run full test suite
mvn clean verify
```

#### 2. Manual Release via GitHub Actions
1. Navigate to Actions tab in GitHub
2. Select "Maven Release & GitHub Deployment"
3. Click "Run workflow"
4. Fill in release parameters:
   - Branch: main
   - Release Version: X.Y.Z
   - Development Version: X.Y.Z-SNAPSHOT
   - Dry Run: false

#### 3. Automated Release Process
The workflow automatically:
- Creates release branch
- Updates version numbers
- Runs tests
- Creates Git tags
- Builds release artifacts
- Creates GitHub release
- Uploads JAR file

### Release Artifacts
- **Executable JAR**: `chaos-conquest-bot-X.Y.Z.jar`
- **Source Distribution**: Source code archive
- **Documentation**: README and configuration files

## Local Development Setup

### Prerequisites
- JDK 21 or higher
- Maven 3.6+
- Git

### Environment Setup
```bash
# Clone repository
git clone https://github.com/rossilorenzo83/ChaosBot.git
cd ChaosBot

# Verify Java version
java -version

# Verify Maven installation
mvn -version
```

### Development Workflow
```bash
# 1. Sync with latest changes
git pull origin main

# 2. Build project
mvn clean compile

# 3. Run comprehensive test suite
mvn clean verify

# 4. Run specific test types
mvn test                    # Unit tests only
mvn verify -DskipUnitTests  # Integration tests only

# 5. Create executable JAR
mvn package

# 6. Run application
java -jar target/chaos-conquest-bot-0.0.2-SNAPSHOT.jar
```

### Test-Driven Development Workflow
```bash
# 1. Write failing test first (TDD)
# 2. Run test to confirm it fails
mvn test -Dtest=NewFeatureTest

# 3. Implement minimal code to pass test
# 4. Run test to confirm it passes
mvn test -Dtest=NewFeatureTest

# 5. Refactor while keeping tests green
# 6. Run full test suite
mvn clean verify
```

### Comprehensive Test Coverage

The project now includes comprehensive test coverage with the following test types:

#### **Unit Tests**
- **CoreMechanicsUnitTest**: Business logic with proper mocking
- **UtilsUnitTest**: Utility functions and edge cases
- **Configuration Tests**: Property binding and validation

#### **Integration Tests**
- **ChaosBotIntegrationTest**: Basic Spring Boot integration
- **ChaosBotFullIntegrationTest**: Complete application context
- **ConfigurationIntegrationTest**: Configuration bean loading

#### **Test Coverage Areas**
- ✅ **Business Logic**: Core automation methods and enums
- ✅ **Configuration**: Property binding and validation
- ✅ **Utilities**: Screen capture, window management, OCR
- ✅ **Spring Boot**: Application context and dependency injection
- ✅ **Discord Integration**: WebClient configuration
- ✅ **Image Processing**: Template matching and quality thresholds
- ✅ **Error Handling**: Exception scenarios and edge cases

#### **Test Data**
- **Test Resources**: Sample screenshots and mock images
- **Test Configurations**: Alternative property files
- **Test Documentation**: Comprehensive test data guidelines

## Build Configuration Files

### pom.xml Key Sections

#### Project Metadata
```xml
<groupId>com.lr</groupId>
<artifactId>chaos-conquest-bot</artifactId>
<version>0.0.2-SNAPSHOT</version>
```

#### Parent POM
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.4</version>
</parent>
```

#### Build Configuration
```xml
<build>
    <plugins>
        <!-- Java 21 compilation -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>21</source>
                <target>21</target>
            </configuration>
        </plugin>
        
        <!-- Spring Boot executable JAR -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        
        <!-- Release management -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-release-plugin</artifactId>
            <version>3.1.1</version>
        </plugin>
    </plugins>
</build>
```

### release.properties Configuration
- **SCM URL**: Git repository configuration
- **Tag Format**: `@{project.artifactId}-@{project.version}`
- **Release Strategy**: Default Maven release strategy
- **Push Changes**: Enabled for automated releases

## Troubleshooting

### Common Build Issues

#### 1. Java Version Mismatch
**Error**: "Source option 21 is no longer supported"
**Solution**: Update to JDK 21 or higher

#### 2. Maven Dependencies
**Error**: "Could not resolve dependencies"
**Solution**: 
```bash
mvn clean
mvn dependency:resolve
```

#### 3. Spring Boot Plugin Issues
**Error**: "No main manifest attribute"
**Solution**: Ensure spring-boot-maven-plugin is configured correctly

#### 4. Release Plugin Failures
**Error**: "SCM connection failed"
**Solution**: Verify Git credentials and repository access

### Performance Optimization

#### Maven Build Optimization
```bash
# Parallel build
mvn -T 1C clean package

# Skip tests for faster builds
mvn package -DskipTests

# Use local repository cache
mvn -o package
```

#### CI/CD Optimization
- Dependency caching in GitHub Actions
- Parallel job execution
- Conditional step execution

## Security Considerations

### Dependency Security
- Automated dependency scanning via Dependabot
- Regular security updates
- Vulnerability assessment in CI/CD

### Build Security
- No secrets in build artifacts
- Secure dependency resolution
- Signed releases (future enhancement)

## Future Enhancements

### Planned Improvements
1. **Multi-platform builds**: Windows, macOS, Linux
2. **Containerization**: Docker image builds
3. **Automated testing**: Integration test suite
4. **Code quality**: SonarQube integration
5. **Documentation**: Automated API documentation

### Build System Evolution
- Migration to Gradle (if needed)
- Microservices architecture support
- Cloud-native deployment options

## Support and Maintenance

### Build System Maintenance
- Regular dependency updates
- Maven plugin version updates
- CI/CD workflow improvements

### Documentation Updates
- Keep BUILD.md current with changes
- Update README.md with build instructions
- Maintain release notes

---

**Last Updated**: December 2024
**Build System Version**: Maven 3.x + Spring Boot 3.2.2
**Java Version**: JDK 21 (source/target), JDK 21 (CI/CD) 