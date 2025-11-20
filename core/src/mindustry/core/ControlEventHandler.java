package mindustry.core;

import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.scene.style.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Objectives.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.io.*;

import static arc.Core.*;
import static mindustry.Vars.*;

/**
 * Handles all event listeners for Control.
 * Extracted from Control to separate event handling concerns.
 */
public class ControlEventHandler{
    private final Control control;

    public ControlEventHandler(Control control){
        this.control = control;
    }

    /**
     * Sets up listener for build damage events.
     * Adds attack indicators when player's team buildings are damaged.
     */
    public void setupBuildDamageListener(){
        Events.on(BuildDamageEvent.class, e -> {
            if(e.build.team == player.team()){
                control.indicators.add(e.build.tileX(), e.build.tileY());
            }
        });
    }

    /**
     * Sets up listener for client load events.
     * Shows dialog if mod loading was skipped and checks auto-unlocks.
     */
    public void setupClientLoadListener(){
        //show dialog saying that mod loading was skipped.
        Events.on(ClientLoadEvent.class, e -> {
            if(mods.skipModLoading() && mods.list().any()){
                Time.runTask(4f, () -> {
                    ui.showInfo("@mods.initfailed");
                });
            }
            control.checkAutoUnlocks();
        });
    }

    /**
     * Sets up listener for state change events.
     * Updates RPC when transitioning between menu and playing states.
     */
    public void setupStateChangeListener(){
        Events.on(StateChangeEvent.class, event -> {
            if((event.from == State.playing && event.to == State.menu) || (event.from == State.menu && event.to != State.menu)){
                Time.runTask(5f, platform::updateRPC);
            }
        });
    }

    /**
     * Sets up listeners for world load events.
     * Handles player/camera positioning, player addition, and PVP autohost.
     */
    public void setupWorldLoadListeners(){
        // Set player/camera position based on player position
        Events.on(WorldLoadEvent.class, event -> {
            if(Mathf.zero(player.x) && Mathf.zero(player.y)){
                Building core = player.bestCore();
                if(core != null){
                    player.set(core);
                    camera.position.set(core);
                }
            }else{
                camera.position.set(player);
            }
        });

        // Add player when world loads regardless
        Events.on(WorldLoadEvent.class, e -> {
            player.add();
            // Make player admin on any load when hosting
            if(net.active() && net.server()){
                player.admin = true;
            }
        });

        // Autohost for PVP maps
        Events.on(WorldLoadEvent.class, event -> app.post(() -> {
            if(state.rules.pvp && !net.active()){
                try{
                    net.host(port);
                    player.admin = true;
                }catch(IOException e){
                    ui.showException("@server.error", e);
                    state.set(State.menu);
                }
            }
        }));
    }

    /**
     * Sets up listeners for game over events.
     * Handles stats, effects, and save deletion on campaign game over.
     */
    public void setupGameOverListeners(){
        Events.on(GameOverEvent.class, event -> {
            state.stats.wavesLasted = state.wave;
            Effect.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
            //the restart dialog can show info for any number of scenarios
            Call.gameOver(event.winner);
        });

        // Delete save on campaign game over
        Events.on(GameOverEvent.class, e -> {
            if(state.isCampaign() && !net.client() && !headless){
                //save gameover state immediately
                if(control.saves.getCurrent() != null){
                    control.saves.getCurrent().save();
                }
            }
        });
    }

    /**
     * Sets up listeners for sector-related events.
     * Handles unlock events and sector capture events.
     */
    public void setupSectorListeners(){
        Events.on(UnlockEvent.class, e -> {
            if(e.content.showUnlock()){
                ui.hudfrag.showUnlock(e.content);
            }

            control.checkAutoUnlocks();

            if(e.content instanceof SectorPreset){
                for(TechNode node : TechTree.all){
                    if(!node.content.unlocked() && node.objectives.contains(o -> o instanceof SectorComplete sec && sec.preset == e.content) && !node.objectives.contains(o -> !o.complete())){
                        ui.hudfrag.showToast(new TextureRegionDrawable(node.content.uiIcon), iconLarge, bundle.get("available"));
                    }
                }
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            app.post(control::checkAutoUnlocks);

            if(!net.client() && e.sector.preset != null && e.sector.preset.isLastSector && e.initialCapture){
                Time.run(60f * 2f, () -> {
                    ui.campaignComplete.show(e.sector.planet);
                });
            }
        });
    }

    /**
     * Sets up listener for new game trigger.
     * Handles core landing animation, music, and prebuild base logic.
     */
    public void setupNewGameListener(){
        Events.run(Trigger.newGame, () -> {
            var core = player.bestCore();
            if(core == null) return;

            camera.position.set(core);
            player.set(core);

            float coreDelay = 0f;
            if(!settings.getBool("skipcoreanimation") && !state.rules.pvp){
                coreDelay = core.launchDuration();
                //delay player respawn so animation can play.
                player.deathTimer = Player.deathDelay - core.launchDuration();
                //TODO this sounds pretty bad due to conflict
                if(settings.getInt("musicvol") > 0){
                    //TODO what to do if another core with different music is already playing?
                    Music music = core.landMusic();
                    music.stop();
                    music.play();
                    music.setVolume(settings.getInt("musicvol") / 100f);
                }

                renderer.showLanding(core);
            }

            if(state.isCampaign()){
                if(state.rules.sector.info.importRateCache != null){
                    state.rules.sector.info.refreshImportRates(state.rules.sector.planet);
                }

                //don't run when hosting, that doesn't really work.
                if(state.rules.sector.planet.prebuildBase){
                    control.toBePlaced.clear();
                    float unitsPerTick = 2f;
                    float buildRadius = state.rules.enemyCoreBuildRadius * 1.5f;

                    //TODO if the save is unloaded or map is hosted, these blocks do not get built.
                    boolean anyBuilds = false;
                    for(var build : state.rules.defaultTeam.data().buildings.copy()){
                        if(!(build instanceof CoreBuild) && !build.block.privileged){
                            var ccore = build.closestCore();

                            if(ccore != null){
                                anyBuilds = true;

                                if(!net.active()){
                                    build.pickedUp();
                                    build.tile.remove();

                                    control.toBePlaced.add(build);

                                    Time.run(build.dst(ccore) / unitsPerTick + coreDelay, () -> {
                                        if(build.tile.build != build){
                                            control.placeLandBuild(build);

                                            control.toBePlaced.remove(build);
                                        }
                                    });
                                }else{
                                    //when already hosting, instantly build everything. this looks bad but it's better than a desync
                                    Fx.coreBuildBlock.at(build.x, build.y, 0f, build.block);
                                    build.block.placeEffect.at(build.x, build.y, build.block.size);
                                }
                            }
                        }
                    }

                    if(anyBuilds){
                        for(var ccore : state.rules.defaultTeam.data().cores){
                            Time.run(coreDelay, () -> {
                                Fx.coreBuildShockwave.at(ccore.x, ccore.y, buildRadius);
                            });
                        }
                    }
                }
            }
        });
    }
}

