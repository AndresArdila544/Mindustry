package mindustry.world.blocks.distribution;

import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.helper.ConveyorGeometryUtils;
import mindustry.helper.ConveyorTimingController;
import mindustry.helper.ItemRoutingController;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Junction extends Block{
    public float speed = 26; //frames taken to go through this junction
    public int capacity = 6;
    public float displayedSpeed = 13f;

    public Junction(String name){
        super(name);
        update = true;
        solid = false;
        underBullets = true;
        group = BlockGroup.transportation;
        unloadable = false;
        floating = true;
        noUpdateDisabled = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        //(60f / speed * capacity) returns 13.84 which is not the actual value (non linear, depends on fps)
        stats.add(Stat.itemsMoved, displayedSpeed, StatUnit.itemsSecond);
        stats.add(Stat.itemCapacity, capacity, StatUnit.items);
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class JunctionBuild extends Building {

        DirectionalItemBuffer buffer = new DirectionalItemBuffer(6);

        // Extracted controllers
        ItemRoutingController routing = new ItemRoutingController();
        ConveyorTimingController timing = new ConveyorTimingController();
        ConveyorGeometryUtils geometry = new ConveyorGeometryUtils();

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return 0;
        }

        @Override
        public void updateTile(){
            for(int side = 0; side < 4; side++){
                if(buffer.indexes[side] <= 0) continue;

                long packed = buffer.buffers[side][0];
                float inputTime = BufferItem.time(packed);

                if(!timing.readyToTransfer(inputTime, ((Junction)block).speed, Time.delta)) continue;

                Item item = content.item(BufferItem.item(packed));
                Building dest = nearby(side);

                if(dest == null || item == null) continue;

                if(!dest.acceptItem(this, item) || dest.team != team) continue;

                dest.handleItem(this, item);

                System.arraycopy(buffer.buffers[side], 1, buffer.buffers[side], 0, buffer.indexes[side] - 1);
                buffer.indexes[side]--;
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            routing.routeItemToBuffer(buffer, this, source, item);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            int rel = source.relativeTo(tile);

            boolean accepts = rel != -1 && buffer.accepts(rel);
            Building next = nearby(rel);
            boolean sameTeam = next != null && next.team == team;

            return routing.canAccept(this, source, item, accepts, true, sameTeam);
        }
    }
}
