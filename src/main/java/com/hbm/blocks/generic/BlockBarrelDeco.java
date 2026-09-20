package com.hbm.blocks.generic;

import com.hbm.blocks.bomb.BlockDetonatable;
import com.hbm.blocks.bomb.BlockTNTBase;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import com.hbm.tileentity.network.TileEntityBarrelDeco;
import com.hbm.api.block.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code BlockDetonatable} half of {@code RedBarrel}/{@code YellowBarrel}: a barrel-shaped
 * (12×16×12) explosive drum that goes off with a random fuse of 50–150 ticks when caught in an
 * explosion, that a flammable variant also lights from adjacent fire or lava and detonates instantly
 * when shot ({@link BlockDetonatable}), and whose primed entity blows up on the first thing it hits
 * ({@code detonateOnCollision}).
 *
 * <p>Extends {@link BlockTNTBase} only so the shared {@link EntityPrimedExplosive} can call back into
 * {@link #detonate}; the TNT base's redstone/flint-and-steel/screwdriver behaviours are switched off
 * here, because 1.7 barrels had none of them. The drum is drawn by a block-entity renderer
 * ({@code barrel.obj}), which is what {@link TileEntityBarrelDeco} exists for.
 */
public abstract class BlockBarrelDeco extends BlockTNTBase implements EntityBlock, BlockDetonatable {

    private static final VoxelShape SHAPE = Shapes.box(0.125, 0, 0.125, 0.875, 1, 0.875);
    private static final int POP_FUSE = 100;

    protected final String texture;
    protected final int kind;
    protected final boolean flammable;

    protected BlockBarrelDeco(Properties properties, String texture, int kind, boolean flammable) {
        super(properties);
        this.texture = texture;
        this.kind = kind;
        this.flammable = flammable;
    }

    public String barrelTexture() {
        return texture;
    }

    @Override public int kindId() { return kind; }
    @Override public String textureName() { return texture; }
    @Override public float blastStrength() { return 0F; }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBarrelDeco(pos, state);
    }

    // --- ignition: only fire next to a flammable drum, checked on neighbour change (1.7 BlockFlammable.shouldIgnite / BlockDetonatable) ---

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        checkIgnite(level, pos);
    }

    private void checkIgnite(Level level, BlockPos pos) {
        if (level.isClientSide() || !flammable) return;
        for (Direction dir : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(dir));
            if (n.is(Blocks.FIRE)) {
                level.removeBlock(pos, false);
                pop(level, pos);
                return;
            }
        }
    }

    /** The 1.7 {@code onBlockDestroyedByExplosion}: a primed drum with a 50–150 tick fuse that pops on impact. */
    private void pop(Level level, BlockPos pos) {
        EntityPrimedExplosive primed = new EntityPrimedExplosive(level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, null, defaultBlockState());
        primed.setFuse(level.getRandom().nextInt(POP_FUSE) + POP_FUSE / 2);
        primed.detonateOnCollision = true;
        level.addFreshEntity(primed);
    }

    @Override
    protected boolean onExplosion(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                                  net.minecraft.world.level.Explosion explosion,
                                  java.util.function.BiConsumer<ItemStack, BlockPos> onHit) {
        level.removeBlock(pos, false);
        pop(level, pos);
        return true;
    }

    @Override
    public void onShot(Level level, BlockPos pos) {
        if (!flammable || level.isClientSide()) return;
        level.removeBlock(pos, false);
        detonate(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, null); // insta-explod
    }

    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {}

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, net.minecraft.world.entity.projectile.Projectile projectile) {}

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, IToolable.ToolType tool) {
        return false;
    }

    /** The 1.7 {@code explodeEntity}; {@code primed} is null for the instant on-shot detonation. */
    @Override
    public abstract void detonate(Level level, double x, double y, double z, @Nullable EntityPrimedExplosive primed);
}
