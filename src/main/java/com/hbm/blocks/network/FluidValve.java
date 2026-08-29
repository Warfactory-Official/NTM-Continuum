package com.hbm.blocks.network;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.client.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.network.TileEntityFluidValve;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        implements EntityBlock, IFluidConnectorBlockMK2, ILookOverlay {

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
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.FLUID_VALVE.get(), TileEntityFluidValve::serverTick);
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
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return world.getBlockState(new BlockPos(x, y, z)).getValue(OPEN)
                && world.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityFluidValve valve
                && valve.getFluidType() != com.hbm.inventory.fluid.Fluids.NONE
                && valve.canConnect(type, dir);
    }

    @Override
    public void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFluidValve valve)) return;
        List<String> text = new ArrayList<>();
        text.add(level.getBlockState(pos).getValue(OPEN) ? "§aOpen" : "§cClosed");
        text.add("&[" + valve.getFluidType().getColor() + "&]" + valve.getFluidType().getDisplayNameString());
        ILookOverlay.printGeneric(graphics, getName().getString(), 0xffff00, 0x404000, text);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
