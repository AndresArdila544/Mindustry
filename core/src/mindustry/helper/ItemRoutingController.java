package mindustry.helper;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.DirectionalItemBuffer;

public class ItemRoutingController {

    public boolean canAccept(Building self, Building source, Item item, boolean itemsCompatible, boolean notFull, boolean correctDirection){
        if(self == source) return notFull && itemsCompatible;
        if(!itemsCompatible) return false;
        return notFull && correctDirection;
    }

    public void routeItemToBuffer(DirectionalItemBuffer buffer, Building self, Building source, Item item){
        int relative = source.relativeTo(self.tile);
        buffer.accept(relative, item);
    }

    public boolean moveForward(Building front, Item item){
        if(front != null && front.acceptItem(null, item)){
            front.handleItem(null, item);
            return true;
        }
        return false;
    }
}
