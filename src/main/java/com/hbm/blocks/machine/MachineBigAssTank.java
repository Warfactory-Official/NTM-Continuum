package com.hbm.blocks.machine;

import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineBigAssTank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The legacy 9x9x5 Big Ass Tank multiblock. */
public class MachineBigAssTank extends MachineGiantTank {

    public MachineBigAssTank(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineBigAssTank(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{5, 0, 4, 4, 4, 4};
    }

    @Override
    public int getOffset() {
        return 6;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        BlockPos core = new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset);
        fillAdditional(level, core, dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        fillAdditional(level, core, dir);
    }

    private void fillAdditional(Level level, BlockPos core, ForgeDirection dir) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        com.hbm.handler.MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{4, 0, 5, -4, 2, 2}, this, dir);
        com.hbm.handler.MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{4, 0, -4, 5, 2, 2}, this, dir);
        com.hbm.handler.MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{4, 0, 2, 2, 5, -4}, this, dir);
        com.hbm.handler.MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{4, 0, 2, 2, -4, 5}, this, dir);
        com.hbm.handler.MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{3, 0, 6, -5, 0, 0}, this, dir);
        com.hbm.handler.MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{3, 0, -5, 6, 0, 0}, this, dir);
        makeExtra(level, x + dir.offsetX * 6, y, z + dir.offsetZ * 6);
        makeExtra(level, x - dir.offsetX * 6, y, z - dir.offsetZ * 6);
    }

    @Override
    public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        BlockPos core = new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset);
        if (!com.hbm.handler.MultiblockHandlerXR.checkSpace(level, core.getX(), core.getY(), core.getZ(),
                getDimensions(), x, y, z, dir)) return false;
        int[][] regions = {
                {4, 0, 5, -4, 2, 2}, {4, 0, -4, 5, 2, 2},
                {4, 0, 2, 2, 5, -4}, {4, 0, 2, 2, -4, 5},
                {3, 0, 6, -5, 0, 0}, {3, 0, -5, 6, 0, 0}
        };
        for (int[] region : regions) {
            if (!com.hbm.handler.MultiblockHandlerXR.checkSpace(level, core.getX(), core.getY(), core.getZ(),
                    region, x, y, z, dir)) return false;
        }
        return true;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(stack.getItem() instanceof IItemFluidId identifier)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && findCoreTE(level, pos) instanceof TileEntityMachineBigAssTank tank) {
            var type = identifier.getType(level, tank.getBlockPos().getX(), tank.getBlockPos().getY(),
                    tank.getBlockPos().getZ(), stack);
            tank.setTankType(type);
            player.sendSystemMessage(Component.literal("Changed type to ")
                    .withStyle(ChatFormatting.YELLOW).append(type.getDisplayName()).append("!"));
        }
        return InteractionResult.SUCCESS;
    }

}
