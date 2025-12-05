# Metrics Analysis Plan: Bug Fix Task Before vs. After Refactoring

## Research Questions

**RQ1:** How does refactoring affect design quality in object-oriented systems in terms of coupling, cohesion, and complexity?

**RQ2:** To what extent does refactoring influence the effort required to perform maintenance tasks in a software project?

## Analysis Scope

### Task: Bug Fix - Campaign Game Over State Management

**Bug Description:** After losing the player's core in a campaign sector, players could still unpause, use freecam, and control units despite being in game over state.

**Fix Description:** Added `afterGameOver` flag and implemented checks across multiple layers to prevent unpausing and disable actions after game over in campaign mode.

---

## Commits to Analyze

### Initial State (Same for Both Teams)
**Base Commit:** `7d6a547680737678308252b76af2c8ab2941c96c`
- Original codebase structure (non-refactored)
- **This is the starting point for both teams**
- Both teams begin from this same initial state

### Team A: Fix on Non-Refactored Code
**Base Commit:** `7d6a547680737678308252b76af2c8ab2941c96c` (same initial state)
- **Team A's Fix Commit:** [To be provided]
- Team A applied bug fix directly to the original (non-refactored) structure
- This represents the "before refactoring" effort

### Team B: Fix on Refactored Code
**Base Commit:** `d25e64aef1f86cca31249503dcd4944130c42de4`
- Refactored codebase structure (refactoring happened between initial state and this commit)
- **Team B's Fix Commit:** Current implementation
- Team B (this team) applied bug fix to the refactored structure
- This represents the "after refactoring" effort

**Timeline:**
1. Initial state: `7d6a547680737678308252b76af2c8ab2941c96c` (non-refactored)
2. Refactoring: `d25e64aef1f86cca31249503dcd4944130c42de4` (refactored, no bug fix)
3. Team A fixes bug: [Commit to be provided] (on initial state)
4. Team B fixes bug: Current implementation (on refactored state)

---

## Files to Analyze

### Initial State (Before Refactoring) - 5 files

**Directly Modified Files:**
1. **core/src/mindustry/core/Control.java**
   - Lines 692-698: Pause prevention logic
   - Main control loop with pause/unpause logic embedded
   - Contains all pause state management
   - ~1200+ lines (estimated)

2. **core/src/mindustry/core/GameState.java**
   - Line 27: `afterGameOver` field addition
   - Central state management
   - ~100 lines

3. **core/src/mindustry/ui/dialogs/GameOverDialog.java**
   - Line 42: Set `afterGameOver` flag
   - ~200 lines

4. **core/src/mindustry/ui/dialogs/PausedDialog.java**
   - Line 61: Disable abandon button
   - ~300 lines

5. **core/src/mindustry/ui/fragments/HudFragment.java**
   - Lines 937-939: Hide status text
   - Contains all status display logic embedded
   - ~960+ lines (estimated)

### After Refactoring - 7 files (2 new classes, 2 refactored)

**Directly Modified Files:**
1. **core/src/mindustry/core/GameState.java**
   - Line 27: `afterGameOver` field addition
   - ~120 lines
   - **Unchanged structure from initial**

2. **core/src/mindustry/core/GameStateManager.java** ⭐ NEW CLASS
   - Lines 23, 57-60: Pause prevention logic
   - Extracted from Control.java
   - Handles all pause-related state management
   - ~98 lines

3. **core/src/mindustry/core/Control.java** ⚠️ REFACTORED
   - Delegates pause logic to GameStateManager
   - Still part of the system (delegation pattern)
   - ~405 lines (reduced from ~1200+)

4. **core/src/mindustry/ui/dialogs/GameOverDialog.java**
   - Line 44: Set `afterGameOver` flag
   - ~170 lines
   - **Unchanged structure from initial**

5. **core/src/mindustry/ui/dialogs/PausedDialog.java**
   - Line 60: Disable abandon button
   - ~178 lines
   - **Unchanged structure from initial**

6. **core/src/mindustry/ui/fragments/StatusDisplayBuilder.java** ⭐ NEW CLASS
   - Lines 137-140: Hide status text
   - Extracted from HudFragment.java
   - Handles all status display building
   - ~418 lines

7. **core/src/mindustry/ui/fragments/HudFragment.java** ⚠️ REFACTORED
   - Delegates status display to StatusDisplayBuilder
   - Still part of the system (delegation pattern)
   - ~126 lines (reduced from ~960+)

**Note:** Control.java and HudFragment.java are included because they're part of the system architecture, even though they now delegate to extracted classes. This gives a complete picture of the system structure.

---

## Metrics to Calculate

### RQ1: Design Quality Metrics (Focus: Coupling, Cohesion, Complexity)

#### Core Metrics
1. **CBO (Coupling Between Objects)** ⭐ PRIMARY
   - Number of classes coupled to
   - **Directly measures coupling** - key for RQ1
   - Files: All classes in both states

2. **LCOM (Lack of Cohesion of Methods)** ⭐ PRIMARY
   - Measures cohesion within a class
   - **Directly measures cohesion** - key for RQ1
   - Files: All classes in both states

3. **Cyclomatic Complexity** ⭐ PRIMARY
   - Method-level complexity
   - **Directly measures complexity** - key for RQ1
   - Focus on modified methods:
     - Initial: `Control.update()`, `HudFragment.makeStatusTable()`
     - After: `GameStateManager.updatePauseState()`, `StatusDisplayBuilder.createStatusLabel()`
   - Also calculate average complexity per class

#### Supporting Metrics
4. **LOC (Lines of Code)**
   - Total lines per file
   - **Context for understanding size changes**
   - Files: All classes in both states

5. **WMC (Weighted Methods per Class)**
   - Count of methods weighted by complexity
   - **Indicates class complexity**
   - Files: All classes in both states

---

### RQ2: Maintenance Effort Metrics (Focus: Time, Files, LOC)

**Note:** RQ2 compares effort between Team A (fixing on non-refactored code) and Team B (fixing on refactored code).

#### Primary Effort Measures
1. **Time Spent** ⭐ PRIMARY
   - **Team A (Initial/Non-refactored):** Actual time from Team A's fix commit
   - **Team B (After refactoring):** Actual time spent (30 min + 45 min + 60 min = 135 min)
   - **Compare:** Time difference between teams
   - **Directly measures effort** - key for RQ2

2. **Number of Files Modified** ⭐ PRIMARY
   - **Team A (Initial):** Count from Team A's fix commit
   - **Team B (After refactoring):** 7 files (5 directly modified + 2 refactored classes that delegate)
   - **Compare:** File count difference
   - **Directly measures effort** - key for RQ2

3. **Lines of Code Changed (LOC)** ⭐ PRIMARY
   - **Team A (Initial):** Calculate from Team A's fix commit diff
     - Total lines added
     - Total lines removed
     - Net change (added - removed)
   - **Team B (After refactoring):** Calculate from Team B's fix commit diff
     - Total lines added
     - Total lines removed
     - Net change (added - removed)
   - **Compare:** LOC change difference between teams
   - **Directly measures effort** - key for RQ2

#### Supporting Measures
4. **Code Churn**
   - **Team A:** Total additions and deletions across all files
   - **Team B:** Total additions and deletions across all files
   - **Compare:** Churn difference
   - **Context for understanding change magnitude**

---

## Analysis Steps

### Step 1: Extract Code from Commits
- [ ] Checkout initial state (same for both teams): `7d6a547680737678308252b76af2c8ab2941c96c`
- [ ] Extract relevant files from initial state (5 files: Control, GameState, GameOverDialog, PausedDialog, HudFragment)
- [ ] **Team A:**
  - Checkout Team A's fix commit: [To be provided]
  - Extract relevant files AFTER Team A's fix (5 files with bug fix applied on non-refactored code)
- [ ] **Team B:**
  - Checkout refactored base commit: `d25e64aef1f86cca31249503dcd4944130c42de4`
  - Extract relevant files BEFORE fix (7 files: Control, GameState, GameStateManager, GameOverDialog, PausedDialog, StatusDisplayBuilder, HudFragment)
  - Extract relevant files AFTER Team B's fix (5 files: Control, GameState, GameOverDialog, PausedDialog, HudFragment)

### Step 2: Extract Both Teams' Fixes
- [ ] **Team A (Non-refactored):**
  - Use Team A's fix commit to get the actual implementation
  - Document what changes Team A made
  - Extract files with Team A's fix applied
  - Note: Fix applied to initial state (non-refactored)
- [ ] **Team B (Refactored):**
  - Use Team B's fix commit (current implementation)
  - Document what changes Team B made
  - Extract files with Team B's fix applied
  - Note: Fix applied to refactored state

### Step 3: Calculate Quality Metrics (RQ1)
**Note:** RQ1 compares Initial State vs. Refactored Before Fix (impact of refactoring only, not bug fix)
- [ ] Use tool (e.g., Understand, SonarQube, CKJM, or custom script)
- [ ] **Initial State (5 files):**
  - Calculate CBO (coupling) for all classes
  - Calculate LCOM (cohesion) for all classes
  - Calculate Cyclomatic Complexity for methods and average per class
  - Calculate LOC for all files
  - Calculate WMC for all classes
- [ ] **Refactored Before Fix (7 files):**
  - Calculate CBO (coupling) for all classes
  - Calculate LCOM (cohesion) for all classes
  - Calculate Cyclomatic Complexity for methods and average per class
  - Calculate LOC for all files
  - Calculate WMC for all classes
- [ ] **Compare:** Calculate differences and percentage changes

### Step 4: Calculate Effort Metrics (RQ2)
- [ ] **Team A (Initial/Non-refactored):**
  - Count files modified from Team A's fix commit
  - Calculate LOC changed using git diff (additions, deletions, net change)
  - Document time spent (from Team A's task logs or commit message)
  - Calculate code churn (total additions, total deletions)
- [ ] **Team B (After refactoring):**
  - Count files modified (7 files)
  - Calculate LOC changed using git diff (additions, deletions, net change)
  - Document time spent 
  - Calculate code churn (total additions, total deletions)
- [ ] **Compare:** Calculate differences between Team A and Team B metrics

### Step 5: Comparative Analysis
- [ ] **RQ1 - Quality Metrics:**
  - Compare quality metrics: Initial state vs. Refactored before fix
  - **Focus:** Impact of refactoring on coupling, cohesion, and complexity
  - Identify improvements/degradations from refactoring
  - Calculate percentage changes
  - **Note:** Bug fix is not part of RQ1 analysis
- [ ] **RQ2 - Effort Metrics:**
  - Compare effort metrics: Team A (fix on initial/non-refactored) vs. Team B (fix on refactored)
  - Both teams started from the same initial state
  - Calculate differences: time, files, LOC, churn
  - Calculate percentage changes
  - Identify which team required more/less effort
  - Answer: Did refactoring reduce maintenance effort?

### Step 6: Interpretation
- [ ] Analyze results for RQ1 (quality impact of refactoring)
- [ ] Analyze results for RQ2 (effort comparison: Team A vs. Team B)
- [ ] Document findings and insights
- [ ] Answer: Did refactoring reduce maintenance effort? (Compare Team A vs. Team B)

---

## Expected Findings

### RQ1: Quality Improvements Expected
- **Lower Coupling:** New classes (GameStateManager, StatusDisplayBuilder) should reduce coupling in Control and HudFragment
- **Higher Cohesion:** Extracted classes should have higher cohesion (single responsibility)
- **Lower Complexity:** Methods in extracted classes should have lower cyclomatic complexity
- **Better Maintainability:** Better separation of concerns

### RQ2: Effort Comparison Expected (Team A vs. Team B)
- **File Count:** Team A (Initial): 5 files, Team B (After refactoring): 7 files
- **Different File Structure:** Team B works with 2 new extracted classes (GameStateManager, StatusDisplayBuilder)
- **Time Comparison:** 
  - Team A may have spent more time navigating large files (Control ~1200 lines, HudFragment ~960 lines)
  - Team B may have spent less time due to better code organization, even with more files
- **LOC Changes:** 
  - Team A: Changes in large monolithic files
  - Team B: Changes in smaller, focused classes
- **Hypothesis:** Team B (refactored) should require less effort despite more files, due to better separation of concerns

---

## Tools Needed

1. **Code Analysis Tools:**
   - Understand (SciTools)
   - SonarQube
   - CKJM (Chidamber-Kemerer Java Metrics)
   - PMD
   - Custom Python/Java scripts

2. **Version Control:**
   - Git (for commit checkout and diff analysis)

3. **Documentation:**
   - Spreadsheet (Excel/Google Sheets) for metric comparison
   - Markdown for documentation

---

## Deliverables

1. **Metrics Spreadsheet**
   - Quality metrics for both states (before and after fixes)
   - Effort metrics for Team A (Initial) and Team B (After refactoring)
   - Comparative analysis (Team A vs. Team B)

2. **Analysis Report**
   - Summary of findings
   - Interpretation of results
   - Answers to RQ1 (quality impact) and RQ2 (effort comparison)
   - Team A vs. Team B comparison

3. **Code Snapshots**
   - Initial state files (before and after Team A's fix)
   - After refactoring files (before and after Team B's fix)
   - Diff analysis for both teams

---

## Timeline

1. **Extract Code:** 30 min
2. **Apply Fix to Initial State:** 30 min
3. **Calculate Metrics:** 2-3 hours (depending on tool)
4. **Comparative Analysis:** 1 hour
5. **Documentation:** 1 hour

**Total Estimated Time:** 5-6 hours

