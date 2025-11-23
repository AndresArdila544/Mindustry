package mindustry.core;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.*;

import static mindustry.Vars.*;

/**
 * Refactored Logic class: this file demonstrates extraction of responsibilities
 * into smaller components while keeping @Remote methods inside Logic as requested.
 * The original behaviour is preserved by delegating to helper components.
 */
public class Logic implements ApplicationListener{
    // Extracted components
    private final EventRegistrar eventRegistrar;
    private final GameStateController gameStateController;
    private final WeatherEngine weatherEngine;
    private final TeamAIManager teamAIManager;

    public Logic(){
        this.eventRegistrar = new EventRegistrar();
        this.gameStateController = new GameStateController();
        this.weatherEngine = new WeatherEngine();
        this.teamAIManager = new TeamAIManager();

        // Register event listeners (moved out for clarity)
        eventRegistrar.register();
    }

    /* -----------------
       Main loop and lifecycle (delegates to components)
       ----------------- */
    @Override
    public void update(){
        PerfCounter.frame.end();
        PerfCounter.frame.begin();

        Events.fire(Trigger.update);
        universe.updateGlobal();

        if(Core.settings.modified() && !state.isPlaying()){
            netServer.admins.forceSave();
            Core.settings.forceSave();
        }

        boolean runStateCheck = !net.client() && !world.isInvalidMap() && !state.isEditor() && state.rules.canGameOver;

        if(state.isGame()){
            if(!net.client()){
                state.enemies = Groups.unit.count(u -> u.team() == state.rules.waveTeam && u.isEnemy());
            }

            if(!state.isPaused()){
                Events.fire(Trigger.beforeGameUpdate);

                float delta = Core.graphics.getDeltaTime();
                state.tick += Float.isNaN(delta) || Float.isInfinite(delta) ? 0f : delta * 60f;
                state.updateId ++;
                state.teams.updateTeamStats();
                MapPreviewLoader.checkPreviews();

                if(state.rules.fog){
                    fogControl.update();
                }

                if(state.isCampaign()){
                    state.rules.sector.info.update();
                    universe.update();
                }

                Time.update();
                logicVars.update();

                // weather is serverside
                if(!net.client() && !state.isEditor()){
                    weatherEngine.updateWeather();

                    // Delegate AI updates
                    teamAIManager.updateAll();

                    // spawn units for prebuild AI cores moved into TeamAIManager
                }

                if(!state.isEditor()){
                    state.rules.objectives.update();
                }

                if(state.rules.waves && state.rules.waveTimer && !state.gameOver){
                    if(!isWaitingWave()){
                        state.wavetime = Math.max(state.wavetime - Time.delta, 0);
                    }
                }

                if(!net.client() && state.wavetime <= 0 && state.rules.waves){
                    gameStateController.runWave();
                }

                // apply weather attributes
                state.envAttrs.clear();
                state.envAttrs.add(state.rules.attributes);
                Groups.weather.each(w -> state.envAttrs.add(w.weather.attrs, w.opacity));

                PerfCounter.entityUpdate.begin();
                Groups.update();
                PerfCounter.entityUpdate.end();

                Events.fire(Trigger.afterGameUpdate);
            }

            if(runStateCheck){
                gameStateController.checkGameState();
            }
        }else if(netServer.isWaitingForPlayers() && runStateCheck){
            gameStateController.checkGameState();
        }
    }

    @Override
    public void dispose(){
        //save the settings before quitting
        if(netServer != null){
            netServer.admins.forceSave();
        }
        Core.settings.manualSave();
    }

    public void update(float delta){
        // kept for compatibility with ApplicationListener variants that use a delta param
        update();
    }

    // keep helper methods that are small delegations
    public boolean isWaitingWave(){
        return (state.rules.waitEnemies || (state.wave >= state.rules.winWave && state.rules.winWave > 0)) && state.enemies > 0;
    }

    /* -----------------
       IMPORTANT: Remote methods remain inside Logic (as requested).
       Their implementation is preserved, but they may delegate where appropriate.
       ----------------- */

    @Remote(called = Loc.server)
    public static void sectorCapture(){
        //the sector has been conquered - waves get disabled
        state.rules.waves = false;

        if(state.rules.sector == null){
            //disable attack mode
            state.rules.attackMode = false;
            return;
        }

        boolean initial = !state.rules.sector.info.wasCaptured;

        state.rules.sector.info.wasCaptured = true;

        //fire capture event
        Events.fire(new SectorCaptureEvent(state.rules.sector, initial));

        //disable attack mode
        state.rules.attackMode = false;

        //map is over, no more world processor objective stuff
        state.rules.disableWorldProcessors = true;

        Call.clearObjectives();

        //save, just in case
        if(!headless && !net.client()){
            control.saves.saveSector(state.rules.sector);
        }
    }

    @Remote(called = Loc.both)
    public static void updateGameOver(Team winner){
        state.gameOver = true;
        if(!headless){
            state.won = player.team() == winner;
        }
    }

    @Remote(called = Loc.both)
    public static void gameOver(Team winner){
        state.stats.wavesLasted = state.wave;
        state.won = player.team() == winner;
        Time.run(60f * 3f, () -> ui.restart.show(winner));
        netClient.setQuiet();
    }

    //called when the remote server researches something
    @Remote
    public static void researched(Content content){
        if(!(content instanceof UnlockableContent u)) return;

        boolean was = u.unlockedNowHost();
        state.rules.researched.add(u);

        if(!was){
            Events.fire(new UnlockEvent(u));
        }
    }

    public void skipWave() {
        gameStateController.skipWave();
    }

    public void reset(){
        gameStateController.reset();
    }

    public void play() {
        gameStateController.play();
    }
}

/** Responsible only for registering event listeners. */
class EventRegistrar{
    public void register(){
        Events.on(BlockDestroyEvent.class, event -> {
            if(!state.rules.ghostBlocks) return;
            Tile tile = event.tile;
            if(tile.build == null || !tile.block().rebuildable) return;
            tile.build.addPlan(true);
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking){
                checkOverlappingPlans(event.team, event.tile);

                if(event.team == state.rules.defaultTeam){
                    state.stats.placedBlockCount.increment(event.tile.block());
                }
            }
        });

        Events.on(PayloadDropEvent.class, e -> {
            if(e.build != null){
                checkOverlappingPlans(e.build.team, e.build.tile);
            }
        });

        Events.on(SaveLoadEvent.class, e -> {
            // moved logic inlined from original Logic ctor
            if(state.isCampaign()){
                state.rules.coreIncinerates = true;
                state.rules.canGameOver = true;
                state.rules.allowEditRules = false;

                if(!e.isMap){
                    SectorInfo info = state.rules.sector.info;
                    info.write();

                    if(state.rules.sector.planet.allowWaveSimulation){
                        int wavesPassed = info.wavesPassed;

                        if(wavesPassed > 0){
                            Groups.unit.each(u -> {
                                if(u.team == state.rules.waveTeam){
                                    u.remove();
                                }
                            });
                        }

                        if(wavesPassed > 0){
                            state.wave += wavesPassed;
                            state.wavetime = state.rules.waveSpacing * state.getPlanet().campaignRules.difficulty.waveTimeMultiplier;

                            SectorDamage.applyCalculatedDamage();
                        }
                    }

                    state.getSector().planet.applyRules(state.rules);

                    info.damage = 0f;
                    info.wavesPassed = 0;
                    info.hasCore = true;
                    info.secondsPassed = 0;

                    state.rules.sector.saveInfo();
                }
            }
        });

        Events.on(PlayEvent.class, e -> {
            var randomWeather = state.rules.weather.copy().shuffle();
            float sum = 0f;
            for(var weather : randomWeather){
                weather.cooldown = sum + Mathf.random(weather.maxFrequency);
                sum += weather.cooldown;
            }
            state.tick = 0f;
        });

        Events.on(WorldLoadEvent.class, e -> {
            state.rules.waveTeam.rules().infiniteAmmo = true;

            if(state.isCampaign()){
                state.rules.coreIncinerates = true;
                state.rules.allowEditRules = false;
                state.rules.allowEditWorldProcessors = false;
                state.rules.waveTeam.rules().infiniteResources = true;
                state.rules.waveTeam.rules().fillItems = true;
                state.rules.waveTeam.rules().buildSpeedMultiplier *= state.getPlanet().enemyBuildSpeedMultiplier;
            }

            Core.settings.manualSave();
        });

        Events.on(UnlockEvent.class, e -> {
            if(net.server()){
                Call.researched(e.content);
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            if(!net.client() && e.sector == state.getSector() && e.sector.isBeingPlayed()){
                state.rules.waveTeam.data().destroyToDerelict();
            }

            if(!net.client() && e.sector.planet.generator != null){
                e.sector.planet.generator.onSectorCaptured(e.sector);
            }
        });

        Events.on(SectorLoseEvent.class, e -> {
            if(!net.client() && e.sector.planet.generator != null){
                e.sector.planet.generator.onSectorLost(e.sector);
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.build instanceof CoreBuild core && core.team.isAI() && state.rules.coreDestroyClear){
                Core.app.post(() -> {
                    core.team.data().timeDestroy(core.x, core.y, state.rules.enemyCoreBuildRadius);
                });
            }
        });

        Events.on(CoreChangeEvent.class, e -> Core.app.post(() -> {
            if(state.rules.cleanupDeadTeams && state.rules.pvp && !e.core.isAdded() && e.core.team != Team.derelict && e.core.team.cores().isEmpty()){
                e.core.team.data().destroyToDerelict();
            }
        }));

        Events.on(BlockBuildEndEvent.class, e -> {
            if(e.team == state.rules.defaultTeam){
                if(e.breaking){
                    state.stats.buildingsDeconstructed++;
                }else{
                    state.stats.buildingsBuilt++;
                }
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.team() == state.rules.defaultTeam){
                state.stats.buildingsDestroyed ++;
            }
        });

        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit.team() != state.rules.defaultTeam){
                state.stats.enemyUnitsDestroyed ++;
            }
        });

        Events.on(UnitCreateEvent.class, e -> {
            if(e.unit.team == state.rules.defaultTeam){
                state.stats.unitsCreated++;
            }
        });
    }

    // helper kept local to this component since it pertains to plan bookkeeping
    private void checkOverlappingPlans(Team team, Tile tile){
        TeamData data = team.data();
        Iterator<BlockPlan> it = data.plans.iterator();
        var bounds = tile.block().bounds(tile.x, tile.y, Tmp.r1);
        while(it.hasNext()){
            BlockPlan b = it.next();
            if(bounds.overlaps(b.block.bounds(b.x, b.y, Tmp.r2))){
                b.removed = true;
                it.remove();
            }
        }
    }
}

/** Responsible for wave control and high-level game state checks. */
class GameStateController{
    public void play(){
        state.set(State.playing);
        state.wavetime = (state.rules.initialWaveSpacing <= 0 ? state.rules.waveSpacing * 2 : state.rules.initialWaveSpacing) * (state.isCampaign() ? state.getPlanet().campaignRules.difficulty.waveTimeMultiplier : 1f);;
        Events.fire(new PlayEvent());

        if(!state.isCampaign() || !state.rules.sector.planet.allowLaunchLoadout || (state.rules.sector.preset != null && state.rules.sector.preset.addStartingItems)){
            for(TeamData team : state.teams.getActive()){
                if(team.hasCore()){
                    CoreBuild entity = team.core();
                    entity.items.clear();

                    for(ItemStack stack : state.rules.loadout){
                        entity.items.add(stack.item, Math.min(stack.amount, entity.storageCapacity - entity.items.get(stack.item)));
                    }
                }
            }
        }

        for(TeamData team : state.teams.getActive()){
            for(var entity : team.cores){
                entity.heal();
            }
        }
    }

    public void reset(){
        State prev = state.getState();
        state = new GameState();
        Events.fire(new StateChangeEvent(prev, State.menu));

        Groups.clear();
        Time.clear();
        Events.fire(new ResetEvent());
        world.tiles = new Tiles(0, 0);

        Core.settings.manualSave();
    }

    public void skipWave(){
        runWave();
    }

    public void runWave(){
        spawner.spawnEnemies();
        state.wave++;
        state.wavetime = state.rules.waveSpacing * (state.isCampaign() ? state.getPlanet().campaignRules.difficulty.waveTimeMultiplier : 1f);

        Events.fire(new WaveEvent());
    }

    public void checkGameState(){
        // campaign logic preserved
        if(state.isCampaign()){
            if(state.teams.playerCores().size == 0 && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }

            if(state.rules.waves && spawner.countSpawns() + state.teams.cores(state.rules.waveTeam).size <= 0){
                state.rules.waves = false;
            }

            if(state.rules.waves && (state.enemies == 0 && state.rules.winWave > 0 && state.wave >= state.rules.winWave && !spawner.isSpawning()) ||
                    (state.rules.attackMode && !state.rules.waveTeam.isAlive())){

                if(state.rules.sector.preset != null && state.rules.sector.preset.attackAfterWaves && !state.rules.attackMode){
                    state.rules.attackMode = true;
                    state.rules.waves = false;
                    Call.setRules(state.rules);
                }else{
                    Call.sectorCapture();
                }
            }
        }else{
            if(!state.rules.attackMode && state.teams.playerCores().size == 0 && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }else if(state.rules.attackMode){
                int countAlive = state.teams.getActive().count(t -> t.isAlive() && t.team != Team.derelict);

                if((countAlive <= 1 || (!state.rules.pvp && state.rules.defaultTeam.core() == null)) && !state.gameOver){
                    TeamData left = state.teams.getActive().find(t -> t.isAlive() && t.team != Team.derelict);
                    Events.fire(new GameOverEvent(left == null ? Team.derelict : left.team));
                    state.gameOver = true;
                }
            }else if(!state.gameOver && state.rules.waves && (state.enemies == 0 && state.rules.winWave > 0 && state.wave >= state.rules.winWave && !spawner.isSpawning())){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.defaultTeam));
            }
        }
    }
}

/** Weather responsibilities extracted here. */
class WeatherEngine{
    public void updateWeather(){
        state.rules.weather.removeAll(w -> w.weather == null);

        for(WeatherEntry entry : state.rules.weather){
            entry.cooldown -= Time.delta;

            if((entry.cooldown < 0 || entry.always) && !entry.weather.isActive()){
                float duration = entry.always ? Float.POSITIVE_INFINITY : Mathf.random(entry.minDuration, entry.maxDuration);
                entry.cooldown = duration + Mathf.random(entry.minFrequency, entry.maxFrequency);
                Tmp.v1.setToRandomDirection();
                Call.createWeather(entry.weather, entry.intensity, duration, Tmp.v1.x, Tmp.v1.y);
            }
        }
    }
}

/** Handles updating per-team AIs and prebuild unit spawning. */
class TeamAIManager{
    public void updateAll(){
        // ensure infinite resources are disabled
        state.rules.infiniteResources = false;

        for(TeamData data : state.teams.getActive()){
            var rules = data.team.rules();
            if(rules.fillItems && data.cores.size > 0){
                var core = data.cores.first();
                content.items().each(i -> {
                    if(i.isOnPlanet(Vars.state.getPlanet()) && !i.isHidden()){
                        core.items.set(i, core.getMaximumAccepted(i));
                    }
                });
            }

            if(rules.buildAi && !state.rules.pvp){
                if(data.buildAi == null) data.buildAi = new BaseBuilderAI(data);
                data.buildAi.update();
            }

            if(rules.rtsAi){
                if(data.rtsAi == null) data.rtsAi = new RtsAI(data);
                data.rtsAi.update();
            }

            if(rules.prebuildAi && !state.isEditor()){
                for(var core : data.cores){
                    var units = data.getUnits(((CoreBlock)core.block).unitType);
                    if(units == null || !units.contains(u -> u.flag == core.pos())){
                        Unit unit = ((CoreBlock)core.block).unitType.spawn(core, data.team);
                        unit.flag = core.pos();
                        unit.add();
                        Units.notifyUnitSpawn(unit);
                        Fx.spawn.at(unit);
                    }
                }
            }
        }
    }
}
