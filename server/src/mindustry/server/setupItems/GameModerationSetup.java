package mindustry.server.setupItems;

import arc.Events;
import arc.util.Log;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.mod.Mods;
import mindustry.net.Administration;
import mindustry.server.ServerControl;

import static arc.util.Log.info;
import static mindustry.Vars.mods;
import static mindustry.Vars.state;

public class GameModerationSetup extends SetupItem
{

    @Override
    public void initiateEvents() {

        Events.on(EventType.PlayerJoin.class, e -> {
            if(state.isPaused() && ServerControl.instance.isAutoPaused() && Administration.Config.autoPause.bool()){
                state.set(GameState.State.playing);
                ServerControl.instance.setAutoPaused(false);
            }
        });

        Events.on(EventType.PlayerLeave.class, e -> {
            // The player list length is compared with 1 and not 0 here,
            // because when PlayerLeave gets fired, the player hasn't been removed from the player list yet
            if(!state.isPaused() && Administration.Config.autoPause.bool() && Groups.player.size() == 1){
                state.set(GameState.State.paused);
                ServerControl.instance.setAutoPaused(true);
            }
        });

    }

    @Override
    public void setupSteps() {

        if(!mods.orderedMods().isEmpty()){
            info("@ mods loaded.", mods.orderedMods().size);
        }

        int unsupported = mods.list().count(l -> !l.enabled());

        if(unsupported > 0){
            Log.err("There were errors loading @ mod(s):", unsupported);
            for(Mods.LoadedMod mod : mods.list().select(l -> !l.enabled())){
                Log.err("- @ &ly(" + mod.state + ")", mod.meta.name);
            }
        }

    }
}
