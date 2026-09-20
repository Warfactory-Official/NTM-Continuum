package com.hbm.blocks.generic;

import com.hbm.handler.ArmorUtil;
import com.hbm.lib.ModDamageSources;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.hbm.client.render.SecurityObjRenderer;
import com.hbm.tileentity.TileEntitySecurityObj;

/**
 * Barbed wire, port of 1.7.10's {@code BarbedWire}. No collision box; anything inside is held like a
 * cobweb, slowed further, and takes the variant's contact effect every tick:
 * <ul>
 *   <li>plain — 2 cactus damage</li>
 *   <li>fire — 2 cactus damage and set on fire for one second</li>
 *   <li>poison — 2 cactus damage and Poison III for 5 s</li>
 *   <li>acid — 2 cactus damage and one durability off every worn armour piece (players only)</li>
 *   <li>wither — 2 cactus damage and Wither V for 5 s</li>
 *   <li>ultradeath — 5 pink-cloud (absolute, armour-bypassing) damage and Radiation X for 5 s</li>
 * </ul>
 * The 1.7.10 block stored the placement yaw in its metadata and drew the OBJ rotated by -90°
 * (east/west) or -180° (north/south); here that is the horizontal {@link #FACING}, read by
 * {@code SecurityObjRenderer}. The block itself is invisible — the wire OBJ is drawn by the BER on
 * its {@link TileEntitySecurityObj}, the same invisible-block + renderer route the deco OBJs take.
 */
public class BarbedWire extends CompatBlock implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** The six variants; {@code texture} is the block id, which is also the sprite / {@code _model} skin name. */
    public enum Kind {
        PLAIN("barbed_wire"), FIRE("barbed_wire_fire"), POISON("barbed_wire_poison"), ACID("barbed_wire_acid"),
        WITHER("barbed_wire_wither"), ULTRADEATH("barbed_wire_ultradeath");

        public final String texture;

        Kind(String texture) {
            this.texture = texture;
        }
    }

    private final Kind kind;

    public BarbedWire(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // 1.7.10 setInWeb() (vanilla's 0.25/0.05/0.25 web multiplier) plus an extra slowdown of the motion.
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.15D, motion.y * 0.1D, motion.z * 0.15D);

        if (!(level instanceof ServerLevel server)) return;

        switch (kind) {
            case PLAIN -> Compat.hurt(entity, server, level.damageSources().cactus(), 2.0F);
            case FIRE -> {
                Compat.hurt(entity, server, level.damageSources().cactus(), 2.0F);
                Effects.ignite(entity, 1);
            }
            case POISON -> {
                Compat.hurt(entity, server, level.damageSources().cactus(), 2.0F);
                if (entity instanceof LivingEntity living)
                    Effects.add(living, MobEffects.POISON, 5 * 20, 2);
            }
            case ACID -> {
                Compat.hurt(entity, server, level.damageSources().cactus(), 2.0F);
                if (entity instanceof Player player) {
                    // damageSuit(player, slot, 1) for all four slots
                    ArmorUtil.damageWholeSuit(player, 1);
                }
            }
            case WITHER -> {
                Compat.hurt(entity, server, level.damageSources().cactus(), 2.0F);
                if (entity instanceof LivingEntity living)
                    Effects.add(living, MobEffects.WITHER, 5 * 20, 4);
            }
            case ULTRADEATH -> {
                // ModDamageSource.pc: absolute + bypasses armour, the port's pink_cloud type.
                Compat.hurt(entity, server, ModDamageSources.source(level, ModDamageSources.PINK_CLOUD), 5.0F);
                if (entity instanceof LivingEntity living)
                    Effects.addRegistered(living, ModEffects.RADIATION.get(), 5 * 20, 9);
            }
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySecurityObj(pos, state);
    }

    /**
     * 1.7.10 {@code onBlockPlacedBy}: metadata from the placer's yaw (south→5, west→3, north→4,
     * east→2), which the renderer only ever read as "meta < 4 → -90°, else -180°". Keeping the
     * player's own horizontal direction reproduces both cases exactly.
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
