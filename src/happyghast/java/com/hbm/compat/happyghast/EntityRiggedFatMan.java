package com.hbm.compat.happyghast;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.bomb.NukeMan;
import com.hbm.tileentity.bomb.TileEntityNukeMan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A fully assembled Fat Man lifted off its block so a happy ghast can carry it, the way 26.1 lets a happy ghast carry a
 * leashed boat. Port-only content (no 1.7 counterpart): it exists only on the nodes with happy ghasts.
 *
 * <p>Life cycle:
 * <ol>
 *   <li>{@link HbmHappyGhast#tryRig}: sneak + right-click with a lead on a ready {@code nuke_man} block turns it into
 *       this entity, carrying the block's six parts, placer and facing, and already on the player's lead.
 *   <li>Vanilla does the rest of the carrying: sneak-using the happy ghast moves the lead over
 *       ({@code Entity.interact}), and {@link Leashable#tickLeash} pulls it along on four ropes, as for a boat.
 *   <li>When a happy ghast loses it (shears on the bomb or the ghast, the lead snapping, the ghast dying) it
 *       {@linkplain #arm() arms} and falls by the custom nuke's drop rule ({@code EntityFallingNuke}, the
 *       {@code custom_fall} upgrade), detonating as the Fat Man ({@link NukeMan#detonate}) where it lands.
 *   <li>Released by anything else (a player, a fence knot, a ghast merely unloaded) it stays a harmless carried bomb;
 *       sneak + empty hand on it while it is not leashed sets it back down as the block, parts intact.
 * </ol>
 */
public class EntityRiggedFatMan extends Entity implements Leashable {

    /** Whether the bomb is armed and falling; the renderer tips it nose-down while this is set. */
    private static final EntityDataAccessor<Boolean> FALLING =
            SynchedEntityData.defineId(EntityRiggedFatMan.class, EntityDataSerializers.BOOLEAN);

    /** The Fat Man's six slots: igniter, four explosive lenses, core ({@link TileEntityNukeMan}). */
    static final int SLOTS = 6;

    /**
     * The model's turn inside the entity frame: +90 degrees about Y puts the OBJ's nose (-X) on the entity's +Z, which
     * is forward for every entity (yRot 0 looks along +Z). A happy ghast's harness corners and these rope ends share the
     * boat's layout ({@link Leashable#createQuadLeashOffsets}: length on Z, width on X), so the ropes settle the bomb
     * with its yaw on the ghast's and the nose facing the way the ghast faces. The stored heading takes the turn
     * back off ({@link #yRotForModelYaw}), so lifting the bomb off its block does not move it.
     */
    public static final float MODEL_YAW = 90F;

    /**
     * Rope ends on the bomb, in the entity frame and in {@link Leashable#createQuadLeashOffsets}' corner order
     * (left-front, left-back, right-back, right-front), so each rope runs to the matching harness corner and none
     * cross: 0.4 to either side and 0.6 fore and aft of the middle, on top of the body (its top is at y 1.59).
     */
    private static final Vec3[] QUAD_OFFSETS = {
            new Vec3(-0.4, 1.45, 0.6), new Vec3(-0.4, 1.45, -0.6),
            new Vec3(0.4, 1.45, -0.6), new Vec3(0.4, 1.45, 0.6)
    };
    private static final Vec3 LEASH_OFFSET = new Vec3(0.0, 1.5, 0.0);

    /** 1.7 EntityFallingNuke.rotation(): two degrees a tick; it stops 75 degrees past its start. */
    private static final float TIP_PER_TICK = 2F;
    private static final float TIP_MAX = 75F;

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private Leashable.@Nullable LeashData leashData;
    private final NonNullList<ItemStack> parts = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private @Nullable UUID placer;
    private @Nullable UUID detonator;
    private @Nullable UUID shearer;

    public EntityRiggedFatMan(EntityType<? extends EntityRiggedFatMan> type, Level level) {
        super(type, level);
    }

    /**
     * The bomb lifted off a {@code nuke_man} block.
     *
     * @param modelYaw the block's model yaw ({@link #yawForFacing}); see {@link #yRotForModelYaw}
     */
    EntityRiggedFatMan(Level level, List<ItemStack> parts, @Nullable UUID placer, float modelYaw) {
        this(HbmHappyGhast.RIGGED_FAT_MAN.get(), level);
        for (int i = 0; i < SLOTS && i < parts.size(); i++) this.parts.set(i, parts.get(i).copy());
        this.placer = placer;
        setYRot(yRotForModelYaw(modelYaw));
        this.yRotO = getYRot();
    }

    // Block <-> entity

    /** {@code BombObjRenderer}'s Fat Man yaw map: legacy facing 2 -> 270, 3 -> 90, 4 -> 0, 5 -> 180. */
    static float yawForFacing(int facing) {
        return switch (facing) {
            case 3 -> 90F;
            case 4 -> 0F;
            case 5 -> 180F;
            default -> 270F;
        };
    }

    /**
     * The heading that draws the model at the block's yaw. The renderer turns by {@code -yRot} (the turn vanilla gives
     * the leash attachment points, so the ropes stay on the body as it swings) and then by {@link #MODEL_YAW}.
     */
    static float yRotForModelYaw(float modelYaw) {
        return Mth.wrapDegrees(MODEL_YAW - modelYaw); // drawn at -yRot + MODEL_YAW = modelYaw
    }

    /** The legacy facing whose model yaw is nearest to this bomb's current heading. */
    int nearestFacing() {
        int quarter = Math.floorMod(Math.round(Mth.wrapDegrees(MODEL_YAW - getYRot()) / 90F), 4);
        return switch (quarter) {
            case 1 -> 3;   // 90
            case 2 -> 5;   // 180
            case 3 -> 2;   // 270
            default -> 4;  // 0
        };
    }

    /** Sets the bomb back down as its block, parts and placer intact. False when there is no room. */
    boolean placeAsBlock() {
        BlockPos pos = blockPosition();
        if (!level().getBlockState(pos).canBeReplaced()) pos = pos.above();
        if (!level().getBlockState(pos).canBeReplaced()) return false;

        BlockState state = ModBombBlocks.nuke_man.get().defaultBlockState().setValue(NukeMan.FACING, nearestFacing());
        if (!level().setBlock(pos, state, 3)) return false;
        if (level().getBlockEntity(pos) instanceof TileEntityNukeMan nuke) {
            for (int i = 0; i < SLOTS; i++) nuke.setItem(i, parts.get(i).copy());
            nuke.placerID = placer;
            nuke.setChanged();
        }
        parts.clear();
        level().playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
        discard();
        return true;
    }

    // Falling (the custom nuke's drop)

    public boolean isFalling() {
        return entityData.get(FALLING);
    }

    /** Lets go of the bomb as a drop: it falls and detonates on the first non-air block. */
    void arm() {
        if (isFalling()) return;
        entityData.set(FALLING, true);
        detonator = shearer != null ? shearer : placer;
        setOnGround(false);
    }

    /**
     * 1.7 EntityFallingNuke.onUpdate, unchanged: no collision, horizontal drag 0.99, gravity 0.05 with a terminal
     * speed of 1, and detonation the moment its own block position is not air. It keeps the swing it had under the
     * ghast, as that bomb kept whatever motion it was given.
     */
    private void tickFalling() {
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.x * 0.99D, Math.max(motion.y - 0.05D, -1.0D), motion.z * 0.99D);

        // EntityFallingNuke.rotation(): the Little Boy tipped two degrees a tick; the Fat Man hangs level, so it starts
        // level and noses over by the same rate and the same 75 degree limit instead of 1.7's 90 -> -75 sweep
        if (getXRot() < TIP_MAX) setXRot(Math.min(TIP_MAX, getXRot() + TIP_PER_TICK));

        if (!level().getBlockState(BlockPos.containing(getX(), getY(), getZ())).isAir()) {
            parts.clear();
            NukeMan.detonate(level(), getX(), getY(), getZ(), detonator);
            discard();
        }
    }

    // Carried

    /** Boat-like in air (AbstractBoat.floatBoat, IN_AIR): gravity 0.04 and horizontal drag 0.9; it sinks in water. */
    private void tickCarried() {
        applyGravity();
        move(MoverType.SELF, getDeltaMovement());
        Vec3 motion = getDeltaMovement();
        if (isInWater()) {
            setDeltaMovement(motion.scale(0.8D));
        } else if (onGround()) {
            setDeltaMovement(motion.x * 0.5D, motion.y, motion.z * 0.5D);
        } else {
            setDeltaMovement(motion.x * 0.9D, motion.y, motion.z * 0.9D);
        }
    }

    @Override
    public void tick() {
        super.tick(); // baseTick runs the vanilla leash: restore, snap, elastic pull, rope torque
        interpolation.interpolate();
        if (level().isClientSide()) return;

        if (isFalling()) {
            tickFalling();
        } else {
            tickCarried();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04D;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // Leash

    @Override
    public Leashable.@Nullable LeashData getLeashData() {
        return leashData;
    }

    /**
     * Every way a leash ends goes through here with {@code null} while the old holder is still known, so this is where
     * a happy ghast losing the bomb becomes a drop. A ghast that was only unloaded (logged out with its rider, chunk
     * unloaded) is not a release, and neither is this bomb's own removal.
     */
    @Override
    public void setLeashData(Leashable.@Nullable LeashData leashData) {
        Leashable.LeashData old = this.leashData;
        this.leashData = leashData;
        if (leashData == null && old != null && old.leashHolder instanceof HappyGhast ghast
                && !level().isClientSide() && !isRemoved() && releasedForGood(ghast)) {
            arm();
        }
    }

    private static boolean releasedForGood(Entity holder) {
        Entity.RemovalReason reason = holder.getRemovalReason();
        return reason == null || reason.shouldDestroy();
    }

    /** Shears used on the bomb itself: whoever cut it loose is the detonator. */
    @Override
    public boolean shearOffAllLeashConnections(@Nullable Player player) {
        if (player != null && isLeashed()) shearer = player.getUUID();
        return super.shearOffAllLeashConnections(player);
    }

    /** Not while it is falling: a dropped bomb cannot be caught again. */
    @Override
    public boolean canBeLeashed() {
        return !isFalling();
    }

    @Override
    public boolean supportQuadLeash() {
        return true;
    }

    @Override
    public Vec3[] getQuadLeashOffsets() {
        return QUAD_OFFSETS.clone();
    }

    @Override
    public Vec3 getLeashOffset() {
        return LEASH_OFFSET;
    }

    // Interaction

    /**
     * Sneak + empty hand on a loose bomb sets it back down as the block. Any other sneak use is swallowed, so the bomb
     * never becomes a leash holder for the player's other leashed mobs (vanilla's first branch). Leads, shears and
     * untying are vanilla.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive() && !player.getItemInHand(hand).is(Items.SHEARS)) {
            if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            if (isFalling()) return InteractionResult.PASS;
            if (isLeashed()) {
                player.sendOverlayMessage(Component.translatable("hbm.rigged_fat_man.leashed"));
                return InteractionResult.FAIL;
            }
            if (!placeAsBlock()) {
                player.sendOverlayMessage(Component.translatable("hbm.rigged_fat_man.no_room"));
                return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Like the block, it does not go off when hit; it is moved only by its leash. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D; // 1.7 EntityFallingNuke.isInRangeToRenderDist
    }

    // Data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FALLING, false);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        List<ItemStack> saved = input.read("parts", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < SLOTS; i++) parts.set(i, i < saved.size() ? saved.get(i) : ItemStack.EMPTY);
        placer = input.read("placer", UUIDUtil.CODEC).orElse(null);
        detonator = input.read("detonator", UUIDUtil.CODEC).orElse(null);
        entityData.set(FALLING, input.getBooleanOr("falling", false));
        readLeashData(input);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("parts", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(parts));
        output.storeNullable("placer", UUIDUtil.CODEC, placer);
        output.storeNullable("detonator", UUIDUtil.CODEC, detonator);
        output.putBoolean("falling", isFalling());
        writeLeashData(output, leashData);
    }

    /** The carried parts, for the GameTests. */
    List<ItemStack> parts() {
        return List.copyOf(parts);
    }
}
