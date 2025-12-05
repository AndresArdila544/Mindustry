# Metrics Analysis - File Organization

## RQ1: Design Quality Metrics

**Comparison:** Initial State vs. Refactored Before Fix

### Initial State (Before Refactoring)
**Commit:** `7d6a547680737678308252b76af2c8ab2941c96c`
**Files (5):**
- Control.java
- GameState.java
- GameOverDialog.java
- PausedDialog.java
- HudFragment.java

### Refactored Before Fix (After Refactoring)
**Commit:** `d25e64aef1f86cca31249503dcd4944130c42de4`
**Files (7):**
- Control.java (refactored - delegates to GameStateManager)
- GameState.java
- GameStateManager.java (NEW - extracted from Control)
- GameOverDialog.java
- PausedDialog.java
- StatusDisplayBuilder.java (NEW - extracted from HudFragment)
- HudFragment.java (refactored - delegates to StatusDisplayBuilder)

## RQ2: Maintenance Effort Metrics

**Comparison:** Team A (fix on initial state) vs. Team B (fix on refactored state)

### Team A: Fix on Initial State
**Base Commit:** `7d6a547680737678308252b76af2c8ab2941c96c`
**Fix Commit:** [To be provided]
**Files:** [To be extracted when commit is available]

### Team B: Fix on Refactored State
**Base Commit:** `d25e64aef1f86cca31249503dcd4944130c42de4`
**Fix Commit:** `dd1d0b05430a6bb50ce3ffac805b58ea55090a72`
**Files (7):** Same as refactored_before_fix but with bug fix applied

## Metrics to Calculate

### RQ1 Metrics (Initial vs. Refactored Before Fix)
1. CBO (Coupling Between Objects)
2. LCOM (Lack of Cohesion of Methods)
3. Cyclomatic Complexity
4. LOC (Lines of Code)
5. WMC (Weighted Methods per Class)

### RQ2 Metrics (Team A vs. Team B)
1. Time Spent
2. Number of Files Modified
3. LOC Changed (additions, deletions, net change)
4. Code Churn

