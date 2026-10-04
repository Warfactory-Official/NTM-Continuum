package com.hbm.entity.mob.glyphid.ai;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code EntityGlyphid.updateWanderPath}: the old-AI {@code EntityCreature} wander, only while the bug is idle.
 * 1.7 rolled it each tick with {@code (no path and 1 in 180) or 1 in 120}, about 1 in 72 for a bug standing
 * still, and only while {@code entityAge < 100} (the stroll goal's own no-action-time check); the spot is the best
 * of ten within 6 horizontally and 3 vertically by path weight, darker preferred for a mob - which is exactly what
 * {@code DefaultRandomPos.getPos(mob, 6, 3)} does with {@code Monster.getWalkTargetValue}.
 */
public class GlyphidWanderGoal extends RandomStrollGoal {

    /** About 1 in 72 per tick; the goal selector halves it back through {@code reducedTickDelay}. */
    private static final int LEGACY_WANDER_INTERVAL = 72;

    private final EntityGlyphid glyphid;

    public GlyphidWanderGoal(EntityGlyphid glyphid, double speedModifier) {
        super(glyphid, speedModifier, LEGACY_WANDER_INTERVAL);
        this.glyphid = glyphid;
    }

    @Override
    public boolean canUse() {
        return glyphid.getCurrentTask() == EntityGlyphid.TASK_IDLE && super.canUse();
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        return DefaultRandomPos.getPos(this.mob, 6, 3);
    }
}
