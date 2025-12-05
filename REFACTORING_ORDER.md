# Refactoring Order Recommendation

## Strategy Overview
**Principle**: Extract methods first, then extract classes. Start with simpler, isolated refactorings before tackling complex, interdependent ones.

---

## Phase 1: Method Extraction (Low Risk, High Value)
*Goal: Break down long methods into smaller, testable units without changing class structure*

### 1.1 Control.java - Constructor Event Listeners (Lines 60-285)
**Priority: HIGH** | **Risk: LOW** | **Estimated Time: Medium**

**Why First:**
- Constructor is called once at initialization
- Event listeners are relatively independent
- Low risk of breaking existing functionality
- Makes subsequent refactorings easier

**Order:**
1. `setupBuildDamageListener()` - Lines 64-68 (simplest, isolated)
2. `setupClientLoadListener()` - Lines 71-78
3. `setupStateChangeListener()` - Lines 80-84
4. `setupWorldLoadListeners()` - Lines 93-103, 135-141, 144-154 (group related)
5. `setupGameOverListeners()` - Lines 127-132, 183-191
6. `setupSectorListeners()` - Lines 156-180
7. `setupNewGameListener()` - Lines 193-268 (most complex)

**Dependencies:** None - these are all independent event handlers

---

### 1.2 HudFragment.java - build() Method (Lines 144-567)
**Priority: VERY HIGH** | **Risk: MEDIUM** | **Estimated Time: High**

**Why Second:**
- Very large method (423 lines) - high impact
- UI building is relatively self-contained
- Can be tested incrementally
- Some methods may be reused in class extraction phase

**Order:**
1. `setupEventListeners()` - Lines 147-186 (independent, no UI dependencies)
2. `buildPausedTable()` - Lines 188-195 (simple, isolated)
3. `buildWaitingTable()` - Lines 197-202 (simple, isolated)
4. `buildMinimap()` - Lines 204-220 (moderate complexity)
5. `buildSavingIndicator()` - Lines 509-514 (simple)
6. `buildSpawnerWarning()` - Lines 499-507 (simple)
7. `buildCoreInfo()` - Lines 422-497 (complex, contains multiple sub-components)
8. `buildGutterAreas()` - Lines 224-420 (most complex, contains mobile buttons, waves/editor tables, FPS display)

**Dependencies:** 
- Steps 1-6 are independent
- Step 7 may reference earlier methods
- Step 8 is most complex and should be last

---

### 1.3 Control.java - playSector() Method (Lines 409-527)
**Priority: HIGH** | **Risk: MEDIUM** | **Estimated Time: High**

**Why Third:**
- Complex method with nested logic
- Critical game functionality - needs careful testing
- Will inform SectorLoader class extraction later

**Order:**
1. `shouldCreateNewSector()` - Decision logic (extract condition from line 410)
2. `loadSectorSave()` - Lines 428-432 (save loading, relatively simple)
3. `handleSaveException()` - Lines 498-504 (error handling, isolated)
4. `restoreSectorWithDamage()` - Lines 438-492 (most complex, deeply nested)
   - Within this, extract:
     - `restoreEnemyBase()` - Lines 460-472
     - `resetGameState()` - Lines 444-457
     - `placeSpawnCore()` - Lines 438-442

**Dependencies:**
- Step 1 is independent
- Steps 2-3 are independent
- Step 4 depends on understanding the full flow

---

### 1.4 Control.java - update() Method (Lines 633-725)
**Priority: HIGH** | **Risk: MEDIUM** | **Estimated Time: Medium**

**Why Fourth:**
- Called every frame - performance critical
- Multiple responsibilities mixed together
- Some extracted methods will be reused in GameStateManager

**Order:**
1. `updateSaves()` - Line 611 (simple, one line)
2. `updateAssets()` - Lines 614-617 (simple, isolated)
3. `updateInput()` - Line 619 (simple, one line)
4. `updateSound()` - Line 621 (simple, one line)
5. `handleFullscreenToggle()` - Lines 623-631 (isolated feature)
6. `validatePlayerPosition()` - Lines 633-638 (validation logic)
7. `canTogglePause()` - Extract condition from line 666 (will be reused)
8. `handlePauseInput()` - Lines 666-668 (pause/unpause logic)
9. `handleMenuInput()` - Lines 670-679 (menu handling)
10. `updateGameState()` - Lines 640-668 (game state updates: indicators, RPC, core items, pause)
11. `updateMenuState()` - Lines 685-694 (menu state updates)

**Dependencies:**
- Steps 1-6 are independent
- Step 7 is used by step 8
- Steps 8-11 depend on understanding the state flow

---

### 1.5 HudFragment.java - makeStatusTable() Method (Lines 735-1001)
**Priority: VERY HIGH** | **Risk: MEDIUM** | **Estimated Time: High**

**Why Fifth:**
- Very large method (266 lines)
- Contains inner class (SideBar) that should be extracted
- Complex string building logic
- Will inform StatusDisplayBuilder class extraction

**Order:**
1. Extract `SideBar` inner class (Lines 755-831) to separate static inner class or separate file
2. `buildPlayerStatusBar()` - Lines 833-866 (health/ammo bars)
3. `buildStatusText()` - Lines 871-952 (main status text building)
4. `buildStatusString()` - Extract string building logic from step 3
   - Within this, extract:
     - `appendObjectives()` - Lines 895-913
     - `appendEnemyCoreStatus()` - Lines 915-919
     - `appendWaveStatus()` - Lines 929-950

**Dependencies:**
- Step 1 is independent (extract inner class first)
- Step 2 is independent
- Steps 3-4 are sequential

---

## Phase 2: Class Extraction (Higher Risk, Structural Changes)
*Goal: Extract responsibilities into separate classes to reduce God Class complexity*

### 2.1 Extract ToastManager from HudFragment
**Priority: MEDIUM** | **Risk: LOW** | **Estimated Time: Low**

**Why First in Phase 2:**
- Most isolated functionality
- `showToast()` and `showUnlock()` are self-contained
- Low coupling with rest of HudFragment
- Easy to test independently

**Methods to Extract:**
- `showToast()` - Lines 586-620
- `showUnlock()` - Lines 622-710
- `scheduleToast()` - Lines 570-580
- `hasToast()` - Lines 582-584

**Dependencies:** None - these methods are relatively independent

---

### 2.2 Extract GameStateManager from Control
**Priority: HIGH** | **Risk: MEDIUM** | **Estimated Time: Medium**

**Why Second:**
- Pause logic is used in multiple places
- Will clean up Control.update() significantly
- Moderate complexity

**Methods to Extract:**
- `handlePauseInput()` - From update() method
- `canTogglePause()` - From update() method
- Pause-related state management from update()

**Dependencies:** Uses methods extracted in Phase 1.4 (steps 7-8)

---

### 2.3 Extract SectorLoader from Control
**Priority: HIGH** | **Risk: HIGH** | **Estimated Time: High**

**Why Third:**
- Complex sector loading logic
- Critical game functionality
- High risk if not done carefully

**Methods to Extract:**
- `playSector()` - Main sector loading (already refactored in Phase 1.3)
- `playNewSector()` - New sector initialization
- `restoreSectorWithDamage()` - Building restoration (already extracted)
- `loadSectorSave()` - Save loading (already extracted)
- `shouldCreateNewSector()` - Decision logic (already extracted)
- `handleSaveException()` - Error handling (already extracted)

**Dependencies:** 
- Depends on Phase 1.3 being complete
- May need access to Control's state (saves, etc.)

---

### 2.4 Extract ControlEventHandler from Control
**Priority: MEDIUM** | **Risk: LOW** | **Estimated Time: Medium**

**Why Fourth:**
- All event listeners already extracted in Phase 1.1
- Low risk - just moving code to new class
- Clean separation of concerns

**Methods to Extract:**
- All `setup*Listener()` methods from Phase 1.1

**Dependencies:** Depends on Phase 1.1 being complete

---

### 2.5 Extract StatusDisplayBuilder from HudFragment
**Priority: MEDIUM** | **Risk: MEDIUM** | **Estimated Time: Medium**

**Why Fifth:**
- Status table building is complex
- Methods already extracted in Phase 1.5
- Moderate coupling with HudFragment

**Methods to Extract:**
- `makeStatusTable()` - Main method (already refactored)
- `buildPlayerStatusBar()` - From Phase 1.5
- `buildStatusText()` - From Phase 1.5
- `buildStatusString()` and helpers - From Phase 1.5
- `SideBar` class - From Phase 1.5

**Dependencies:** Depends on Phase 1.5 being complete

---

### 2.6 Extract HudUIBuilder from HudFragment
**Priority: LOW** | **Risk: MEDIUM** | **Estimated Time: High**

**Why Last:**
- Most complex extraction
- High coupling with HudFragment
- Many UI components depend on each other
- Can be done incrementally

**Methods to Extract:**
- `build()` - Main HUD building (already refactored in Phase 1.2)
- All `build*()` methods from Phase 1.2
- `setupEventListeners()` - From Phase 1.2

**Dependencies:** 
- Depends on Phase 1.2 being complete
- May want to keep some methods in HudFragment for now

---

## Summary: Recommended Execution Order

### Week 1: Foundation (Low Risk)
1. ✅ Control.java Constructor Event Listeners (1.1)
2. ✅ HudFragment build() - Simple methods first (1.2, steps 1-6)

### Week 2: Core Methods
3. ✅ Control.java playSector() extraction (1.3)
4. ✅ Control.java update() extraction (1.4)
5. ✅ HudFragment makeStatusTable() extraction (1.5)

### Week 3: Class Extraction - Easy Wins
6. ✅ Extract ToastManager (2.1)
7. ✅ Extract ControlEventHandler (2.4) - Easy since methods already extracted

### Week 4: Class Extraction - Core Functionality
8. ✅ Extract GameStateManager (2.2)
9. ✅ Extract SectorLoader (2.3) - High value, but needs careful testing

### Week 5: Class Extraction - UI Components
10. ✅ Extract StatusDisplayBuilder (2.5)
11. ✅ Extract HudUIBuilder (2.6) - Can be done incrementally

---

## Testing Strategy

After each phase:
1. **Unit Tests**: Test extracted methods in isolation
2. **Integration Tests**: Verify methods work together
3. **Manual Testing**: Play through game scenarios
4. **Regression Testing**: Ensure no functionality is broken

**Critical Test Scenarios:**
- Sector loading (new and existing)
- Pause/unpause functionality
- UI rendering and interactions
- Toast notifications
- Status display updates
- Event handling

---

## Risk Mitigation

1. **Use Feature Branches**: One branch per phase
2. **Incremental Commits**: Commit after each method extraction
3. **Keep Old Code**: Comment out old code initially, remove after verification
4. **Code Reviews**: Review each phase before moving to next
5. **Rollback Plan**: Keep ability to revert if issues arise

---

## Notes

- **Dependencies are key**: Always complete method extraction before class extraction
- **Test frequently**: Don't wait until the end to test
- **Incremental is better**: Small, tested changes beat large refactorings
- **Document as you go**: Update comments and documentation with each change

