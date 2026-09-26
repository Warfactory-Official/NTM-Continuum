package com.hbm.blocks.network;

import net.minecraft.core.Direction;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.registry.TickingBlock;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.api.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.network.TileEntityFluidValve;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** A manually toggled, typed fluid-pipe gate. */
public class FluidValve extends dev.golem.core.world.CompatBlock
        implements TickingBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public FluidValve(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidValve(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof IItemFluidId identifier)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityFluidValve valve) {
            FluidType type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            if (type != valve.getFluidType()) {
                if (player.isShiftKeyDown()) {
                    BlockFluidDuct.spreadType(level, pos, type, valve.getFluidType(), 256);
                } else {
                    valve.setFluidType(type);
                    level.updateNeighborsAt(pos, this);
                }
                player.sendSystemMessage(Component.literal("Valve fluid: ")
                        .append(type.getDisplayName()).withStyle(ChatFormatting.AQUA));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * A block appearing beside a valve is the one endpoint transition nothing else announces, exactly as
     * for a duct — an open valve is a conductor with faces of its own.
     */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            dev.golem.core.graph.LevelNodeGraph.invalidateEndpointsAt(server, pos);
        }
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide()) {
            boolean open = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, open), 3);
            level.playSound(null, pos, ModSounds.REACTOR_START.get(), net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0F, open ? 1.0F : 0.85F);
            level.updateNeighborsAt(pos, this);
            if (level.getBlockEntity(pos) instanceof TileEntityFluidValve valve) valve.updateState();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return world.getBlockState(new BlockPos(x, y, z)).getValue(OPEN)
                && world.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityFluidValve valve
                && valve.getFluidType() != com.hbm.inventory.fluid.Fluids.NONE
                && valve.canConnect(type, dir.toDirection());
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFluidValve valve)) return null;
        List<String> text = new ArrayList<>();
        text.add(level.getBlockState(pos).getValue(OPEN) ? "§aOpen" : "§cClosed");
        text.add("&[" + valve.getFluidType().getColor() + "&]" + valve.getFluidType().getDisplayNameString());
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

}
