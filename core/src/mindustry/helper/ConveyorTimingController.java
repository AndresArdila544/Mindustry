package mindustry.helper;

import arc.util.Time;

public class ConveyorTimingController {

    public boolean readyToTransfer(float lastTime, float speed, float timeScale){
        return (Time.time >= lastTime + speed / timeScale || Time.time < lastTime);
    }

    public float updateCooldown(float cooldown, float speed, float efficiency, float delta, float maxRecharge){
        cooldown -= speed * efficiency * delta;
        return Math.max(0, Math.min(cooldown, maxRecharge));
    }

    public boolean transferBlocked(float cooldown, float recharge){
        return cooldown > recharge - 1f;
    }
}