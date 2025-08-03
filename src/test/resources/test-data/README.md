# Test Data Directory

This directory contains test data and resources for ChaosBot unit and integration tests.

## Structure

```
test-data/
├── README.md                    # This file
├── sample-screenshots/          # Sample game screenshots for testing
├── mock-images/                 # Mock UI element images for template matching
└── test-configs/                # Test configuration files
```

## Usage

### Sample Screenshots
- Used for testing screen capture functionality
- Simulate real game screens for OCR testing
- Test image processing and template matching

### Mock Images
- UI element templates for button recognition
- Test image quality thresholds
- Validate coordinate calculations

### Test Configs
- Alternative configuration files for testing
- Edge case configurations
- Performance testing scenarios

## Test Scenarios

1. **Image Recognition Tests**
   - Template matching with various quality thresholds
   - Button detection on different screen resolutions
   - OCR text extraction from game screenshots

2. **Configuration Tests**
   - Property binding validation
   - Default value testing
   - Edge case configuration handling

3. **Integration Tests**
   - Full automation workflow simulation
   - Discord API integration testing
   - Multi-window scenario testing

## Adding Test Data

When adding new test data:

1. **Images**: Use PNG format for consistency
2. **Configs**: Follow the same structure as main config files
3. **Documentation**: Update this README with new test scenarios
4. **Naming**: Use descriptive names that indicate the test purpose

## Test Data Guidelines

- Keep test data minimal but representative
- Use realistic game scenarios
- Include edge cases and error conditions
- Maintain consistency with actual game UI elements
- Document any assumptions about game state 