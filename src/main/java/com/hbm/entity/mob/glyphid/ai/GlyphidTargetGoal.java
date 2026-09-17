package com.hbm.entity.mob.glyphid.ai;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

public class GlyphidTargetGoal extends Goal {

    private final EntityGlyphid glyphid;

    public GlyphidTargetGoal(EntityGlyphid glyphid) {
        this.glyphid = glyphid;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        // 1.7: acquisition only fires while entityToAttack is null, and a high-priority waypoint holds it
        if (glyphid.getTarget() == null && !glyphid.hasHighPriorityWaypoint()) {
            @Nullable Player found = glyphid.findTargetCandidate();
            if (found != null) glyphid.setTarget(found);
        }

        int id = glyphid.getId();
        if (id % 3 > 0 && (id + glyphid.tickCount) % 100 == 0) {
            @Nullable Player found = glyphid.findTargetCandidate();
            if (found != null) glyphid.setTarget(found);
        }
    }
}
