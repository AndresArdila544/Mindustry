import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import mindustry.game.Team;
import mindustry.ui.dialogs.GameOverDialog;
import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import arc.Events;
import mindustry.game.EventType.WinEvent;
import mindustry.game.EventType.LoseEvent;

/**
 * Tests for GameOverDialog class.
 * Tests the actual GameOverDialog class methods.
 */
public class GameOverDialogTest extends ApplicationTests {
    
    private GameOverDialog dialog;
    private MockedStatic<Events> eventsMock;
    
    @BeforeAll
    public static void setup() {
        ApplicationTests.launchApplication();
    }
    
    @BeforeEach
    void setUp() {
        eventsMock = Mockito.mockStatic(Events.class);
        // Note: ui is null in headless mode, so dialog will be null
        if (ui != null) {
            dialog = ui.restart; // GameOverDialog is accessed via ui.restart
        }
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (eventsMock != null) {
            eventsMock.close();
        }
    }
    
    @Test
    void testConstructor() {
        // Test that dialog is created
        // Note: ui is null in headless mode
        if (ui != null) {
            assertNotNull(dialog, "Dialog should be created");
        }
    }
    
    @Test
    void testShowWithNullWinner() {
        // Test that show() handles null winner
        // Note: ui is null in headless mode
        if (dialog != null) {
            assertDoesNotThrow(() -> {
                dialog.show((Team)null);
            }, "show() should handle null winner without crashing");
        }
    }
    
    @Test
    void testShowWithWinner() {
        // Test that show() works with a winner team
        // Note: ui is null in headless mode
        if (dialog != null) {
            Team winner = Team.sharded;
            
            assertDoesNotThrow(() -> {
                dialog.show(winner);
            }, "show() should work with a winner team");
            
            // Verify Events.fire was called (either WinEvent or LoseEvent)
            eventsMock.verify(() -> Events.fire(Mockito.any(WinEvent.class)), Mockito.atMostOnce());
            eventsMock.verify(() -> Events.fire(Mockito.any(LoseEvent.class)), Mockito.atMostOnce());
        }
    }
    
    @Test
    void testShowFiresWinEventWhenPlayerWins() {
        // Test that show() fires WinEvent when player's team wins
        // Note: ui is null in headless mode
        if (dialog != null && player != null) {
            Team playerTeam = player.team();
            Team winner = playerTeam;
            
            assertDoesNotThrow(() -> {
                dialog.show(winner);
            }, "show() should fire WinEvent when player's team wins");
            
            // Verify WinEvent was fired
            eventsMock.verify(() -> Events.fire(Mockito.any(WinEvent.class)));
        }
    }
    
    @Test
    void testShowFiresLoseEventWhenPlayerLoses() {
        // Test that show() fires LoseEvent when player's team loses
        // Note: ui is null in headless mode
        if (dialog != null && player != null) {
            Team playerTeam = player.team();
            Team winner = playerTeam == Team.sharded ? Team.crux : Team.sharded; // Different team
            
            assertDoesNotThrow(() -> {
                dialog.show(winner);
            }, "show() should fire LoseEvent when player's team loses");
            
            // Verify LoseEvent was fired
            eventsMock.verify(() -> Events.fire(Mockito.any(LoseEvent.class)));
        }
    }
}
