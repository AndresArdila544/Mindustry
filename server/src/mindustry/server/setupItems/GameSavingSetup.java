package mindustry.server.setupItems;

import arc.Core;
import arc.Events;
import arc.util.Interval;
import arc.util.Timer;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.net.Administration;
import mindustry.server.ServerControl;

import static mindustry.Vars.netServer;
import static mindustry.Vars.state;

public class GameSavingSetup extends SetupItem
{

    private final Interval autosaveCount = new Interval();

    @Override
    public void initiateEvents() {
        //reset autosave on world load
        Events.on(EventType.WorldLoadEvent.class, e -> {
            autosaveCount.reset(0, Administration.Config.autosaveSpacing.num() * 60);
        });

        Events.on(EventType.SaveLoadEvent.class, e -> {
            Core.app.post(() -> {
                if(Administration.Config.autoPause.bool() && Groups.player.size() == 0){
                    state.set(GameState.State.paused);
                    ServerControl.instance.setAutoPaused(true);
                }
            });
        });
    }

    @Override
    public void setupSteps() {

        //autosave settings once a minute
        float saveInterval = 60;
        Timer.schedule(() -> {
                netServer.admins.forceSave();
                Core.settings.forceSave();
            }, saveInterval, saveInterval);

    }
}
