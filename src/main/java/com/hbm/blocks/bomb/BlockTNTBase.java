package com.hbm.blocks.bomb;

import com.hbm.api.block.IToolable;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Fires;
import dev.golem.core.world.ItemStacks;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Shared legacy TNT behavior: configurable break ignition and a custom persistent primed entity. */
public abstract class BlockTNTBase extends CompatBlock implements IToolable {

    public static final BooleanProperty ARMED = BooleanProperty.create("armed");
    private static final int POP_FUSE = 20;

    protected BlockTNTBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ARMED, false));
        // 1.7 BlockTNTBase: super(Material.tnt, 15, 100, ...) - BlockFlammable encouragement 15, flammability 100
        Fires.setFlammable(this, 15, 100);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMED);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) reactToEnvironment(level, pos, state);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                     boolean movedByPiston) {
        reactToEnvironment(level, pos, state);
    }

    private void reactToEnvironment(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos)) {
            prime(level, pos, state.setValue(ARMED, true), null);
            level.removeBlock(pos, false);
            return;
        }
        if (hasAdjacentFire(level, pos)) {
            // Legacy checkAndIgnite called the armed-on-break path with the current state. Consequently
            // adjacent fire consumes an unarmed block without priming it; retain that odd rule exactly.
            if (state.getValue(ARMED)) prime(level, pos, state, null);
            level.removeBlock(pos, false);
        }
    }

    private static boolean hasAdjacentFire(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.FIRE)) return true;
        }
        return false;
    }

    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(ARMED)) prime(level, pos, state, null);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            prime(level, pos, state.setValue(ARMED, true), player);
            level.removeBlock(pos, false);
            ItemStacks.hurtAndBreak(stack, 1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide() && projectile.isOnFire()) {
            Entity owner = projectile.getOwner();
            prime(level, hit.getBlockPos(), state.setValue(ARMED, true),
                    owner instanceof LivingEntity living ? living : null);
            level.removeBlock(hit.getBlockPos(), false);
        }
    }

    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        Entity source = explosion.getDirectSourceEntity();
        // Vanilla resolves projectile shooters and vanilla PrimedTnt placers through this indirect source.
        LivingEntity owner = explosion.getIndirectSourceEntity();
        if (owner == null && source instanceof EntityPrimedExplosive primed) owner = primed.getOwnerEntity();
        if (owner == null && source instanceof LivingEntity living) owner = living;
        level.removeBlock(pos, false);
        // Below 26.1 vanilla removes the block before wasExploded, so the callback state is AIR. The
        // callback still belongs to this block instance; use its neutral state to retain the exact kind.
        // That callback supplies no pre-removal state, so ARMED necessarily falls back to false there.
        // ARMED only controls later block-break/fire ignition; primed entities and chain detonation use
        // the explosive kind, making the lost bit semantically inert for this callback.
        BlockState explosiveState = state.is(this) ? state : defaultBlockState();
        EntityPrimedExplosive primed = prime(level, pos, explosiveState, owner, false);
        primed.setFuse(level.getRandom().nextInt(POP_FUSE) + POP_FUSE / 2);
        return true;
    }

    protected EntityPrimedExplosive prime(Level level, BlockPos pos, BlockState state,
                                           @Nullable LivingEntity owner) {
        return prime(level, pos, state, owner, true);
    }

    private EntityPrimedExplosive prime(Level level, BlockPos pos, BlockState state,
                                        @Nullable LivingEntity owner, boolean playSound) {
        EntityPrimedExplosive primed = new EntityPrimedExplosive(level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, owner, state);
        level.addFreshEntity(primed);
        if (playSound) {
            level.playSound(null, primed.getX(), primed.getY(), primed.getZ(),
                    SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return primed;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        BlockState state = level.getBlockState(pos);
        if (tool == ToolType.DEFUSER) {
            if (!level.isClientSide()) {
                level.removeBlock(pos, false);
                Block.popResource(level, pos, new ItemStack(this));
            }
            return true;
        }
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!level.isClientSide()) {
            boolean armed = !state.getValue(ARMED);
            level.setBlock(pos, state.setValue(ARMED, armed), Block.UPDATE_ALL);
            player.sendSystemMessage(Component.literal(armed
                    ? "[ Ignite On Break: Enabled ]" : "[ Ignite On Break: Disabled ]")
                    .withStyle(armed ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
        return true;
    }

    public abstract int kindId();

    public abstract String textureName();

    public abstract float blastStrength();

    public void detonate(Level level, double x, double y, double z, EntityPrimedExplosive primed) {
        level.explode(primed, x, y, z, blastStrength(), true, Level.ExplosionInteraction.TNT);
    }
}
