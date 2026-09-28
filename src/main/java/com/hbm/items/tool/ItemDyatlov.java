package com.hbm.items.tool;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import dev.golem.core.world.CompatItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1.7 {@code ItemDyatlov}, the Dyatlov Instant Meltdown Applicator: an RBMK column melts down on the spot and a
 * ZIRNOX core is set to 200,000 heat. 1.7 always answered {@code false}, so the click still falls through.
 */
public class ItemDyatlov extends CompatItem {

    public ItemDyatlov(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            BlockPos pos = context.getClickedPos();
            Block block = level.getBlockState(pos).getBlock();
            BlockPos core = MultiblockSurface.coreOfAny(level, pos);
            BlockEntity te = level.getBlockEntity(core != null ? core : pos);

            if (block instanceof RBMKBase && te instanceof BlockEntityRBMKBase rbmk) {
                rbmk.meltdown();
            }
            if (te instanceof TileEntityReactorZirnox zirnox) {
                zirnox.heat = 200000;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
