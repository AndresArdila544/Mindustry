package mindustry.core;

import arc.*;
import mindustry.core.GameState.*;
import mindustry.input.*;

import static arc.Core.*;
import static mindustry.Vars.*;

/**
 * Manages game state transitions, particularly pause/unpause logic.
 * Extracted from Control to separate state management concerns.
 */
public class GameStateManager{
    private boolean wasPaused = false;
    private boolean backgroundPaused = false;

    /**
     * Checks if pause can be toggled based on current conditions.
     * @return true if pause can be toggled
     */
    public boolean canTogglePause(){
        return !net.client() && Core.input.keyTap(Binding.pause) && !renderer.isCutscene() && !scene.hasDialog() && !scene.hasKeyboard() && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing));
    }

    /**
     * Handles pause input and toggles pause state if conditions are met.
     */
    public void handlePauseInput(){
        if(canTogglePause()){
            state.set(state.isPaused() ? State.playing : State.paused);
        }
    }

    /**
     * Handles menu input, which can also pause the game.
     */
    public void handleMenuInput(){
        if(Core.input.keyTap(Binding.menu) && !ui.restart.isShown() && !ui.minimapfrag.shown()){
            if(ui.chatfrag.shown()){
                ui.chatfrag.hide();
            }else if(!ui.paused.isShown() && !scene.hasDialog()){
                ui.paused.show();
                if(!net.active()){
                    state.set(State.paused);
                }
            }
        }
    }

    /**
     * Updates pause-related state during game updates.
     * Handles background pause and cutscene unpause logic.
     */
    public void updatePauseState(){
        if(backgroundPaused && settings.getBool("backgroundpause") && !net.active()){
            state.set(State.paused);
        }

        //cannot launch while paused
        if(state.isPaused() && renderer.isCutscene()){
            state.set(State.playing);
        }
    }

    /**
     * Called when the application is paused (e.g., app goes to background).
     * Pauses the game if background pause is enabled.
     */
    public void onApplicationPause(){
        if(settings.getBool("backgroundpause", true) && !net.active()){
            backgroundPaused = true;
            wasPaused = state.is(State.paused);
            if(state.is(State.playing)){
                state.set(State.paused);
            }
        }
    }

    /**
     * Called when the application is resumed (e.g., app comes to foreground).
     * Unpauses the game if it was auto-paused by background pause.
     */
    public void onApplicationResume(){
        if(state.is(State.paused) && !wasPaused && settings.getBool("backgroundpause", true) && !net.active()){
            state.set(State.playing);
        }
        backgroundPaused = false;
    }
}

