package com.hbm.blocks.generic;

import com.hbm.items.block.ItemBlockDecoVariant;
import com.hbm.tileentity.deco.TileEntityTrinket;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The part 1.7's {@code BlockBobble}, {@code BlockSnowglobe} and {@code BlockPlushie} share: a render-type -1
 * figure on a sixteen-way facing (the 1.7 metadata, {@code floor((yaw + 180) * 16 / 360 + 0.5) & 15}), whose
 * type sits in the block entity and comes from the placing stack's damage.
 *
 * <p>1.7 dropped nothing through the normal path ({@code getItemDropped} null) and spawned the typed item by
 * hand in {@code onBlockHarvested} for a non-creative player only - so an explosion or a creative break gives
 * nothing, and any tool (or none) gives the figure back. {@link #getDrops} keeps that: only a player's break.
 */
public abstract class BlockTrinket extends CompatBlock implements TickingBlock {

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

    protected BlockTrinket(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        float yaw = context.getPlayer() == null ? 0F : context.getPlayer().getYRot();
        return defaultBlockState().setValue(ROTATION, Mth.floor((yaw + 180.0F) * 16.0F / 360.0F + 0.5D) & 15);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityTrinket trinket
                && stack.getItem() instanceof ItemBlockDecoVariant item) {
            trinket.setTypeOrdinal(item.variant(stack));
        }
    }

    protected ItemStack stackFor(int type) {
        return asItem() instanceof ItemBlockDecoVariant item ? item.of(type) : new ItemStack(this);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(params.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player)) return List.of();
        return List.of(stackFor(be instanceof TileEntityTrinket trinket ? trinket.typeOrdinal() : 0));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return clone(level, pos);
    }

    private ItemStack clone(BlockGetter level, BlockPos pos) {
        return stackFor(level.getBlockEntity(pos) instanceof TileEntityTrinket trinket ? trinket.typeOrdinal() : 0);
    }

    /** Structure rotation and mirroring turn the 16-step placement angle, as vanilla's skulls and banners do. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }
}
