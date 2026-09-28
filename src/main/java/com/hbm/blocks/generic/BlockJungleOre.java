package com.hbm.blocks.generic;

import com.hbm.potion.ModEffects;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The two enargite bricks 1.7 registered as {@code BlockOre}, which gave them nothing but their
 * {@code onEntityWalking} arms: the radioactive brick ({@code brick_jungle_ooze}) applies radiation X for
 * 15 seconds to anything living that walks on it, the arcane brick ({@code brick_jungle_mystic}) taint III
 * for 15 seconds. Both glow at light 5 and drop themselves.
 */
public class BlockJungleOre extends CompatBlock {

    public enum Kind { OOZE, MYSTIC }

    private final Kind kind;

    public BlockJungleOre(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // 1.7 also ran this on the client, whose copy of the effect the server's sync then replaced
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (kind == Kind.OOZE) Effects.addRegistered(living, ModEffects.RADIATION.get(), 15 * 20, 9);
        else Effects.addRegistered(living, ModEffects.TAINT.get(), 15 * 20, 2);
    }
}
