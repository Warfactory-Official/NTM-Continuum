package com.hbm.blocks.machine.rbmk;
import com.hbm.handler.BossSpawnHandler; import com.hbm.items.machine.ItemRBMKRod; import com.hbm.sound.ModSounds; import com.hbm.tileentity.ModRBMKBlockEntities; import com.hbm.tileentity.machine.rbmk.*; import net.minecraft.core.BlockPos; import net.minecraft.sounds.SoundSource; import net.minecraft.world.InteractionHand; import net.minecraft.world.InteractionResult; import net.minecraft.world.entity.player.Player; import net.minecraft.world.item.ItemStack; import net.minecraft.world.level.Level; import net.minecraft.world.level.block.entity.BlockEntityType; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.phys.BlockHitResult;
public class RBMKRod extends RBMKBase { public final boolean moderated; public RBMKRod(Properties properties, boolean moderated) { super(properties); this.moderated = moderated; } @Override protected BlockEntityType<? extends BlockEntityRBMKBase> beType() { return ModRBMKBlockEntities.RBMK_ROD.get(); } @Override protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) { return new BlockEntityRBMKRod(pos, state); }
    /** 1.7 {@code onBlockActivated}: a fuel rod in hand goes straight into an empty channel, lid or not, and opens no GUI. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!(held.getItem() instanceof ItemRBMKRod)) return super.onUseItem(state, level, pos, player, hand, held, hit);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos core = coreOf(level, pos, state);
        if (core == null || !(level.getBlockEntity(core) instanceof BlockEntityRBMKRod rod) || !rod.inventory.getStackInSlot(0).isEmpty())
            return super.onUseItem(state, level, pos, player, hand, held, hit);
        BossSpawnHandler.markFBI(player);
        rod.inventory.setStackInSlot(0, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) held.shrink(1);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        rod.markChanged();
        return InteractionResult.SUCCESS;
    }
}
