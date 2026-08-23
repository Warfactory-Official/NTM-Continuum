package com.hbm.items.machine;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKNeutronNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Concrete or lead-glass RBMK column lid. */
public class ItemRBMKLid extends Item {
    public final RBMKBase.Lid lid;

    public ItemRBMKLid(Properties properties, RBMKBase.Lid lid) { super(properties); this.lid = lid; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof RBMKBase rbmk) || rbmk.hasOwnLid()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockState coreState = level.getBlockState(core);
        if (RBMKBase.lidOf(coreState).present()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide()) {
            level.setBlock(core, coreState.setValue(RBMKBase.LID, lid), 3);
            if (NeutronNodeWorld.getNode(level, core) instanceof RBMKNeutronNode node) node.addLid();
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
