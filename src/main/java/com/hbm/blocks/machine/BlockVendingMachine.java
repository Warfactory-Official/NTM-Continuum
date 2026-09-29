package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.items.ModItems;
import com.hbm.items.block.ItemBlockDecoVariant;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.sound.ModGunSounds;
import com.hbm.tileentity.deco.TileEntityVendingMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code BlockVendingMachine}: a two-tall {@code BlockDummyable} ({@code {1, 0, 0, 0, 0, 0}}, offset 0) in two
 * kinds - item damage 0 is the soda machine, 1 the snack machine. A vending token in hand is used up for one
 * roll of {@code POOL_SODA} or {@code POOL_SNACKS}, dropped out of the front with the bolt-open sound.
 *
 * <p>1.7 marked the snack machine with {@code makeExtra} on its upper cell (metadata 6 and up) and read it back
 * from there; here every cell of it carries {@link #SNACKS}, which the renderer, the drop and the token read.
 */
public class BlockVendingMachine extends BlockMultiblockMachine {

    public static final BooleanProperty SNACKS = BooleanProperty.create("snacks");

    public BlockVendingMachine(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SNACKS, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SNACKS);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityVendingMachine(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        boolean snacks = stack.getItem() instanceof ItemBlockDecoVariant item && item.variant(stack) % 2 == 1;
        if (!snacks) return;
        for (BlockPos p : new BlockPos[]{pos, pos.above()}) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() == this) level.setBlock(p, s.setValue(SNACKS, Boolean.TRUE), 2);
        }
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (held.isEmpty() || held.getItem() != ModItems.coin_token.get()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        held.shrink(1);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos core = coreOf(level, pos, state);
        if (core != null) {
            BlockState coreState = level.getBlockState(core);
            Direction dir = coreState.getValue(FACING);
            boolean snacks = coreState.getValue(SNACKS);
            ItemStack toDrop = ItemPool.getStack(snacks ? ItemPools.POOL_SNACKS : ItemPools.POOL_SODA, level.getRandom());

            if (!toDrop.isEmpty()) {
                // 1.7: the clicked cell's column, at the core's height, a little in front of the machine
                ItemEntity item = new ItemEntity(level, pos.getX() + 0.5 + dir.getStepX() * 0.75, core.getY() + 0.25,
                        pos.getZ() + 0.5 + dir.getStepZ() * 0.75, toDrop);
                level.addFreshEntity(item);
            }

            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModGunSounds.BOLT_OPEN.get(), SoundSource.BLOCKS, 1F, 0.75F);
        }
        return InteractionResult.SUCCESS;
    }

    private ItemStack stackFor(BlockState state) {
        int variant = state.getValue(SNACKS) ? 1 : 0;
        return asItem() instanceof ItemBlockDecoVariant item ? item.of(variant) : new ItemStack(this);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stackFor(state));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return stackFor(state);
    }

}
