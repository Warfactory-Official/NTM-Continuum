package com.hbm.blocks.machine;

import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code BlockCustomMachine} ({@code custom_machine}): the controller of a configured machine. Its item carries
 * the machine's configuration by name ({@link ModDataComponents#CUSTOM_MACHINE}; 1.7 kept index + 100 in the damage
 * value); placing it
 * stamps the configuration on the block entity, breaking it gives the same item back, and the filters in slots
 * 10-15 are not dropped.
 *
 * <p>Right-click opens the machine when its structure is complete. With an incomplete structure and 1.7's
 * structure wand ({@code wand_s}, {@link com.hbm.items.ModWandItems#wand_s}) in hand it builds the structure instead.
 */
public class BlockCustomMachine extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockCustomMachine(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCustomMachine(pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return activate(level, pos, player);
    }

    private InteractionResult activate(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return player.isShiftKeyDown() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {

            if (level.getBlockEntity(pos) instanceof TileEntityCustomMachine tile) {

                if (tile.checkStructure()) {
                    if (player instanceof ServerPlayer serverPlayer && tile.config != null) {
                        dev.golem.core.gui.Menus.openWithPos(serverPlayer, tile, pos);
                    }
                } else if (isStructureWand(player.getMainHandItem())) {
                    tile.buildStructure();
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static boolean isStructureWand(ItemStack held) {
        return held.is(com.hbm.items.ModWandItems.wand_s.get());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.getBlockEntity(pos) instanceof TileEntityCustomMachine tile) {
            MachineConfiguration config = CustomMachineConfigJSON.byName(ModDataComponents.CUSTOM_MACHINE.getOr(stack, null));

            if (config != null) {
                tile.machineType = config.unlocalizedName;
                tile.init();
                tile.markChanged();
                tile.setChanged();
            }
        }
    }

    /** The controller item for the machine at this position (1.7: damage {@code niceList.indexOf(config) + 100}). */
    public ItemStack stackFor(@Nullable BlockEntity be) {
        ItemStack stack = new ItemStack(this);
        if (be instanceof TileEntityCustomMachine tile && tile.config != null) {
            ModDataComponents.CUSTOM_MACHINE.set(stack, tile.config.unlocalizedName);
        }
        return stack;
    }

    /** 1.7 {@code getDrops}: survival breaking and explosions give the configured controller back; creative does not. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> ret = new ArrayList<>();
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof TileEntityCustomMachine) ret.add(stackFor(be));
        return ret;
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return pick(level, pos);
    }

    private ItemStack pick(BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TileEntityCustomMachine tile && tile.machineType != null && !tile.machineType.isEmpty()) {
            return stackFor(be);
        }
        return new ItemStack(this);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
