package mindustry.ui.fragments;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.gen.Tex.*;

/**
 * Handles building all HUD UI components.
 * Extracted from HudFragment to separate UI building concerns.
 */
public class HudUIBuilder{
    private static final float dsize = 65f, pauseHeight = 36f;

    private final HudFragment hudFragment;
    private final Seq<Block> blocksOut = new Seq<>();

    public HudUIBuilder(HudFragment hudFragment){
        this.hudFragment = hudFragment;
    }

    /**
     * Main entry point for building all HUD components.
     * @param parent The parent group to add UI components to
     */
    public void build(Group parent){
        setupEventListeners();
        buildPausedTable(parent);
        buildWaitingTable(parent);
        buildMinimap(parent);

        ui.hints.build(parent);
        buildGutterAreas(parent);

        buildCoreInfo(parent);
        buildSpawnerWarning(parent);
        buildSavingIndicator(parent);

        //TODO DEBUG: rate table
        if(false)
            parent.fill(t -> {
                t.bottom().left();
                t.table(Styles.black6, c -> {
                    Bits used = new Bits(content.items().size);

                    Runnable rebuild = () -> {
                        c.clearChildren();

                        for(Item item : content.items()){
                            if(state.rules.sector != null && state.rules.sector.info.getExport(item) >= 1){
                                c.image(item.uiIcon);
                                c.label(() -> (int)state.rules.sector.info.getExport(item) + " /s").color(Color.lightGray);
                                c.row();
                            }
                        }
                    };

                    c.update(() -> {
                        boolean wrong = false;
                        for(Item item : content.items()){
                            boolean has = state.rules.sector != null && state.rules.sector.info.getExport(item) >= 1;
                            if(used.get(item.id) != has){
                                used.set(item.id, has);
                                wrong = true;
                            }
                        }
                        if(wrong){
                            rebuild.run();
                        }
                    });
                }).visible(() -> state.isCampaign() && content.items().contains(i -> state.rules.sector != null && state.rules.sector.info.getExport(i) > 0));
            });

        hudFragment.blockfrag.build(parent);
    }

    /**
     * Sets up event listeners for HUD-related events.
     */
    public void setupEventListeners(){
        //warn about guardian/boss waves
        Events.on(WaveEvent.class, e -> {
            int max = 10;
            int winWave = state.rules.winWave > 0 ? state.rules.winWave : Integer.MAX_VALUE;
            outer:
            for(int i = state.wave - 1; i <= Math.min(state.wave + max, winWave - 2); i++){
                for(SpawnGroup group : state.rules.spawns){
                    if(group.effect == StatusEffects.boss && group.getSpawned(i) > 0){
                        int diff = (i + 2) - state.wave;

                        //increments at which to warn about incoming guardian
                        if(diff == 1 || diff == 2 || diff == 5 || diff == 10){
                            hudFragment.showToast(Icon.warning, group.type.emoji() + " " + Core.bundle.format("wave.guardianwarn" + (diff == 1 ? ".one" : ""), diff));
                        }

                        break outer;
                    }
                }
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            if(e.sector.isBeingPlayed()){
                ui.announce("@sector.capture.current", 5f);
            }else{
                hudFragment.showToast(Core.bundle.format("sector.capture", e.sector.name()));
            }
        });

        Events.on(SectorLoseEvent.class, e -> {
            hudFragment.showToast(Icon.warning, Core.bundle.format("sector.lost", e.sector.name()));
        });

        Events.on(SectorInvasionEvent.class, e -> {
            hudFragment.showToast(Icon.warning, Core.bundle.format("sector.attacked", e.sector.name()));
        });

        Events.on(ResetEvent.class, e -> {
            hudFragment.coreItems.resetUsed();
            hudFragment.coreItems.clear();
        });
    }

    /**
     * Builds the paused table overlay.
     */
    public void buildPausedTable(Group parent){
        //paused table
        parent.fill(t -> {
            t.name = "paused";
            t.top().visible(() -> state.isPaused() && hudFragment.shown && !netServer.isWaitingForPlayers()).touchable = Touchable.disabled;
            t.table(Styles.black6, top -> top.label(() -> state.gameOver && state.isCampaign() ? "@sector.curlost" : "@paused")
                .style(Styles.outlineLabel).pad(8f)).height(pauseHeight).growX();
            //.padLeft(dsize * 5 + 4f) to prevent alpha overlap on left
        });
    }

    /**
     * Builds the "waiting for players" table overlay.
     */
    public void buildWaitingTable(Group parent){
        //"waiting for players"
        parent.fill(t -> {
            t.name = "waiting";
            t.visible(() -> netServer.isWaitingForPlayers() && state.isPaused() && hudFragment.shown).touchable = Touchable.disabled;
            t.table(Styles.black6, top -> top.add("@waiting.players").style(Styles.outlineLabel).pad(18f));
        });
    }

    /**
     * Builds the minimap and position display.
     */
    public void buildMinimap(Group parent){
        //minimap + position
        parent.fill(t -> {
            t.name = "minimap/position";
            t.visible(() -> Core.settings.getBool("minimap") && hudFragment.shown);
            //minimap
            t.add(new Minimap()).name("minimap");
            t.row();
            //position
            t.label(() ->
                (Core.settings.getBool("position") ? player.tileX() + "," + player.tileY() + "\n" : "") +
                (Core.settings.getBool("mouseposition") ? "[lightgray]" + World.toTile(Core.input.mouseWorldX()) + "," + World.toTile(Core.input.mouseWorldY()) : ""))
            .visible(() -> Core.settings.getBool("position") || Core.settings.getBool("mouseposition"))
            .touchable(Touchable.disabled)
            .style(Styles.outlineLabel)
            .name("position");
            t.top().right();
        });
    }

    /**
     * Builds the spawner warning indicator.
     */
    public void buildSpawnerWarning(Group parent){
        //spawner warning
        parent.fill(t -> {
            t.name = "nearpoint";
            t.touchable = Touchable.disabled;
            t.table(Styles.black6, c -> c.add("@nearpoint")
            .update(l -> l.setColor(Tmp.c1.set(Color.white).lerp(Color.scarlet, Mathf.absin(Time.time, 10f, 1f))))
            .labelAlign(Align.center, Align.center))
            .margin(6).update(u -> u.color.a = Mathf.lerpDelta(u.color.a, Mathf.num(spawner.playerNear()), 0.1f)).get().color.a = 0f;
        });
    }

    /**
     * Builds the saving indicator.
     */
    public void buildSavingIndicator(Group parent){
        //'saving' indicator
        parent.fill(t -> {
            t.name = "saving";
            t.bottom().visible(() -> control.saves.isSaving());
            t.add("@saving").style(Styles.outlineLabel);
        });
    }

    /**
     * Builds the gutter areas (mobile buttons, waves/editor tables, FPS display).
     */
    public void buildGutterAreas(Group parent){
        //menu at top left
        parent.fill(cont -> {
            cont.name = "overlaymarker";
            cont.top().left();

            if(mobile){
                //for better inset visuals
                cont.rect((x, y, w, h) -> {
                    if(Core.scene.marginTop > 0){
                        Tex.paneRight.draw(x, y, w, Core.scene.marginTop);
                    }
                }).fillX().row();

                cont.table(select -> {
                    select.name = "mobile buttons";
                    select.left();
                    select.defaults().size(dsize).left();

                    ImageButtonStyle style = Styles.cleari;

                    select.button(Icon.menu, style, ui.paused::show).name("menu");
                    hudFragment.setFlipButton(select.button(Icon.upOpen, style, hudFragment::toggleMenus).get());
                    hudFragment.getFlipButton().name = "flip";

                    select.button(Icon.paste, style, ui.schematics::show)
                    .name("schematics");

                    select.button(Icon.pause, style, () -> {
                        if(net.active()){
                            ui.listfrag.toggle();
                        }else{
                            state.set(state.isPaused() ? State.playing : State.paused);
                        }
                    }).name("pause").update(i -> {
                        if(net.active()){
                            i.getStyle().imageUp = Icon.players;
                        }else{
                            i.setDisabled(false);
                            i.getStyle().imageUp = state.isPaused() ? Icon.play : Icon.pause;
                        }
                    });

                    select.button(Icon.chat, style,() -> {
                        if(net.active() && mobile){
                            if(ui.chatfrag.shown()){
                                ui.chatfrag.hide();
                            }else{
                                ui.chatfrag.toggle();
                            }
                        }else if(state.isCampaign()){
                            ui.research.show();
                        }else{
                            ui.database.show();
                        }
                    }).name("chat").update(i -> {
                        if(net.active() && mobile){
                            i.getStyle().imageUp = Icon.chat;
                        }else if(state.isCampaign()){
                            i.getStyle().imageUp = Icon.tree;
                        }else{
                            i.getStyle().imageUp = Icon.book;
                        }
                    });

                    select.image().color(Pal.gray).width(4f).fillY();
                });

                cont.row();
                cont.image().height(4f).color(Pal.gray).fillX();
                cont.row();
            }

            cont.update(() -> {
                if(Core.input.keyTap(Binding.toggleMenus) && !ui.chatfrag.shown() && !Core.scene.hasDialog() && !Core.scene.hasField()){
                    Core.settings.getBoolOnce("ui-hidden", () -> {
                        ui.announce(Core.bundle.format("showui",  Binding.toggleMenus.value.key.toString(), 11));
                    });
                    hudFragment.toggleMenus();
                }

                if(Core.input.keyTap(Binding.skipWave) && hudFragment.statusDisplayBuilder.canSkipWave()){
                    if(net.client() && player.admin){
                        Call.adminRequest(player, AdminAction.wave, null);
                    }else{
                        logic.skipWave();
                    }
                }
            });

            Table wavesMain, editorMain;

            cont.stack(wavesMain = new Table(), editorMain = new Table(), new Element(){
                //this may seem insane, but adding an empty element of a specific height to this stack fixes layout issues on mobile.

                {
                    visible = false;
                    touchable = Touchable.disabled;
                }

                @Override
                public float getPrefHeight(){
                    return Scl.scl(120f);
                }
            }).name("waves/editor");

            wavesMain.visible(() -> hudFragment.shown && !state.isEditor());
            wavesMain.top().left().name = "waves";

            wavesMain.table(s -> {
                //wave info button with text
                s.add(hudFragment.statusDisplayBuilder.makeStatusTable()).grow().name("status");

                var rightStyle = new ImageButtonStyle(){{
                    over = buttonRightOver;
                    down = buttonRightDown;
                    up = buttonRight;
                    disabled = buttonRightDisabled;
                    imageDisabledColor = Color.clear;
                    imageUpColor = Color.white;
                }};

                //table with button to skip wave
                s.button(Icon.play, rightStyle, 30f, () -> {
                    if(net.client() && player.admin){
                        Call.adminRequest(player, AdminAction.wave, null);
                    }else{
                        logic.skipWave();
                    }
                }).growY().fillX().right().width(40f).disabled(b -> !hudFragment.statusDisplayBuilder.canSkipWave()).name("skip").get().toBack();
            }).width(dsize * 5 + 4f).name("statustable");

            wavesMain.row();

            hudFragment.statusDisplayBuilder.addInfoTable(wavesMain.table().width(dsize * 5f + 4f).left().get());

            editorMain.name = "editor";
            editorMain.table(Tex.buttonEdge4, t -> {
                t.name = "teams";


                t.top().table(teams -> {
                    teams.left();
                    for(Team team : Team.baseTeams){
                        ImageButton button = teams.button(Tex.whiteui, Styles.clearNoneTogglei, 33f, () -> Call.setPlayerTeamEditor(player, team))
                        .size(45f).margin(6f).get();
                        button.getImageCell().grow();
                        button.getStyle().imageUpColor = team.color;
                        button.update(() -> button.setChecked(player.team() == team));
                    }

                    teams.button(Icon.downOpen, Styles.emptyi, () -> Core.settings.put("editor-blocks-shown", !Core.settings.getBool("editor-blocks-shown")))
                    .size(45f).update(m -> m.getStyle().imageUp = (Core.settings.getBool("editor-blocks-shown") ? Icon.upOpen : Icon.downOpen));
                }).top().left().row();

                t.collapser(this::addBlockSelection, () -> Core.settings.getBool("editor-blocks-shown"));

            }).width(dsize * 5 + 4f).top();
            if(mobile){
                editorMain.row().spacerY(() -> {
                    if(control.input instanceof MobileInput mob && Core.settings.getBool("editor-blocks-shown")){
                        if(Core.graphics.isPortrait()) return Core.graphics.getHeight() / 2f / Scl.scl(1f);
                        if(mob.hasSchematic()) return 156f;
                        if(mob.showCancel()) return 50f;
                    }
                    return 0f;
                });
            }

            editorMain.row().add().growY();
            editorMain.visible(() -> hudFragment.shown && state.isEditor());

            //fps display
            cont.table(info -> {
                info.name = "fps/ping";
                info.touchable = Touchable.disabled;
                info.top().left().margin(4).visible(() -> Core.settings.getBool("fps") && hudFragment.shown);
                IntFormat fps = new IntFormat("fps");
                IntFormat ping = new IntFormat("ping");
                IntFormat tps = new IntFormat("tps");
                IntFormat mem = new IntFormat("memory");
                IntFormat memnative = new IntFormat("memory2");

                info.label(() -> fps.get(Core.graphics.getFramesPerSecond())).left().style(Styles.outlineLabel).name("fps");
                info.row();

                if(android){
                    info.label(() -> memnative.get((int)(Core.app.getJavaHeap() / 1024 / 1024), (int)(Core.app.getNativeHeap() / 1024 / 1024))).left().style(Styles.outlineLabel).name("memory2");
                }else{
                    info.label(() -> mem.get((int)(Core.app.getJavaHeap() / 1024 / 1024))).left().style(Styles.outlineLabel).name("memory");
                }
                info.row();

                info.label(() -> ping.get(netClient.getPing())).visible(net::client).left().style(Styles.outlineLabel).name("ping").row();
                info.label(() -> tps.get(state.serverTps == -1 ? 60 : state.serverTps)).visible(net::client).left().style(Styles.outlineLabel).name("tps").row();

            }).top().left();
        });
    }

    /**
     * Builds the core info display (core items, attack warning, boss bar, HUD text).
     */
    public void buildCoreInfo(Group parent){
        //core info
        parent.fill(t -> {
            t.top();

            if(Core.settings.getBool("macnotch") ){
                t.margin(macNotchHeight);
            }

            t.visible(() -> hudFragment.shown);

            t.name = "coreinfo";

            t.collapser(v -> v.add().height(pauseHeight), () -> state.isPaused() && !netServer.isWaitingForPlayers()).row();

            t.table(c -> {
                //core items
                c.top().collapser(hudFragment.coreItems, () -> Core.settings.getBool("coreitems") && !mobile && hudFragment.shown).fillX().row();

                float notifDuration = 240f;
                float[] coreAttackTime = {0};

                Events.run(Trigger.teamCoreDamage, () -> coreAttackTime[0] = notifDuration);

                //'core is under attack' table
                c.collapser(top -> top.background(Styles.black6).add("@coreattack").pad(8)
                .update(label -> label.color.set(Color.orange).lerp(Color.scarlet, Mathf.absin(Time.time, 2f, 1f))), true,
                () -> {
                    if(!hudFragment.shown || state.isPaused()) return false;
                    if(state.isMenu() || !player.team().data().hasCore()){
                        coreAttackTime[0] = 0f;
                        return false;
                    }

                    return (coreAttackTime[0] -= Time.delta) > 0;
                })
                .touchable(Touchable.disabled)
                .fillX().row();
            }).row();

            var bossb = new StringBuilder();
            var bossText = Core.bundle.get("guardian");
            int maxBosses = 6;

            t.table(v -> v.margin(10f)
            .add(new Bar(() -> {
                bossb.setLength(0);
                for(int i = 0; i < Math.min(state.teams.bosses.size, maxBosses); i++){
                    bossb.append(state.teams.bosses.get(i).type.emoji());
                }
                if(state.teams.bosses.size > maxBosses){
                    bossb.append("[accent]+[]");
                }
                bossb.append(" ");
                bossb.append(bossText);
                return bossb;
            }, () -> Pal.health, () -> {
                if(state.boss() == null) return 0f;
                float max = 0f, val = 0f;
                for(var boss : state.teams.bosses){
                    max += boss.maxHealth;
                    val += boss.health;
                }
                return max == 0f ? 0f : val / max;
            }).blink(Color.white).outline(new Color(0, 0, 0, 0.6f), 7f)).grow())
            .fillX().width(320f).height(60f).name("boss").visible(() -> state.rules.waves && state.boss() != null && !(mobile && Core.graphics.isPortrait())).padTop(7).row();

            t.table(Styles.black3, p -> p.margin(4).label(() -> hudFragment.getHudText()).style(Styles.outlineLabel)).touchable(Touchable.disabled).with(p -> p.visible(() -> {
                p.color.a = Mathf.lerpDelta(p.color.a, Mathf.num(hudFragment.isShowHudText()), 0.2f);
                if(state.isMenu()){
                    p.color.a = 0f;
                    hudFragment.setShowHudText(false);
                }

                return p.color.a >= 0.001f;
            }));
        });
    }

    /**
     * Adds block selection UI for the editor.
     */
    private void addBlockSelection(Table cont){
        Table blockSelection = new Table();
        var pane = new ScrollPane(blockSelection, Styles.smallPane);
        pane.setFadeScrollBars(false);
        Planet[] last = {state.rules.planet};
        pane.update(() -> {
            if(pane.hasScroll()){
                Element result = Core.scene.getHoverElement();
                if(result == null || !result.isDescendantOf(pane)){
                    Core.scene.setScrollFocus(null);
                }
            }

            if(state.rules.planet != last[0]){
                last[0] = state.rules.planet;
                rebuildBlockSelection(blockSelection, "");
            }
        });

        Table[] configTable = {null};
        Block[] lastBlock = {null};

        cont.table(search -> {
            search.image(Icon.zoom).padRight(8);
            search.field("", text -> rebuildBlockSelection(blockSelection, text)).growX()
            .name("editor/search").maxTextLength(maxNameLength).get().setMessageText("@players.search");
        }).growX().pad(-2).padLeft(6f);
        cont.row();
        cont.collapser(t -> {
            configTable[0] = t;
        }, () -> control.input.block != null && control.input.block.editorConfigurable).with(c -> c.setEnforceMinSize(true)).update(col -> {

            if(lastBlock[0] != control.input.block){
                configTable[0].clear();
                if(control.input.block != null){
                    control.input.block.buildEditorConfig(configTable[0]);
                    col.invalidateHierarchy();
                }
                lastBlock[0] = control.input.block;
            }
        }).growX().row();
        cont.add(pane).expandY().top().left();

        rebuildBlockSelection(blockSelection, "");
    }

    /**
     * Rebuilds the block selection list based on search text.
     */
    private void rebuildBlockSelection(Table blockSelection, String searchText){
        blockSelection.clear();

        blocksOut.clear();
        blocksOut.addAll(Vars.content.blocks());
        blocksOut.sort((b1, b2) -> {
            int synth = Boolean.compare(b1.synthetic(), b2.synthetic());
            if(synth != 0) return synth;
            int ore = Boolean.compare(b1 instanceof OverlayFloor && b1 != Blocks.removeOre, b2 instanceof OverlayFloor && b2 != Blocks.removeOre);
            if(ore != 0) return ore;
            return Integer.compare(b1.id, b2.id);
        });

        int i = 0;

        for(Block block : blocksOut){
            TextureRegion region = block.uiIcon;

            if(!Core.atlas.isFound(region)
            || (!block.inEditor && !(block instanceof RemoveWall) && !(block instanceof RemoveOre))
            || !block.isOnPlanet(state.rules.planet)
            || block.buildVisibility == BuildVisibility.debugOnly
            || (!searchText.isEmpty() && !block.localizedName.toLowerCase().contains(searchText.trim().replaceAll(" +", " ").toLowerCase()))
            ) continue;

            ImageButton button = new ImageButton(Tex.whiteui, Styles.clearNoneTogglei);
            button.getStyle().imageUp = new TextureRegionDrawable(region);
            button.clicked(() -> control.input.block = block);
            button.resizeImage(8 * 4f);
            button.update(() -> button.setChecked(control.input.block == block));
            blockSelection.add(button).size(48f).tooltip(block.localizedName);

            if(++i % 6 == 0){
                blockSelection.row();
            }
        }

        if(i == 0){
            blockSelection.add("@none.found").padLeft(54f).padTop(10f);
        }
    }
}

