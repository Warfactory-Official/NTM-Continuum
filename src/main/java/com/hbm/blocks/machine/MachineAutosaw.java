package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable;
import dev.golem.core.registry.TickingBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The single-block autosaw and its legacy fluid-typing, suspension, and look-overlay hooks. */
public class MachineAutosaw extends dev.golem.core.world.CompatBlock
        implements TickingBlock, IFluidConnectorBlockMK2, IToolable, ILookOverlay {

    public MachineAutosaw(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAutosaw(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, net.minecraft.world.item.ItemStack stack,
                                          BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (!(stack.getItem() instanceof IItemFluidId identifier)) {
            return super.onUseItem(state, level, pos, player, hand, stack, hit);
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof TileEntityMachineAutosaw saw) {
            FluidType type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            if (TileEntityMachineAutosaw.acceptedFuels.contains(type)) {
                saw.tank.setTankType(type);
                saw.setChanged();
                player.sendSystemMessage(Component.literal("Changed type to ")
                        .append(type.getDisplayName()).append("!").withStyle(ChatFormatting.YELLOW));
                return InteractionResult.SUCCESS;
            }
        }

        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return level.isClientSide() || player.isShiftKeyDown()
                ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, net.minecraft.core.Direction side,
                           Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER
                || !(level.getBlockEntity(pos) instanceof TileEntityMachineAutosaw saw)) return false;

        if (!level.isClientSide()) {
            saw.isSuspended = !saw.isSuspended;
            saw.setChanged();
        }
        return true;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN
                && TileEntityMachineAutosaw.acceptedFuels.contains(type)
                && world.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityMachineAutosaw;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityMachineAutosaw saw)) return null;

        List<String> text = new ArrayList<>();
        text.add(saw.tank.getTankType().getDisplayNameString() + ": "
                + saw.tank.getFill() + "/" + saw.tank.getMaxFill() + "mB");
        if (saw.isSuspended) {
            text.add("§c! " + Component.translatable("tile.machine_autosaw.suspended").getString() + " !");
        }
        return new ILookOverlay.Data(Component.translatable("tile.machine_autosaw.name").getString(),
                0xffff00, 0x404000, text);
    }

}
