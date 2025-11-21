package mindustry.server.setupItems;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.util.Strings;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.io.JsonIO;
import mindustry.maps.Map;
import mindustry.maps.Maps;
import mindustry.net.Administration;
import mindustry.net.Packets;
import mindustry.server.ServerControl;
import mindustry.game.EventType.*;

import java.util.Scanner;

import static arc.util.Log.err;
import static arc.util.Log.info;
import static mindustry.Vars.*;

public class GameFlowSetup extends SetupItem
{
    /** Whether the server is currently waiting for the next map to be loaded. */
    public boolean inGameOverWait;
    public Cons<EventType.GameOverEvent> gameOverListener;
    public Runnable serverInput = () -> {
        Scanner scan = new Scanner(System.in);
        while(scan.hasNext()){
            String line = scan.nextLine();
            Core.app.post(() -> ServerControl.instance.handleCommandString(line));
        }
    };

    public GameFlowSetup() {
        inGameOverWait = ServerControl.instance.inGameOverWait;
        gameOverListener = ServerControl.instance.gameOverListener;
    }

    @Override
    public void initiateEvents() {

        Events.on(EventType.GameOverEvent.class, event -> {
            if(!inGameOverWait && gameOverListener != null){
                gameOverListener.get(event);
            }
        });

        Events.on(EventType.ServerLoadEvent.class, e -> {
            if(serverInput != null){
                Thread thread = new Thread(serverInput, "Server Controls");
                thread.setDaemon(true);
                thread.start();
            }

            info("Server loaded. Type @ for help.", "'help'");
        });

        Events.on(EventType.PlayEvent.class, e -> {
            try{
                JsonValue value = JsonIO.json.fromJson(null, Core.settings.getString("globalrules"));
                JsonIO.json.readFields(state.rules, value);
            }catch(Throwable t){
                err("Error applying custom rules, proceeding without them.", t);
            }
        });

        Events.on(EventType.ResetEvent.class, e -> {
            ServerControl.instance.setAutoPaused(false);
        });

        // when a new game is started
        Events.run(Trigger.newGame, () -> {

            // if autopause is enabled
            if(Administration.Config.autoPause.bool()) {
                // if there are no players, autopause the game
                if (Groups.player.isEmpty()) {
                    ServerControl.instance.setAutoPaused(true);
                    state.set(GameState.State.paused);

                    // if there are players, and the game is paused, play it
                } else if (ServerControl.instance.isAutoPaused()) {
                    ServerControl.instance.setAutoPaused(false);
                    state.set(GameState.State.playing);
                }
            }

            // if autopause is disabled, start the game right away
            else if(ServerControl.instance.isAutoPaused()) {
                ServerControl.instance.setAutoPaused(false);
                state.set(GameState.State.playing);
            }
        });

        Events.run(EventType.Trigger.update, () -> {
            if(Administration.Config.autoPause.bool()){
                if(Groups.player.isEmpty()){
                    ServerControl.instance.setAutoPaused(true);
                    state.set(GameState.State.paused);
                }else if(ServerControl.instance.isAutoPaused()){
                    ServerControl.instance.setAutoPaused(false);
                    state.set(GameState.State.playing);
                }
            }else if(ServerControl.instance.isAutoPaused() && Vars.state.isPaused()){ //unpause when the config is disabled
                state.set(GameState.State.playing);
                ServerControl.instance.setAutoPaused(false);
            }
        });
    }

    @Override
    public void setupSteps() {

        //set up default shuffle mode
        try{
            maps.setShuffleMode(Maps.ShuffleMode.valueOf(Core.settings.getString("shufflemode")));
        }catch(Exception e){
            maps.setShuffleMode(Maps.ShuffleMode.all);
        }

        // send modified variables back to ServerControl
        ServerControl.instance.inGameOverWait = inGameOverWait;
        ServerControl.instance.gameOverListener = gameOverListener;
    }
}
