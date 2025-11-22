package mindustry.helper;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import mindustry.gen.Building;

public class TransferAnimationManager {

    private final Vec2 tmp1 = new Vec2();
    private final Vec2 tmp2 = new Vec2();

    public void drawInterpolatedItem(Building from, Building to, float cooldown, float rotationA, float rotationB, float size, TextureRegion icon){

        tmp1.set(from.x, from.y);
        tmp2.set(to.x, to.y);

        tmp1.interpolate(tmp2, 1f - cooldown, Interp.linear);

        float rot = Mathf.lerp(rotationA, rotationB,
                Interp.smooth.apply(1f - Mathf.clamp(cooldown * 2, 0f, 1f)));

        // this now matches Draw.rect's real signature
        Draw.rect(icon, tmp1.x, tmp1.y, size, size, rot);
    }
}
