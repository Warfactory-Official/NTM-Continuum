package com.hbm.blocks.generic;

import com.hbm.tileentity.BedrockOreBlockEntity;
import dev.golem.core.registry.TickingBlock;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidStack;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Infinite, unbreakable bedrock ore. Extraction is performed through its block entity, not block drops. */
public class BlockBedrockOre extends CompatBlock implements TickingBlock, ILookOverlay {

    public BlockBedrockOre(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BedrockOreBlockEntity(pos, state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 3 * 20));
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore)) return null;
        List<String> lines = new ArrayList<>();
        if (!ore.getResource().isEmpty()) lines.add(ore.getResource().getHoverName().getString());
        lines.add(Component.translatableWithFallback("desc.tier", "Tier: %s", ore.getTier()).getString());
        FluidStack acid = ore.getAcidRequirement();
        if (acid != null) {
            lines.add(Component.translatableWithFallback("desc.requires", "Requires: %smB %s",
                    acid.fill, acid.type.getDisplayName()).getString());
        }
        return new ILookOverlay.Data(getName().getString(), 0xFFFF00, 0x404000, lines);
    }
}
