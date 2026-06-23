package com.hbm.blocks;

import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;

public abstract class BlockDummyable extends Block implements EntityBlock {

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    public static final int offset = 10;

    public static final int extra = 6;

    public static boolean safeRem = false;

    public BlockDummyable(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    public abstract int[] getDimensions();

    public abstract int getOffset();

    public int getHeightOffset() {
        return 0;
    }

    public ForgeDirection getDirModified(ForgeDirection dir) {
        return dir;
    }

    protected int getMetaForCore(Level world, BlockPos pos, Player player, int original) {
        return original;
    }

    protected int getMaxCoreSearchSteps() {
        return 512;
    }

    protected boolean isSameMultiblock(Block other) {
        return other == this;
    }

    @Nullable
    public BlockPos findCore(BlockGetter world, BlockPos pos) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos().set(pos);
        for (int steps = 0, max = getMaxCoreSearchSteps(); steps < max; steps++) {
            BlockState state = world.getBlockState(m);
            if (!isSameMultiblock(state.getBlock())) return null;
            int meta = state.getValue(META);
            if (meta >= 12) return m.immutable();
            if (meta >= extra) meta -= extra;
            ForgeDirection dir = ForgeDirection.getOrientation(meta).getOpposite();
            m.move(dir.offsetX, dir.offsetY, dir.offsetZ);
        }
        return null;
    }

    @Nullable
    public BlockEntity findCoreTE(BlockGetter world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        return core == null ? null : world.getBlockEntity(core);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? createCoreBlockEntity(pos, state) : null;
    }

    protected abstract BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state);

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockPos core = findCore(level, pos);
            if (core != null && level.getBlockEntity(core) instanceof MenuProvider provider) {
                Services.PLATFORM.openMenu(serverPlayer, provider, core);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null) return this.defaultBlockState();
        Placement p = resolvePlacement(player, context.getClickedPos());
        if (!checkRequirement(context.getLevel(), p.x, p.y, p.z, p.dir, p.o)) {
            return null;
        }
        return this.defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (world.isClientSide() || !(placer instanceof Player player)) return;

        safeRem = true;
        world.removeBlock(pos, false);
        safeRem = false;

        Placement p = resolvePlacement(player, pos);
        if (!checkRequirement(world, p.x, p.y, p.z, p.dir, p.o)) return;

        BlockPos core = new BlockPos(p.x + p.dir.offsetX * p.o, p.y + p.dir.offsetY * p.o, p.z + p.dir.offsetZ * p.o);

        int meta = getMetaForCore(world, core, player, p.dir.ordinal() + offset);
        world.setBlock(core, this.defaultBlockState().setValue(META, meta), 3);
        fillSpace(world, p.x, p.y, p.z, p.dir, p.o);
    }

    private record Placement(ForgeDirection dir, int x, int y, int z, int o) {}

    private Placement resolvePlacement(Player player, BlockPos clicked) {
        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        ForgeDirection dir = switch (i) {
            case 0 -> ForgeDirection.getOrientation(2);
            case 1 -> ForgeDirection.getOrientation(5);
            case 2 -> ForgeDirection.getOrientation(3);
            case 3 -> ForgeDirection.getOrientation(4);
            default -> ForgeDirection.NORTH;
        };
        dir = getDirModified(dir);
        BlockPos base = clicked.above(getHeightOffset());
        return new Placement(dir, base.getX(), base.getY(), base.getZ(), -getOffset());
    }

    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), x, y, z, dir);
    }

    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), this, dir);
    }

    public void makeExtra(Level world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState s = world.getBlockState(pos);
        if (s.getBlock() != this) return;
        int meta = s.getValue(META);
        if (meta > 5) return;
        safeRem = true;
        world.setBlock(pos, s.setValue(META, meta + extra), 3);
        safeRem = false;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (safeRem) return;
        int meta = state.getValue(META);
        safeRem = true;
        try {

            if (meta < 12) {
                int m = meta >= extra ? meta - extra : meta;
                ForgeDirection toCore = ForgeDirection.getOrientation(m).getOpposite();
                BlockPos core = findCore(level, pos.offset(toCore.offsetX, toCore.offsetY, toCore.offsetZ));
                if (core != null) {
                    level.removeBlock(core, false);
                    cascadeOrphans(level, core);
                }
            }

            cascadeOrphans(level, pos);
        } finally {
            safeRem = false;
        }
    }

    private boolean isOrphan(BlockGetter world, BlockPos pos, BlockState state) {
        int meta = state.getValue(META);
        if (meta >= 12) return false;
        if (meta >= extra) meta -= extra;
        ForgeDirection toCore = ForgeDirection.getOrientation(meta).getOpposite();
        BlockPos towards = pos.offset(toCore.offsetX, toCore.offsetY, toCore.offsetZ);
        return world.getBlockState(towards).getBlock() != this;
    }

    private void cascadeOrphans(Level level, BlockPos seed) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            queue.add(seed.offset(d.offsetX, d.offsetY, d.offsetZ));
        }
        int cap = 4096;
        while (!queue.isEmpty() && cap-- > 0) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (s.getBlock() != this || !isOrphan(level, p, s)) continue;
            level.removeBlock(p, false);
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                queue.add(p.offset(d.offsetX, d.offsetY, d.offsetZ));
            }
        }
    }
}
