package mindustry.world.blocks.distribution;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.helper.ConveyorGeometryUtils;
import mindustry.helper.ConveyorTimingController;
import mindustry.helper.ItemRoutingController;
import mindustry.helper.TransferAnimationManager;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.Conveyor.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class StackConveyor extends Block implements Autotiler{
    protected static final int stateMove = 0, stateLoad = 1, stateUnload = 2;

    public @Load(value = "@-#", length = 3) TextureRegion[] regions;
    public @Load("@-edge") TextureRegion edgeRegion;
    public @Load("@-stack") TextureRegion stackRegion;
    /** requires power to work properly */
    public @Load(value = "@-glow") TextureRegion glowRegion;
    public @Load(value = "@-edge-glow", fallback = "@-glow") TextureRegion edgeGlowRegion;

    public float glowAlpha = 1f;
    public Color glowColor = Pal.redLight;

    public float baseEfficiency = 0f;
    public float speed = 0f;
    public boolean outputRouter = true;
    /** (minimum) amount of loading docks needed to fill a line. */
    public float recharge = 2f;
    public Effect loadEffect = Fx.conveyorPoof;
    public Effect unloadEffect = Fx.conveyorPoof;

    public StackConveyor(String name){
        super(name);

        rotate = true;
        update = true;
        group = BlockGroup.transportation;
        hasItems = true;
        itemCapacity = 10;
        conveyorPlacement = true;
        underBullets = true;
        priority = TargetPriority.transport;

        ambientSound = Sounds.conveyor;
        ambientSoundVolume = 0.004f;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemsMoved, Mathf.round(itemCapacity * speed * 60), StatUnit.itemsSecond);
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        if(tile.build instanceof StackConveyorBuild b){
            int state = b.state;
            if(state == stateLoad){ //standard conveyor mode
                return otherblock.outputsItems() && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
            }else if(state == stateUnload && !outputRouter){ //router mode
                return otherblock.acceptsItems &&
                    (!otherblock.noSideBlend || lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock)) &&
                    (notLookingAt(tile, rotation, otherx, othery, otherrot, otherblock) ||
                    (otherblock instanceof StackConveyor && facing(otherx, othery, otherrot, tile.x, tile.y))) &&
                    !(world.build(otherx, othery) instanceof StackConveyorBuild s && s.state == stateUnload) &&
                    !(world.build(otherx, othery) instanceof StackConveyorBuild s2 && s2.state == stateMove &&
                        !facing(otherx, othery, otherrot, tile.x, tile.y));
            }
        }
        return otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock) && otherblock instanceof StackConveyor;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        int[] bits = getTiling(plan, list);

        if(bits == null) return;

        TextureRegion region = regions[0];
        Draw.rect(region, plan.drawx(), plan.drawy(), plan.rotation * 90);

        for(int i = 0; i < 4; i++){
            if((bits[3] & (1 << i)) == 0){
                Draw.rect(edgeRegion, plan.drawx(), plan.drawy(), (plan.rotation - i) * 90);
            }
        }
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        Building tile = world.build(x, y);
        if(tile instanceof StackConveyorBuild s){
            return s.state != stateUnload;
        }
        return super.rotatedOutput(x, y);
    }

    public class StackConveyorBuild extends Building {

        int state;
        int link = -1;
        float cooldown;
        Item lastItem;

        ItemRoutingController routing = new ItemRoutingController();
        ConveyorTimingController timing = new ConveyorTimingController();
        ConveyorGeometryUtils geom = new ConveyorGeometryUtils();
        TransferAnimationManager anim = new TransferAnimationManager();

        @Override
        public void updateTile(){
            float eff = enabled ? (efficiency + ((StackConveyor)block).baseEfficiency) : 1f;

            cooldown = timing.updateCooldown(cooldown, ((StackConveyor)block).speed, eff, delta(), ((StackConveyor)block).recharge);

            if(link == -1 || cooldown > 0f) return;

            if(lastItem == null || !items.has(lastItem)){
                lastItem = items.first();
            }

            if(!enabled) return;

            // unload mode
            if(state == 1){
                while(lastItem != null && routing.moveForward(front(), lastItem)){
                    items.remove(lastItem, 1);
                    if(!items.has(lastItem)){
                        lastItem = null;
                        break;
                    }
                }
                return;
            }

            // normal transfer
            if(front() instanceof StackConveyorBuild next && next.team == team){
                if(next.link == -1){
                    next.items.add(items);
                    next.lastItem = lastItem;
                    next.link = tile.pos();
                    link = -1;
                    items.clear();
                    cooldown = ((StackConveyor)block).recharge;
                    next.cooldown = 1f;
                }
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            if(items.empty()) link = tile.pos();
            super.handleItem(source, item);
            lastItem = item;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            boolean compatible = !items.any() || items.has(item);
            boolean notFull = items.total() < itemCapacity;
            return routing.canAccept(this, source, item, compatible, notFull, true);
        }

        // Draw is cleaned up to delegate animations
        @Override
        public void draw(){
            Draw.rect(((StackConveyor)block).regions[state], x, y, rotdeg());

            if(link != -1 && lastItem != null){
                Tile from = world.tile(link);
                if(from != null && from.build != null){
                    anim.drawInterpolatedItem(from.build, this, cooldown,
                            from.build.rotation * 90,
                            rotation * 90,
                            6f,
                            lastItem.fullIcon
                    );
                }
            }
        }
    }
}
