package mindustry.ui.fragments;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;
import static mindustry.gen.Tex.*;

/**
 * Handles building the status display table for the HUD.
 * Extracted from HudFragment to separate status display concerns.
 */
public class StatusDisplayBuilder{
    private final HudFragment hudFragment;

    public StatusDisplayBuilder(HudFragment hudFragment){
        this.hudFragment = hudFragment;
    }

    /**
     * Creates the main status table with wave info, player display, and status text.
     * @return The status table
     */
    public Table makeStatusTable(){
        Table table = new Table(Tex.wavepane);

        IntFormat[] formats = createIntFormats();
        IntFormat wavef = formats[0], wavefc = formats[1], enemyf = formats[2], enemiesf = formats[3], enemycf = formats[4], enemycsf = formats[5], waitingf = formats[6];

        setupStatusTable(table);

        Cell[] lcell = {null};
        boolean[] couldSkip = {true};
        StringBuilder builder = new StringBuilder();

        lcell[0] = createStatusLabel(table, lcell, couldSkip, builder, wavef, wavefc, enemyf, enemiesf, enemycf, enemycsf, waitingf);

        table.row();

        //TODO nobody reads details anyway.
        /*
        table.clicked(() -> {
            if(state.rules.objectives.any()){
                StringBuilder text = new StringBuilder();

                boolean first = true;
                for(var obj : state.rules.objectives){
                    if(!obj.qualified()) continue;

                    String details = obj.details();
                    if(details != null){
                        if(!first) text.append('\n');
                        text.append(details);

                        first = false;
                    }
                }

                //TODO this, as said before, could be much better.
                ui.showInfo(text.toString());
            }
        });*/

        return table;
    }

    /**
     * Adds info table with payload and status effect displays.
     * @param table The table to add info to
     */
    public void addInfoTable(Table table){
        table.name = "infotable";
        table.left();

        var count = new float[]{-1};
        table.table().update(t -> {
            if(player.unit() instanceof Payloadc payload){
                if(count[0] != payload.payloadUsed()){
                    payload.contentInfo(t, 8 * 2, 275f);
                    count[0] = payload.payloadUsed();
                }
            }else{
                count[0] = -1;
                t.clear();
            }
        }).growX().visible(() -> player.unit() instanceof Payloadc p && p.payloadUsed() > 0).colspan(2);
        table.row();

        Bits statuses = new Bits();

        table.table().update(t -> {
            t.left();
            Bits applied = player.dead() ? null : player.unit().statusBits();
            if(!statuses.equals(applied)){
                t.clear();

                if(applied != null){
                    for(StatusEffect effect : content.statusEffects()){
                        if(applied.get(effect.id) && !effect.isHidden()){
                            t.image(effect.uiIcon).size(iconMed).get()
                            .addListener(new Tooltip(l -> l.label(() ->
                                player.dead() ? "" : effect.localizedName + " [lightgray]" + UI.formatTime(player.unit().getDuration(effect))).style(Styles.outlineLabel)));
                        }
                    }

                    statuses.set(applied);
                }else{
                    statuses.clear();
                }
            }
        }).left();
    }

    /**
     * Creates the status label with dynamic text updates.
     */
    private Cell createStatusLabel(Table table, Cell[] lcell, boolean[] couldSkip, StringBuilder builder, IntFormat wavef, IntFormat wavefc, IntFormat enemyf, IntFormat enemiesf, IntFormat enemycf, IntFormat enemycsf, IntFormat waitingf){
        return table.labelWrap(() -> {
            updateSkipWavePadding(table, lcell, couldSkip);
            builder.setLength(0);

            String missionOrObjectives = buildMissionOrObjectivesText(builder);
            if(missionOrObjectives != null) return builder;

            String nonWaveText = buildNonWaveText(builder, enemycf, enemycsf);
            if(nonWaveText != null) return builder;

            buildWaveAndEnemyText(builder, wavef, wavefc, enemyf, enemiesf, waitingf);
            return builder;
        }).growX().pad(8f);
    }

    /**
     * Updates padding based on skip wave button visibility.
     */
    private void updateSkipWavePadding(Table table, Cell[] lcell, boolean[] couldSkip){
        boolean can = canSkipWave();
        if(can != couldSkip[0]){
            if(canSkipWave()){
                lcell[0].padRight(8f);
            }else{
                lcell[0].padRight(-42f);
            }
            table.invalidateHierarchy();
            table.pack();
            couldSkip[0] = can;
        }
    }

    /**
     * Builds mission or objectives text.
     */
    private String buildMissionOrObjectivesText(StringBuilder builder){
        //mission overrides everything
        if(state.rules.mission != null && state.rules.mission.length() > 0){
            builder.append(state.rules.mission);
            return builder.toString();
        }

        //objectives override mission?
        if(state.rules.objectives.any()){
            boolean first = true;
            for(var obj : state.rules.objectives){
                if(!obj.qualified() || obj.hidden) continue;

                String text = obj.text();
                if(text != null && !text.isEmpty()){
                    if(!first) builder.append("\n[white]");
                    builder.append(text);
                    first = false;
                }
            }

            //TODO: display standard status when empty objective?
            if(builder.length() > 0){
                return builder.toString();
            }
        }
        return null;
    }

    /**
     * Builds text for non-wave modes.
     */
    private String buildNonWaveText(StringBuilder builder, IntFormat enemycf, IntFormat enemycsf){
        if(!state.rules.waves && state.rules.attackMode){
            int sum = Math.max(state.teams.present.sum(t -> t.team != player.team() ? t.cores.size : 0), 1);
            builder.append(sum > 1 ? enemycsf.get(sum) : enemycf.get(sum));
            return builder.toString();
        }

        if(!state.rules.waves && state.isCampaign()){
            builder.append("[lightgray]").append(Core.bundle.get("sector.curcapture"));
        }

        if(!state.rules.waves){
            return builder.toString();
        }
        return null;
    }

    /**
     * Builds wave number, enemy count, and waiting timer text.
     */
    private void buildWaveAndEnemyText(StringBuilder builder, IntFormat wavef, IntFormat wavefc, IntFormat enemyf, IntFormat enemiesf, IntFormat waitingf){
        if(state.rules.winWave > 1 && state.rules.winWave >= state.wave){
            builder.append(wavefc.get(state.wave, state.rules.winWave));
        }else{
            builder.append(wavef.get(state.wave));
        }
        builder.append("\n");

        if(state.enemies > 0){
            if(state.enemies == 1){
                builder.append(enemyf.get(state.enemies));
            }else{
                builder.append(enemiesf.get(state.enemies));
            }
            builder.append("\n");
        }

        if(state.rules.waveTimer){
            builder.append((logic.isWaitingWave() ? Core.bundle.get("wave.waveInProgress") : (waitingf.get((int)(state.wavetime/60)))));
        }else if(state.enemies == 0){
            builder.append(Core.bundle.get("waiting"));
        }
    }

    /**
     * Creates the player unit display with health/ammo bars.
     */
    private Stack createPlayerUnitDisplay(){
        return new Stack(
        new Element(){
            @Override
            public void draw(){
                Draw.color(Pal.darkerGray, parentAlpha);
                Fill.poly(x + width/2f, y + height/2f, 6, height / Mathf.sqrt3);
                Draw.reset();
                Drawf.shadow(x + width/2f, y + height/2f, height * 1.13f, parentAlpha);
            }
        },
        new Table(t -> {
            float bw = 40f;
            float pad = -20;
            t.margin(0);
            t.clicked(() -> {
                if(!player.dead() && mobile){
                    Call.unitClear(player);
                    control.input.recentRespawnTimer = 1f;
                    control.input.controlledType = null;
                }
            });

            t.add(new SideBar(() -> player.dead() ? 0f : player.unit().healthf(), () -> true, true)).width(bw).growY().padRight(pad);
            t.image(() -> player.icon()).scaling(Scaling.bounded).grow().maxWidth(54f);

            Boolp playerHasPayloads = () -> player.unit() instanceof Payloadc pay && !pay.payloads().isEmpty();
            Floatp playerPayloadCapacityUsed = () -> player.unit() instanceof Payloadc pay ? pay.payloadUsed() / player.unit().type().payloadCapacity : 0f;

            t.add(new SideBar(() -> player.dead() ? 0f : player.displayAmmo() ? player.unit().ammof() : playerHasPayloads.get() ? playerPayloadCapacityUsed.get() : player.unit().healthf(), () -> !(player.displayAmmo() || playerHasPayloads.get()), false)).width(bw).growY().padLeft(pad).update(b -> {
                b.color.set(player.displayAmmo() ? player.dead() || player.unit() instanceof BlockUnitc ? Pal.ammo : player.unit().type.ammoType.color() : playerHasPayloads.get() ? Pal.items : Pal.health);
            });

            t.getChildren().get(1).toFront();
        })
        );
    }

    /**
     * Checks if wave can be skipped.
     */
    public boolean canSkipWave(){
        return state.rules.waves && state.rules.waveSending && ((net.server() || player.admin) || !net.active()) && state.enemies == 0 && !spawner.isSpawning();
    }

    /**
     * Creates all IntFormat instances for wave/enemy formatting.
     */
    private IntFormat[] createIntFormats(){
        StringBuilder ibuild = new StringBuilder();
        return new IntFormat[]{
            new IntFormat("wave"),
            new IntFormat("wave.cap"),
            new IntFormat("wave.enemy"),
            new IntFormat("wave.enemies"),
            new IntFormat("wave.enemycore"),
            new IntFormat("wave.enemycores"),
            new IntFormat("wave.waiting", i -> {
                ibuild.setLength(0);
                int m = i/60;
                int s = i % 60;
                if(m > 0){
                    ibuild.append(m);
                    ibuild.append(":");
                    if(s < 10){
                        ibuild.append("0");
                    }
                }
                ibuild.append(s);
                return ibuild.toString();
            })
        };
    }

    /**
     * Sets up table properties and adds player unit display.
     */
    private void setupStatusTable(Table table){
        table.touchable = Touchable.enabled;
        table.name = "waves";
        table.marginTop(0).marginBottom(4).marginLeft(4);
        table.stack(createPlayerUnitDisplay()).size(120f, 80).padRight(4);
    }

    /**
     * Side bar element for displaying health/ammo bars.
     */
    public static class SideBar extends Element{
        public final Floatp amount;
        public final boolean flip;
        public final Boolp flash;

        float last, blink, value;

        public SideBar(Floatp amount, Boolp flash, boolean flip){
            this.amount = amount;
            this.flip = flip;
            this.flash = flash;

            setColor(Pal.health);
        }

        @Override
        public void draw(){
            float next = amount.get();

            if(Float.isNaN(next) || Float.isInfinite(next)) next = 1f;

            if(next < last && flash.get()){
                blink = 1f;
            }

            blink = Mathf.lerpDelta(blink, 0f, 0.2f);
            value = Mathf.lerpDelta(value, next, 0.15f);
            last = next;

            if(Float.isNaN(value) || Float.isInfinite(value)) value = 1f;

            drawInner(Pal.darkishGray, 1f);
            drawInner(Tmp.c1.set(color).lerp(Color.white, blink), value);
        }

        void drawInner(Color color, float fract){
            if(fract < 0) return;

            fract = Mathf.clamp(fract);
            if(flip){
                x += width;
                width = -width;
            }

            float stroke = width * 0.35f;
            float bh = height/2f;
            Draw.color(color, parentAlpha);

            float f1 = Math.min(fract * 2f, 1f), f2 = (fract - 0.5f) * 2f;

            float bo = -(1f - f1) * (width - stroke);

            Fill.quad(
            x, y,
            x + stroke, y,
            x + width + bo, y + bh * f1,
            x + width - stroke + bo, y + bh * f1
            );

            if(f2 > 0){
                float bx = x + (width - stroke) * (1f - f2);
                Fill.quad(
                x + width, y + bh,
                x + width - stroke, y + bh,
                bx, y + height * fract,
                bx + stroke, y + height * fract
                );
            }

            Draw.reset();

            if(flip){
                width = -width;
                x -= width;
            }
        }
    }
}

