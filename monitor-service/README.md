# Monitor Service

## Overview
The Monitor Service is a component that simulates monitoring functionality with intentional error conditions for testing and demonstration purposes.

## Simulated Errors
The service includes an intentional error simulation mechanism:

- **IllegalStateException**: Thrown every 5 seconds by the MonitorService's background thread
- **Error ID**: 007553f8-4449-11f0-80e7-0242ac160004
- **Severity Score**: 95/100 (Intentionally high to simulate critical conditions)
- **Purpose**: This error is properly handled within the monitoring span and is used for:
  - Testing error handling mechanisms
  - Demonstrating monitoring capabilities
  - Simulating critical error conditions

## Important Note
These errors are **expected behavior** and should not trigger actual incident responses. The high severity score is part of the simulation design.