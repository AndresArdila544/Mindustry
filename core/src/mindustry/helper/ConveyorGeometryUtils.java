package mindustry.helper;

import arc.math.geom.Geometry;

public class ConveyorGeometryUtils {

    public int relativeDirection(int rotation, int index){
        return rotation - index & 3;
    }

    public float offsetX(int dir, float amount){
        return Geometry.d4x(dir) * amount;
    }

    public float offsetY(int dir, float amount){
        return Geometry.d4y(dir) * amount;
    }
}