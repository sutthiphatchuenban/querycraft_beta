# QueryCraft Java Project Refactoring Plan

## Overview

This document provides a detailed plan for reorganizing the Java package structure to improve maintainability and separation of concerns.

---

## 1. File Mapping (Old → New Location)

### 1.1 service/ → connection/ (6 files)

| Old Location | New Location | Classification |
|--------------|--------------|----------------|
| `service/ConnectionManager.java` | `connection/ConnectionManager.java` | Interface |
| `service/ConnectionObserver.java` | `connection/ConnectionObserver.java` | Interface |
| `service/DatabaseConnectionService.java` | `connection/DatabaseConnectionService.java` | Service |
| `service/PooledConnectionManager.java` | `connection/PooledConnectionManager.java` | Implementation |
| `service/CsvConnectionManager.java` | `connection/CsvConnectionManager.java` | Implementation |

### 1.2 service/ → query/ (5 files)

| Old Location | New Location | Classification |
|--------------|--------------|----------------|
| `service/QueryExecutor.java` | `query/QueryExecutor.java` | Core Executor |
| `service/QueryExecutorService.java` | `query/QueryExecutorService.java` | Service |
| `service/PreparedStatementService.java` | `query/PreparedStatementService.java` | Service |
| `service/StreamingQueryService.java` | `query/StreamingQueryService.java` | Service |
| `service/TransactionService.java` | `query/TransactionService.java` | Service |

### 1.3 util/ → export/ (7 files)

| Old Location | New Location | Classification |
|--------------|--------------|----------------|
| `util/DataExporter.java` | `export/DataExporter.java` | Interface |
| `util/CsvExporter.java` | `export/CsvExporter.java` | Implementation |
| `util/CsvStreamingExporter.java` | `export/CsvStreamingExporter.java` | Implementation |
| `util/SqlStreamingExporter.java` | `export/SqlStreamingExporter.java` | Implementation |
| `util/CompositeStreamingExporter.java` | `export/CompositeStreamingExporter.java` | Implementation |
| `util/SqlInsertGenerator.java` | `export/SqlInsertGenerator.java` | Implementation |
| `util/ExporterFactory.java` | `export/ExporterFactory.java` | Factory |

### 1.4 ui/ → ui/controller/ (4 files)

| Old Location | New Location | Classification |
|--------------|--------------|----------------|
| `ui/MainController.java` | `ui/controller/MainController.java` | Main Controller |
| `ui/ConnectionStateController.java` | `ui/controller/ConnectionStateController.java` | Controller |
| `ui/QueryExecutionController.java` | `ui/controller/QueryExecutionController.java` | Controller |
| `ui/DialogManager.java` | `ui/controller/DialogManager.java` | Manager |

### 1.5 ui/ → ui/dialog/ (5 files)

| Old Location | New Location | Classification |
|--------------|--------------|----------------|
| `ui/ConnectionDialog.java` | `ui/dialog/ConnectionDialog.java` | Dialog |
| `ui/ExportDialog.java` | `ui/dialog/ExportDialog.java` | Dialog |
| `ui/ParameterDialog.java` | `ui/dialog/ParameterDialog.java` | Dialog |
| `ui/SettingsDialog.java` | `ui/dialog/SettingsDialog.java` | Dialog |
| `ui/HelpDialog.java` | `ui/dialog/HelpDialog.java` | Dialog |

### 1.6 util/ → util/ (5 files - pure utilities only)

| Current Location | Retained | Notes |
|------------------|----------|-------|
| `util/ResourceUtils.java` | ✓ | Pure utility |
| `util/ResultSetUtils.java` | ✓ | Pure utility |
| `util/ValidationUtils.java` | ✓ | Pure utility |
| `util/CsvValueFormatter.java` | ✓ | Utility (used by exporters) |
| `util/SqlValueFormatter.java` | ✓ | Utility (used by exporters) |

### 1.7 ui/ → ui/ (2 files - shared components)

| Current Location | Retained | Notes |
|------------------|----------|-------|
| `ui/SqlEditor.java` | ✓ | Shared component |
| `ui/ExportConfig.java` | ✓ | Data class used by dialogs |

### 1.8 Packages Unchanged

- `dialect/` - All dialect files remain
- `exception/` - Exception classes remain
- `model/` - Model classes remain
- `ui/component/` - UI components remain

---

## 2. Package Declaration Changes

### 2.1 connection/ Package (`querycraft.connection`)

Files changing from `package querycraft.service;` to `package querycraft.connection;`:

- `ConnectionManager.java`
- `ConnectionObserver.java`
- `DatabaseConnectionService.java`
- `PooledConnectionManager.java`
- `CsvConnectionManager.java`

### 2.2 query/ Package (`querycraft.query`)

Files changing from `package querycraft.service;` to `package querycraft.query;`:

- `QueryExecutor.java`
- `QueryExecutorService.java`
- `PreparedStatementService.java`
- `StreamingQueryService.java`
- `TransactionService.java`

### 2.3 export/ Package (`querycraft.export`)

Files changing from `package querycraft.util;` to `package querycraft.export;`:

- `DataExporter.java`
- `CsvExporter.java`
- `CsvStreamingExporter.java`
- `SqlStreamingExporter.java`
- `CompositeStreamingExporter.java`
- `SqlInsertGenerator.java`
- `ExporterFactory.java`

### 2.4 ui/controller/ Package (`querycraft.ui.controller`)

Files changing from `package querycraft.ui;` to `package querycraft.ui.controller;`:

- `MainController.java`
- `ConnectionStateController.java`
- `QueryExecutionController.java`
- `DialogManager.java`

### 2.5 ui/dialog/ Package (`querycraft.ui.dialog`)

Files changing from `package querycraft.ui;` to `package querycraft.ui.dialog;`:

- `ConnectionDialog.java`
- `ExportDialog.java`
- `ParameterDialog.java`
- `SettingsDialog.java`
- `HelpDialog.java`

---

## 3. Import Updates Required

### 3.1 Import Changes in connection/ Package

Files in `connection/` need these import updates:

```java
// Old imports (from service package)
import querycraft.service.ConnectionManager;
import querycraft.service.ConnectionObserver;
import querycraft.service.DatabaseConnectionService;
import querycraft.service.PooledConnectionManager;
import querycraft.service.CsvConnectionManager;

// New imports (to connection package)
import querycraft.connection.ConnectionManager;
import querycraft.connection.ConnectionObserver;
import querycraft.connection.DatabaseConnectionService;
import querycraft.connection.PooledConnectionManager;
import querycraft.connection.CsvConnectionManager;
```

### 3.2 Import Changes in query/ Package

Files in `query/` need these import updates:

```java
// Old imports
import querycraft.service.QueryExecutor;
import querycraft.service.QueryExecutorService;
import querycraft.service.PreparedStatementService;
import querycraft.service.StreamingQueryService;
import querycraft.service.TransactionService;

// New imports
import querycraft.query.QueryExecutor;
import querycraft.query.QueryExecutorService;
import querycraft.query.PreparedStatementService;
import querycraft.query.StreamingQueryService;
import querycraft.query.TransactionService;
```

### 3.3 Import Changes in export/ Package

Files in `export/` need these import updates:

```java
// Old imports
import querycraft.util.DataExporter;
import querycraft.util.CsvExporter;
import querycraft.util.ExporterFactory;
import querycraft.util.SqlInsertGenerator;

// New imports
import querycraft.export.DataExporter;
import querycraft.export.CsvExporter;
import querycraft.export.ExporterFactory;
import querycraft.export.SqlInsertGenerator;
```

### 3.4 Import Changes in ui/controller/ Package

Files in `ui/controller/` need these import updates:

```java
// Old imports
import querycraft.ui.MainController;
import querycraft.ui.ConnectionStateController;
import querycraft.ui.QueryExecutionController;
import querycraft.ui.DialogManager;
import querycraft.ui.ConnectionDialog;
import querycraft.ui.ExportDialog;
import querycraft.ui.ParameterDialog;
import querycraft.ui.SettingsDialog;
import querycraft.ui.HelpDialog;
import querycraft.ui.SqlEditor;
import querycraft.ui.ExportConfig;

// New imports
import querycraft.ui.controller.MainController;
import querycraft.ui.controller.ConnectionStateController;
import querycraft.ui.controller.QueryExecutionController;
import querycraft.ui.controller.DialogManager;
import querycraft.ui.dialog.ConnectionDialog;
import querycraft.ui.dialog.ExportDialog;
import querycraft.ui.dialog.ParameterDialog;
import querycraft.ui.dialog.SettingsDialog;
import querycraft.ui.dialog.HelpDialog;
import querycraft.ui.SqlEditor;
import querycraft.ui.ExportConfig;
```

### 3.5 Import Changes in ui/dialog/ Package

Files in `ui/dialog/` need these import updates:

```java
// Old imports from service
import querycraft.service.DatabaseConnectionService;
import querycraft.service.QueryExecutorService;
import querycraft.service.PreparedStatementService;

// New imports from connection and query
import querycraft.connection.DatabaseConnectionService;
import querycraft.query.QueryExecutorService;
import querycraft.query.PreparedStatementService;

// Old imports from ui
import querycraft.ui.DialogManager;
import querycraft.ui.ExportConfig;

// New imports
import querycraft.ui.controller.DialogManager;
import querycraft.ui.ExportConfig;
```

### 3.6 Import Changes in Files That Import Moved Classes

The following files need import updates for the classes they reference:

#### Files importing connection classes:
- `ui/ConnectionDialog.java` → `connection.DatabaseConnectionService`
- `ui/ConnectionStateController.java` → `connection.*`
- `ui/MainController.java` → `connection.*`
- `query/QueryExecutorService.java` → `connection.DatabaseConnectionService`
- `query/PreparedStatementService.java` → `connection.DatabaseConnectionService`
- `query/StreamingQueryService.java` → `connection.DatabaseConnectionService`
- `query/TransactionService.java` → `connection.DatabaseConnectionService`
- `connection/DatabaseConnectionService.java` → `connection.*` (internal refs)
- `connection/PooledConnectionManager.java` → `connection.ConnectionManager`
- `connection/CsvConnectionManager.java` → `connection.ConnectionManager`

#### Files importing query classes:
- `ui/ConnectionStateController.java` → `query.QueryExecutorService`
- `ui/MainController.java` → `query.*`
- `ui/QueryExecutionController.java` → `query.*`
- `ui/ParameterDialog.java` → `query.PreparedStatementService`
- `ui/SettingsDialog.java` → `query.QueryExecutorService`

#### Files importing export classes:
- `ui/dialog/ExportDialog.java` → `export.*`
- `ui/MainController.java` → `export.*`

#### Files importing ui.controller classes:
- `QueryCraftApp.java` → `ui.controller.MainController`
- `ui/controller/MainController.java` → `ui.controller.*`
- `ui/component/*` → `ui.controller.*` (if any references)

#### Files importing ui.dialog classes:
- `ui/controller/MainController.java` → `ui.dialog.*`
- `ui/controller/ConnectionStateController.java` → `ui.dialog.*`
- `ui/controller/QueryExecutionController.java` → `ui.dialog.*`
- `ui/controller/DialogManager.java` → `ui.dialog.*`

### 3.7 Test Files Import Updates

Test files need updates for imports:

```java
// Old imports in tests
import querycraft.service.QueryExecutor;
import querycraft.service.ConnectionManager;
import querycraft.service.DatabaseConnectionService;

// New imports
import querycraft.query.QueryExecutor;
import querycraft.connection.ConnectionManager;
import querycraft.connection.DatabaseConnectionService;
```

Affected test files:
- `QueryExecutorTest.java`
- `ConnectionManagerTest.java`
- `ConnectionStateControllerTest.java`
- `PreparedStatementServiceTest.java`
- `QueryExecutionControllerTest.java`
- `StreamingQueryServiceTest.java`
- `ExporterTest.java`

---

## 4. module-info.java Updates

The module descriptor must be updated to export the new packages:

```java
module querycraft {
    // ... existing requires ...

    // Open packages for reflection by JavaFX
    opens querycraft to javafx.graphics, javafx.fxml;
    opens querycraft.ui to javafx.fxml;
    opens querycraft.ui.component to javafx.fxml;
    opens querycraft.ui.controller to javafx.fxml;  // NEW
    opens querycraft.ui.dialog to javafx.fxml;      // NEW

    // Export packages for visibility
    exports querycraft;
    exports querycraft.model;
    exports querycraft.connection;                  // NEW (was service)
    exports querycraft.query;                       // NEW (was service)
    exports querycraft.ui;
    exports querycraft.ui.component;
    exports querycraft.ui.controller;               // NEW
    exports querycraft.ui.dialog;                   // NEW
    exports querycraft.export;                      // NEW (was util export subset)
    exports querycraft.util;
    exports querycraft.exception;
    exports querycraft.dialect;
}
```

---

## 5. Step-by-Step Safe Refactoring Approach

### Phase 1: Preparation (No code changes yet)

1. **Verify build system**
   - Ensure `mvn clean compile` passes
   - Run `mvn test` to establish baseline
   - Commit current state to git

2. **Create new directory structure**
   ```
   src/main/java/querycraft/
   ├── config/          (create empty)
   ├── connection/      (create)
   ├── dialect/         (exists)
   ├── exception/       (exists)
   ├── export/          (create)
   ├── model/           (exists)
   ├── query/           (create)
   ├── ui/
   │   ├── component/   (exists)
   │   ├── controller/  (create)
   │   └── dialog/      (create)
   └── util/            (exists)
   ```

### Phase 2: Move export/ Package (Low Risk)

The export package has minimal dependencies and is a good starting point.

1. **Create export/ directory and move files:**
   - Copy (don't move yet) all export-related files from `util/` to `export/`
   - Update package declarations in copied files to `querycraft.export`
   - Update imports within export/ files

2. **Update files that use exporters:**
   - `ui/MainController.java`
   - `ui/ExportDialog.java`

3. **Verify and switch:**
   - Run `mvn clean compile`
   - Run tests
   - Delete old util/ copies

### Phase 3: Move connection/ Package

1. **Identify connection-related files in service/:**
   - `ConnectionManager.java`
   - `ConnectionObserver.java`
   - `DatabaseConnectionService.java`
   - `PooledConnectionManager.java`
   - `CsvConnectionManager.java`

2. **Move files to connection/:**
   - Copy files to new location
   - Update package declarations
   - Update imports within connection/ files

3. **Update consumers:**
   - `ui/ConnectionDialog.java`
   - `ui/ConnectionStateController.java`
   - `ui/MainController.java`
   - All files in query/ that use DatabaseConnectionService
   - Test files

4. **Verify:**
   - Run `mvn clean compile`
   - Run tests

### Phase 4: Move query/ Package

1. **Identify query-related files in service/:**
   - `QueryExecutor.java`
   - `QueryExecutorService.java`
   - `PreparedStatementService.java`
   - `StreamingQueryService.java`
   - `TransactionService.java`

2. **Move files to query/:**
   - Copy files to new location
   - Update package declarations
   - Update imports (DatabaseConnectionService now from connection/)

3. **Update consumers:**
   - All UI controllers
   - Test files

4. **Verify:**
   - Run `mvn clean compile`
   - Run tests

### Phase 5: Move UI Subpackages

1. **Move controllers to ui/controller/:**
   - Copy controller files
   - Update package declarations
   - Update imports within controllers

2. **Move dialogs to ui/dialog/:**
   - Copy dialog files
   - Update package declarations
   - Update imports within dialogs

3. **Update all UI file references:**
   - `MainController` imports
   - `ConnectionStateController` imports
   - `QueryExecutionController` imports
   - `DialogManager` imports

4. **Verify:**
   - Run `mvn clean compile`
   - Run tests

### Phase 6: Update module-info.java

1. Add new exports:
   - `exports querycraft.connection;`
   - `exports querycraft.query;`
   - `exports querycraft.export;`
   - `exports querycraft.ui.controller;`
   - `exports querycraft.ui.dialog;`

2. Add opens for JavaFX:
   - `opens querycraft.ui.controller to javafx.fxml;`
   - `opens querycraft.ui.dialog to javafx.fxml;`

3. Remove old service export (keep temporarily for compatibility if needed)

### Phase 7: Cleanup and Final Verification

1. **Delete empty service/ directory**
2. **Remove old file copies from util/ (exporters)**
3. **Run full test suite:**
   ```bash
   mvn clean test
   ```
4. **Run application to verify UI works:**
   ```bash
   mvn javafx:run
   ```

---

## 6. Risks and Mitigation Strategies

### Risk 1: Circular Dependencies

**Risk:** Moving files may expose or create circular dependencies between new packages.

**Mitigation:**
- Analyze dependencies before moving files
- Use IDE dependency analysis tools
- If circular dependency found, consider:
  - Extracting common interfaces to a shared package
  - Using dependency inversion principles
  - Keeping related classes together

**Pre-check:**
```bash
# Use Maven dependency analyzer
mvn dependency:analyze
```

### Risk 2: JavaFX FXML Loading Failures

**Risk:** JavaFX FXML files may reference controllers by class name, causing runtime errors after package moves.

**Mitigation:**
- Search for FXML files that reference controllers:
  ```bash
  find src -name "*.fxml" -exec grep -l "fx:controller" {} \;
  ```
- Update `fx:controller` attributes to new package paths
- Ensure `opens` directives in `module-info.java` are correct

**Check:** Verify no FXML files exist or all controller references are updated.

### Risk 3: Reflection-Based Access

**Risk:** Code using reflection to access classes may fail after package moves.

**Mitigation:**
- Search for reflection usage:
  ```bash
  grep -r "Class.forName" src/
  grep -r "\.getClass\(\)" src/ | grep -v ".java:.*//"
  ```
- Update hardcoded package names in reflection calls
- Update `module-info.java` opens/exports

### Risk 4: Test Failures

**Risk:** Tests may fail due to import issues or package-private access changes.

**Mitigation:**
- Run tests after each phase
- Update test imports systematically
- Check for package-private method/field access that may be broken

### Risk 5: Version Control Conflicts

**Risk:** Large refactoring may cause merge conflicts with other developers.

**Mitigation:**
- Coordinate with team members
- Perform refactoring during low-activity period
- Use feature branch approach
- Make small, atomic commits

### Risk 6: Partial Refactoring State

**Risk:** System may be in broken state during multi-phase refactoring.

**Mitigation:**
- Use copy-then-verify-then-delete approach
- Each phase should leave system in working state
- Don't delete original files until new location is verified
- Use git commits between phases

### Risk 7: Missing Import Updates

**Risk:** Some import statements may be missed, causing compilation errors.

**Mitigation:**
- Use IDE automated refactoring tools (IntelliJ "Move" refactoring)
- Run `mvn clean compile` after each phase
- Use IDE's "Optimize Imports" feature
- Check for wildcard imports that may hide issues

### Risk 8: module-info.java Misconfiguration

**Risk:** Incorrect module exports/opens can cause runtime errors.

**Mitigation:**
- Test application startup after module-info changes
- Verify all packages that need reflection have `opens`
- Verify all packages used externally have `exports`

---

## 7. Rollback Plan

If issues occur during refactoring:

1. **Immediate rollback:**
   ```bash
   git reset --hard HEAD~N  # where N is number of refactoring commits
   ```

2. **Selective rollback:**
   - Revert specific file moves
   - Restore original imports
   - Keep new directory structure but delete contents

3. **Staged recovery:**
   - If only one phase fails, revert that phase
   - Keep successful phases
   - Retry failed phase with fixes

---

## 8. Post-Refactoring Checklist

- [ ] All source files compile without errors
- [ ] All tests pass
- [ ] Application starts and runs correctly
- [ ] No orphaned files in old locations
- [ ] module-info.java correctly exports/opens all packages
- [ ] No warnings about deprecated or missing exports
- [ ] IDE shows no import errors
- [ ] Documentation updated (if any references packages)
- [ ] Code review completed

---

## 9. Summary of Changes

| Package | Files | Lines Changed (est.) |
|---------|-------|---------------------|
| connection/ | 5 | ~25 package declarations + imports |
| query/ | 5 | ~25 package declarations + imports |
| export/ | 7 | ~35 package declarations + imports |
| ui/controller/ | 4 | ~20 package declarations + imports |
| ui/dialog/ | 5 | ~25 package declarations + imports |
| Files with import updates | ~15 | ~50-75 import statements |
| module-info.java | 1 | ~10 lines |
| **Total** | **~42 files** | **~150-200 lines** |

---

## 10. Visual Diagram

```
BEFORE:
┌─────────────────────────────────────────────────────────────────┐
│                         querycraft                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────────┐ │
│  │ dialect/ │  │exception/│  │  model/  │  │     util/       │ │
│  │          │  │          │  │          │  │  (mixed)        │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────────────┘ │
│  ┌──────────────────────────────────────┐  ┌─────────────────┐ │
│  │            service/ (14 files)       │  │   ui/ (12 files)│ │
│  │  - ConnectionManager                 │  │  (mixed)        │ │
│  │  - QueryExecutor                     │  └─────────────────┘ │
│  │  - DatabaseConnectionService         │                      │
│  │  - etc. (mixed concerns)             │                      │
│  └──────────────────────────────────────┘                      │
└─────────────────────────────────────────────────────────────────┘

AFTER:
┌─────────────────────────────────────────────────────────────────┐
│                         querycraft                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ dialect/ │  │exception/│  │  model/  │  │  config/ │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ connection/  │  │   query/     │  │   export/    │          │
│  │  (5 files)   │  │  (5 files)   │  │  (7 files)   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────┐  ┌──────────────────────────────────────────┐    │
│  │  util/   │  │                     ui/                  │    │
│  │ (5 pure) │  │  ┌───────────┬───────────┬────────────┐  │    │
│  └──────────┘  │  │ component/│controller/│  dialog/   │  │    │
│                │  │  (3 files)│ (4 files) │ (5 files)  │  │    │
│                │  └───────────┴───────────┴────────────┘  │    │
│                │  + SqlEditor.java, ExportConfig.java     │    │
│                └──────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

*Plan created for QueryCraft Java Project Refactoring*
