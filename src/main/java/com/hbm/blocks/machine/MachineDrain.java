package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineDrain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The legacy three-port drainage pipe. */
public class MachineDrain extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineDrain(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineDrain(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 2, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof IItemFluidId identifier)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide() && !player.isShiftKeyDown()
                && MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineDrain drain) {
            FluidType type = identifier.getType(level, drain.getBlockPos().getX(), drain.getBlockPos().getY(),
                    drain.getBlockPos().getZ(), stack);
            drain.setTankType(type);
            player.sendSystemMessage(Component.literal("Changed type to ")
                    .withStyle(ChatFormatting.YELLOW).append(type.getDisplayName()).append("!"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return level.isClientSide() || player.isShiftKeyDown() ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        if (dir == ForgeDirection.UNKNOWN) return false;
        BlockPos cell = new BlockPos(x, y, z);
        BlockPos core = MultiblockSurface.coreOfAny(world, cell);
        if (core == null || !(world.getBlockEntity(core) instanceof TileEntityMachineDrain drain)) return false;
        for (DirPos port : drain.getConPos()) {
            BlockPos outside = cell.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
            if (port.getDir() == dir && port.getPos().equals(outside)) return true;
        }
        return false;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineDrain drain)) return null;
        List<String> text = new ArrayList<>();
        text.add("§a-> §r" + drain.tank.getTankType().getDisplayNameString() + ": "
                + drain.tank.getFill() + "/" + drain.tank.getMaxFill() + "mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /** 1.7 reaches out on three sides only; the fourth is the drain's own inlet column. */
    @Override
    public int coreMask() {
        return MASK_SOUTH | MASK_WEST | MASK_EAST;
    }
}
