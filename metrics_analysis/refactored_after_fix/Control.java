package mindustry.core;

import arc.*;
import arc.assets.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Objectives.*;
import mindustry.game.Saves.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.io.SaveIO.*;
import mindustry.maps.Map;
import mindustry.maps.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.*;

/**
 * Control module.
 * Handles all input, saving and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control implements ApplicationListener, Loadable{
    public Saves saves;
    public SoundControl sound;
    public InputHandler input;
    public AttackIndicators indicators;
    public GameStateManager gameStateManager = new GameStateManager();
    public SectorLoader sectorLoader = new SectorLoader(this);

    public ControlEventHandler eventHandler = new ControlEventHandler(this);

    private Interval timer = new Interval(2);
    private boolean hiscore = false;
    Seq<Building> toBePlaced = new Seq<>(false);

    public Control(){
        saves = new Saves();
        sound = new SoundControl();
        indicators = new AttackIndicators();

        eventHandler.setupBuildDamageListener();
        eventHandler.setupClientLoadListener();
        eventHandler.setupStateChangeListener();

        Events.on(PlayEvent.class, event -> {
            player.team(netServer.assignTeam(player));
            player.add();

            state.set(State.playing);
        });

        eventHandler.setupWorldLoadListeners();

        Events.on(SaveLoadEvent.class, event -> {
            input.checkUnit();
        });

        Events.on(ResetEvent.class, event -> {
            player.reset();
            toBePlaced.clear();
            indicators.clear();

            hiscore = false;
            saves.resetSave();
        });

        Events.on(WaveEvent.class, event -> {
            if(state.map.getHightScore() < state.wave){
                hiscore = true;
                state.map.setHighScore(state.wave);
            }

            Sounds.wave.play();
        });

        eventHandler.setupGameOverListeners();
        eventHandler.setupSectorListeners();
        eventHandler.setupNewGameListener();

        Events.on(SaveWriteEvent.class, e -> forcePlaceAll());
        Events.on(HostEvent.class, e -> forcePlaceAll());
        Events.on(HostEvent.class, e -> {
            state.set(State.playing);
        });
    }

    private void forcePlaceAll(){
        //force set buildings when a save is done or map is hosted, to prevent desyncs
        for(var build : toBePlaced){
            placeLandBuild(build);
        }

        toBePlaced.clear();
    }

    void placeLandBuild(Building build){
        build.tile.setBlock(build.block, build.team, build.rotation, () -> build);
        build.dropped();

        Fx.coreBuildBlock.at(build.x, build.y, 0f, build.block);
        build.block.placeEffect.at(build.x, build.y, build.block.size);
    }

    @Override
    public void loadAsync(){
        Draw.scl = 1f / Core.atlas.find("scale_marker").width;

        Core.input.setCatch(KeyCode.back, true);

        Core.settings.defaults(
        "ip", "localhost",
        "color-0", playerColors[8].rgba(),
        "name", "",
        "lastBuild", 0
        );

        createPlayer();

        saves.load();
    }

    /** Automatically unlocks things with no requirements and no locked parents. */
    public void checkAutoUnlocks(){
        if(net.client()) return;

        for(TechNode node : TechTree.all){
            if(!node.content.unlocked() && (node.parent == null || node.parent.content.unlocked()) && node.requirements.length == 0 && !node.objectives.contains(o -> !o.complete())){
                node.content.unlock();
            }
        }
    }

    void createPlayer(){
        player = Player.create();
        player.name = Core.settings.getString("name");

        String locale = Core.settings.getString("locale");
        if(locale.equals("default")){
            locale = Locale.getDefault().toString();
        }
        player.locale = locale;

        player.color.set(Core.settings.getInt("color-0"));

        if(mobile){
            input = new MobileInput();
        }else{
            input = new DesktopInput();
        }

        if(state.isGame()){
            player.add();
        }

        Events.on(ClientLoadEvent.class, e -> input.add());
    }

    public void setInput(InputHandler newInput){
        Block block = input.block;
        boolean added = Core.input.getInputProcessors().contains(input);
        input.remove();
        this.input = newInput;
        newInput.block = block;
        if(added){
            newInput.add();
        }
    }

    public void playMap(Map map, Rules rules){
        playMap(map, rules, false);
    }

    public void playMap(Map map, Rules rules, boolean playtest){
        ui.loadAnd(() -> {
            logic.reset();
            world.loadMap(map, rules);
            state.rules = rules;
            if(playtest) state.playtestingMap = map;
            state.rules.sector = null;
            state.rules.editor = false;
            logic.play();
            if(settings.getBool("savecreate") && !world.isInvalidMap() && !playtest){
                control.saves.addSave(map.name() + " " + new SimpleDateFormat("MMM dd h:mm", Locale.getDefault()).format(new Date()));
            }
            Events.fire(Trigger.newGame);

            //booted out of map, resume editing
            if(world.isInvalidMap() && playtest){
                Dialog current = scene.getDialog();
                ui.editor.resumeAfterPlaytest(map);
                if(current != null){
                    current.update(current::toFront);
                }
            }
        });
    }

    public void playSector(Sector sector){
        sectorLoader.playSector(sector);
    }

    public void playSector(@Nullable Sector origin, Sector sector){
        sectorLoader.playSector(origin, sector);
    }

    public void playNewSector(@Nullable Sector origin, Sector sector, WorldReloader reloader){
        sectorLoader.playNewSector(origin, sector, reloader);
    }

    public boolean isHighScore(){
        return hiscore;
    }

    @Override
    public void dispose(){
        //try to save when exiting
        if(saves != null && saves.getCurrent() != null && saves.getCurrent().isAutosave() && !net.client() && !state.isMenu() && !state.gameOver){
            try{
                SaveIO.save(control.saves.getCurrent().file);
                settings.forceSave();
                Log.info("Saved on exit.");
            }catch(Throwable t){
                Log.err(t);
            }
        }

        for(Music music : assets.getAll(Music.class, new Seq<>())){
            music.stop();
        }

        net.dispose();
    }

    @Override
    public void pause(){
        gameStateManager.onApplicationPause();
    }

    @Override
    public void resume(){
        gameStateManager.onApplicationResume();
    }

    @Override
    public void init(){
        platform.updateRPC();

        //display UI scale changed dialog
        if(Core.settings.getBool("uiscalechanged", false)){
            Core.app.post(() -> Core.app.post(() -> {
                BaseDialog dialog = new BaseDialog("@confirm");
                dialog.setFillParent(true);

                float[] countdown = {60 * 11};
                Runnable exit = () -> {
                    Core.settings.put("uiscale", 100);
                    Core.settings.put("uiscalechanged", false);
                    dialog.hide();
                    Core.app.exit();
                };

                dialog.cont.label(() -> {
                    if(countdown[0] <= 0){
                        exit.run();
                    }
                    return Core.bundle.format("uiscale.reset", (int)((countdown[0] -= Time.delta) / 60f));
                }).pad(10f).expand().center();

                dialog.buttons.defaults().size(200f, 60f);
                dialog.buttons.button("@uiscale.cancel", exit);

                dialog.buttons.button("@ok", () -> {
                    Core.settings.put("uiscalechanged", false);
                    dialog.hide();
                });

                dialog.show();
            }));
        }
    }

    @Override
    public void update(){
        //this happens on Android and nobody knows why
        if(assets == null) return;

        updateSaves();
        updateAssets();
        updateInput();
        updateSound();
        handleFullscreenToggle();
        validatePlayerPosition();

        if(state.isGame()){
            updateGameState();
        }else{
            updateMenuState();
        }
    }

    private void updateSaves(){
        saves.update();
    }

    private void updateAssets(){
        //update and load any requested assets
        try{
            assets.update();
        }catch(Exception ignored){
        }
    }

    private void updateInput(){
        input.updateState();
    }

    private void updateSound(){
        sound.update();
    }

    private void handleFullscreenToggle(){
        if(Core.input.keyTap(Binding.fullscreen)){
            boolean full = settings.getBool("fullscreen");
            if(full){
                graphics.setWindowedMode(graphics.getWidth(), graphics.getHeight());
            }else{
                graphics.setFullscreen();
            }
            settings.put("fullscreen", !full);
        }
    }

    private void validatePlayerPosition(){
        if(Float.isNaN(Vars.player.x) || Float.isNaN(Vars.player.y)){
            player.set(0, 0);
            if(!player.dead()) player.unit().kill();
        }
        if(Float.isNaN(camera.position.x)) camera.position.x = world.unitWidth()/2f;
        if(Float.isNaN(camera.position.y)) camera.position.y = world.unitHeight()/2f;
    }

    private void updateGameState(){
        input.update();
        if(!state.isPaused()){
            indicators.update();
        }

        //auto-update rpc every 5 seconds
        if(timer.get(0, 60 * 5)){
            platform.updateRPC();
        }

        //unlock core items
        var core = state.rules.defaultTeam.core();
        if(!net.client() && core != null && state.isCampaign()){
            core.items.each((i, a) -> i.unlock());
        }

        gameStateManager.updatePauseState();
        gameStateManager.handlePauseInput();
        gameStateManager.handleMenuInput();

        if(!mobile && Core.input.keyTap(Binding.screenshot) && !scene.hasField() && !scene.hasKeyboard()){
            renderer.takeMapScreenshot();
        }
    }

    private void updateMenuState(){
        //this runs in the menu
        if(!state.isPaused()){
            Time.update();
        }

        if(!scene.hasDialog() && !scene.root.getChildren().isEmpty() && !(scene.root.getChildren().peek() instanceof Dialog) && Core.input.keyTap(KeyCode.back)){
            platform.hide();
        }
    }
}
