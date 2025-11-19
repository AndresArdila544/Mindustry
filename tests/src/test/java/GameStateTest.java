import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import mindustry.core.GameState;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.type.Sector;
import mindustry.type.Planet;
import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import arc.Events;

/**
 * Tests for GameState class.
 * Tests the actual GameState class methods.
 */
public class GameStateTest extends ApplicationTests {
    
    private GameState gameState;
    private MockedStatic<Events> eventsMock;
    
    @BeforeAll
    public static void setup() {
        ApplicationTests.launchApplication();
    }
    
    @BeforeEach
    void setUp() {
        // Mock Events.fire() to avoid side effects during testing
        eventsMock = Mockito.mockStatic(Events.class);
        gameState = state;
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (eventsMock != null) {
            eventsMock.close();
        }
    }
    
    @Test
    void testInitialState() {
        // Test initial state is menu
        assertTrue(gameState.isMenu(), "Initial state should be menu");
        assertFalse(gameState.isPlaying(), "Initial state should not be playing");
        assertFalse(gameState.isPaused(), "Initial state should not be paused");
        assertEquals(GameState.State.menu, gameState.getState(), "Initial state should be menu");
    }
    
    @Test
    void testStateTransitions() {
        // Test transitioning to playing state
        gameState.set(GameState.State.playing);
        assertTrue(gameState.isPlaying(), "State should be playing after set(playing)");
        assertFalse(gameState.isPaused(), "State should not be paused");
        assertFalse(gameState.isMenu(), "State should not be menu");
        assertEquals(GameState.State.playing, gameState.getState(), "getState() should return playing");
        
        // Verify Events.fire was called
        eventsMock.verify(() -> Events.fire(any(StateChangeEvent.class)));
    }
    
    @Test
    void testStateTransitionsToPaused() {
        // Test transitioning to paused state
        gameState.set(GameState.State.paused);
        assertTrue(gameState.isPaused(), "State should be paused after set(paused)");
        assertFalse(gameState.isPlaying(), "State should not be playing");
        assertFalse(gameState.isMenu(), "State should not be menu");
        assertEquals(GameState.State.paused, gameState.getState(), "getState() should return paused");
    }
    
    @Test
    void testSetSameStateDoesNotFireEvent() {
        // Test that setting the same state doesn't fire event
        gameState.set(GameState.State.menu);
        eventsMock.clearInvocations();
        
        // Setting same state again should not fire event
        gameState.set(GameState.State.menu);
        eventsMock.verifyNoInteractions();
    }
    
    @Test
    void testIsCampaign() {
        // Test campaign detection - initially false
        assertFalse(gameState.isCampaign(), "Should not be campaign initially");
        assertFalse(gameState.hasSector(), "Should not have sector initially");
        assertNull(gameState.getSector(), "getSector() should return null initially");
        
        // Create a mock sector and set it
        Sector mockSector = Mockito.mock(Sector.class);
        gameState.rules.sector = mockSector;
        
        assertTrue(gameState.isCampaign(), "Should be campaign when sector is set");
        assertTrue(gameState.hasSector(), "Should have sector when sector is set");
        assertSame(mockSector, gameState.getSector(), "getSector() should return the set sector");
    }
    
    @Test
    void testIsEditor() {
        // Test editor detection
        assertFalse(gameState.isEditor(), "Should not be editor initially");
        
        gameState.rules.editor = true;
        assertTrue(gameState.isEditor(), "Should be editor when rules.editor is true");
    }
    
    @Test
    void testGetPlanet() {
        // Test getPlanet() when no sector
        Planet testPlanet = content.planets().find(p -> p.sectors.size > 0);
        if(testPlanet != null){
            gameState.rules.planet = testPlanet;
            assertSame(testPlanet, gameState.getPlanet(), "getPlanet() should return rules.planet when no sector");
            
            // Test getPlanet() when sector exists
            // Use a real sector from the planet
            Sector testSector = testPlanet.sectors.first();
            gameState.rules.sector = testSector;
            
            assertSame(testSector.planet, gameState.getPlanet(), "getPlanet() should return sector.planet when sector exists");
        }
    }
    
    @Test
    void testIsGame() {
        // Test isGame() - returns true when not menu
        assertFalse(gameState.isGame(), "Should not be game when in menu");
        
        gameState.set(GameState.State.playing);
        assertTrue(gameState.isGame(), "Should be game when playing");
        
        gameState.set(GameState.State.paused);
        assertTrue(gameState.isGame(), "Should be game when paused");
    }
    
    @Test
    void testIsMethod() {
        // Test is() method
        assertTrue(gameState.is(GameState.State.menu), "is(menu) should return true when in menu");
        assertFalse(gameState.is(GameState.State.playing), "is(playing) should return false when in menu");
        
        gameState.set(GameState.State.playing);
        assertTrue(gameState.is(GameState.State.playing), "is(playing) should return true when playing");
    }
    
    @Test
    void testBoss() {
        // Test boss() method - should return null when no bosses
        assertNull(gameState.boss(), "boss() should return null when no bosses");
    }
    
    @Test
    void testDefaultValues() {
        // Test default field values
        assertEquals(1, gameState.wave, "Default wave should be 1");
        assertEquals(0f, gameState.wavetime, "Default wavetime should be 0");
        assertEquals(0.0, gameState.tick, "Default tick should be 0");
        assertEquals(0L, gameState.updateId, "Default updateId should be 0");
        assertFalse(gameState.gameOver, "Default gameOver should be false");
        assertFalse(gameState.won, "Default won should be false");
        assertEquals(-1, gameState.serverTps, "Default serverTps should be -1");
        assertEquals(0, gameState.enemies, "Default enemies should be 0");
        assertNotNull(gameState.rules, "rules should be initialized");
        assertNotNull(gameState.teams, "teams should be initialized");
        assertNotNull(gameState.stats, "stats should be initialized");
    }
}

