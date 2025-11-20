package mindustry.core;

import arc.*;
import arc.util.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Saves.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.io.SaveIO.*;
import mindustry.maps.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.world.*;

import static arc.Core.*;
import static mindustry.Vars.*;

/**
 * Handles sector loading logic, including new sector creation, save loading, and sector restoration.
 * Extracted from Control to separate sector loading concerns.
 */
public class SectorLoader{
    private final Control control;

    public SectorLoader(Control control){
        this.control = control;
    }

    /**
     * Main entry point for loading a sector.
     * @param sector The sector to load
     */
    public void playSector(Sector sector){
        playSector(sector, sector);
    }

    /**
     * Loads a sector with an origin sector.
     * @param origin The origin sector (can be null)
     * @param sector The sector to load
     */
    public void playSector(@Nullable Sector origin, Sector sector){
        playSector(origin, sector, new WorldReloader());
    }

    /**
     * Internal method that handles the actual sector loading logic.
     * @param origin The origin sector (can be null)
     * @param sector The sector to load
     * @param reloader World reloader for handling player state
     */
    void playSector(@Nullable Sector origin, Sector sector, WorldReloader reloader){
        ui.loadAnd(() -> {
            if(control.saves.getCurrent() != null && state.isGame()){
                control.saves.getCurrent().save();
                control.saves.resetSave();
            }

            if(sector.preset != null){
                sector.preset.quietUnlock();
            }

            ui.planet.hide();
            SaveSlot slot = sector.save;
            sector.planet.setLastSector(sector);
            if(!shouldCreateNewSector(slot, sector)){

                try{
                    boolean hadNoCore = loadSectorSave(slot, sector, reloader);

                    //if there is no base, simulate a new game and place the right loadout at the spawn position
                    if(state.rules.defaultTeam.cores().isEmpty() || hadNoCore){

                        if(sector.planet.clearSectorOnLose || sector.info.hasWorldProcessor){
                            playNewSector(origin, sector, reloader);
                        }else{
                            restoreSectorWithDamage(origin, sector, slot, reloader);
                        }
                    }else{
                        state.set(State.playing);
                        reloader.end();
                    }

                }catch(SaveException e){
                    handleSaveException(e, origin, sector, slot);
                }
                ui.planet.hide();
            }else{
                playNewSector(origin, sector, reloader);
            }
        });
    }

    /**
     * Determines if a new sector should be created (as opposed to loading an existing save).
     * @param slot The save slot
     * @param sector The sector
     * @return true if a new sector should be created
     */
    private boolean shouldCreateNewSector(SaveSlot slot, Sector sector){
        // Returns true if we should create a new sector (opposite of loading existing save)
        return slot == null || clearSectors || !((!(sector.planet.clearSectorOnLose || sector.info.hasWorldProcessor) || sector.info.hasCore));
    }

    /**
     * Loads a sector save file.
     * @param slot The save slot to load
     * @param sector The sector
     * @param reloader World reloader
     * @return true if the sector had no core before loading
     */
    private boolean loadSectorSave(SaveSlot slot, Sector sector, WorldReloader reloader){
        boolean hadNoCore = !sector.info.hasCore;
        reloader.begin();
        //pass in a sector context to make absolutely sure the correct sector is written; it may differ from what's in the meta due to remapping.
        slot.load(world.makeSectorContext(sector));
        slot.setAutosave(true);
        state.rules.sector = sector;
        state.rules.cloudColor = sector.planet.landCloudColor;
        return hadNoCore;
    }

    /**
     * Handles save loading exceptions.
     * @param e The save exception
     * @param origin The origin sector
     * @param sector The sector
     * @param slot The save slot
     */
    private void handleSaveException(SaveException e, @Nullable Sector origin, Sector sector, SaveSlot slot){
        Log.err(e);
        sector.save = null;
        Time.runTask(10f, () -> ui.showErrorMessage("@save.corrupted"));
        slot.delete();
        playSector(origin, sector);
    }

    /**
     * Restores a sector with damage applied (for when player lost the sector).
     * @param origin The origin sector
     * @param sector The sector to restore
     * @param slot The save slot
     * @param reloader World reloader
     */
    private void restoreSectorWithDamage(@Nullable Sector origin, Sector sector, SaveSlot slot, WorldReloader reloader){
        //no spawn set -> delete the sector save
        if(sector.info.spawnPosition == 0){
            //delete old save
            sector.save = null;
            slot.delete();
            //play again
            playSector(origin, sector, reloader);
            return;
        }

        Tile spawn = placeSpawnCore(sector);
        
        //add extra damage.
        SectorDamage.apply(1f);

        resetGameState(sector);
        restoreEnemyBase();

        //kill all units, since they should be dead anyway
        Groups.unit.clear();
        Groups.fire.clear();
        Groups.puddle.clear();

        //reset to 0, so replaced cores don't count
        state.rules.defaultTeam.data().unitCap = 0;
        Schematics.placeLaunchLoadout(spawn.x, spawn.y);

        //set up camera/player locations
        player.set(spawn.x * tilesize, spawn.y * tilesize);
        camera.position.set(player);

        Events.fire(new SectorLaunchEvent(sector));
        Events.fire(Trigger.newGame);

        state.set(State.playing);
        reloader.end();
    }

    /**
     * Places the spawn core for a sector.
     * @param sector The sector
     * @return The tile where the core was placed
     */
    private Tile placeSpawnCore(Sector sector){
        //set spawn for sector damage to use
        Tile spawn = world.tile(sector.info.spawnPosition);
        spawn.setBlock(sector.planet.defaultCore, state.rules.defaultTeam);
        return spawn;
    }

    /**
     * Resets game state for a sector (wave, wave time, etc.).
     * @param sector The sector
     */
    private void resetGameState(Sector sector){
        //reset wave so things are more fair
        state.wave = 1;
        //set up default wave time
        state.wavetime = state.rules.initialWaveSpacing <= 0f ? (state.rules.waveSpacing * (sector.preset == null ? 2f : sector.preset.startWaveTimeMultiplier)) : state.rules.initialWaveSpacing;
        state.wavetime *= sector.planet.campaignRules.difficulty.waveTimeMultiplier;
        //reset captured state
        sector.info.wasCaptured = false;

        if(state.rules.sector.planet.allowWaves){
            //re-enable waves
            state.rules.waves = true;
            //reset win wave??
            state.rules.winWave = state.rules.attackMode ? -1 : sector.preset != null && sector.preset.captureWave > 0 ? sector.preset.captureWave : state.rules.winWave > state.wave ? state.rules.winWave : 30;
        }
    }

    /**
     * Restores the enemy base in attack mode.
     */
    private void restoreEnemyBase(){
        //if there's still an enemy base left, fix it
        if(state.rules.attackMode){
            //replace all broken blocks
            for(var plan : state.rules.waveTeam.data().plans){
                Tile tile = world.tile(plan.x, plan.y);
                if(tile != null){
                    tile.setBlock(plan.block, state.rules.waveTeam, plan.rotation);
                    if(plan.config != null && tile.build != null){
                        tile.build.configureAny(plan.config);
                    }
                }
            }
            state.rules.waveTeam.data().plans.clear();
        }
    }

    /**
     * Initializes a new sector from scratch.
     * @param origin The origin sector
     * @param sector The sector to initialize
     * @param reloader World reloader
     */
    public void playNewSector(@Nullable Sector origin, Sector sector, WorldReloader reloader){
        reloader.begin();
        world.loadSector(sector);
        state.rules.sector = sector;
        //assign origin when launching
        sector.info.origin = origin;
        sector.info.destination = origin;
        logic.play();
        control.saves.saveSector(sector);
        Events.fire(new SectorLaunchEvent(sector));
        Events.fire(Trigger.newGame);
        reloader.end();
        state.set(State.playing);
    }
}

