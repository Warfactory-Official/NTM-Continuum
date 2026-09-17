package com.hbm.entity.item;

import com.hbm.items.ModEntityItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import dev.golem.core.data.CompatBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// 1.7: EntityBoatRubber keeps the faster hull and direct steering on the current boat.
public class EntityBoatRubber extends CompatBoat {

    private double speedMultiplier = 0.07D;

    public EntityBoatRubber(EntityType<? extends EntityBoatRubber> type, Level level) {
        super(type, level, () -> ModEntityItems.boat_rubber.get());
    }

    @Override
    protected void controlBoat() {
        if (!isVehicle()) return;

        int forward = inputUp() == inputDown() ? 0 : inputUp() ? 1 : -1;
        int strafe = inputLeft() == inputRight() ? 0 : inputLeft() ? 1 : -1;
        Vec3 motion = getDeltaMovement();
        double previousSpeed = motion.horizontalDistance();

        if (forward != 0 || strafe != 0) {
            float heading = (float) (-(getYRot() + 90F) * Math.PI / 180D);
            double push = speedMultiplier * forward * 0.05D;
            motion = motion.add(Mth.sin(heading) * push, 0D, Mth.cos(heading) * push);

            float rotated = -strafe * 3F;
            setYRot(getYRot() + rotated);
            float turn = (float) (-rotated * Math.PI / 180D);
            float cos = Mth.cos(turn);
            float sin = Mth.sin(turn);
            motion = new Vec3(motion.x * cos + motion.z * sin, motion.y, motion.z * cos - motion.x * sin);
        }

        double speed = motion.horizontalDistance();
        if (speed > 0.5D) {
            motion = new Vec3(motion.x * (0.5D / speed), motion.y, motion.z * (0.5D / speed));
            speed = 0.5D;
        }

        if (speed > previousSpeed && speedMultiplier < 0.5D) {
            speedMultiplier = Math.min(0.5D, speedMultiplier + (0.5D - speedMultiplier) / 50D);
        } else {
            speedMultiplier = Math.max(0.07D, speedMultiplier - (speedMultiplier - 0.07D) / 35D);
        }

        setDeltaMovement(motion);
        setPaddleState(inputRight() && !inputLeft() || inputUp(), inputLeft() && !inputRight() || inputUp());
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (passenger instanceof Player player) player.setYBodyRot(Mth.wrapDegrees(getYRot() + 90F));
    }
}
