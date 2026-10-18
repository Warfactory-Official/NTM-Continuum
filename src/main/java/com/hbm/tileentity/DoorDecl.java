package com.hbm.tileentity;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import com.hbm.sound.ModDoorSounds;

/**
 * Port of 1.7.10 {@code com.hbm.tileentity.DoorDecl}: one declaration per generic door, holding its
 * footprint, the block ranges that open and close as it moves, its per-cell bounding boxes, timings,
 * sounds, skins and the keyframe ("SEDNA") animation the client plays.
 *
 * <p>Everything here is plain data usable from common code. What was a GL call in the original
 * ({@code doOffsetTransform}, clip planes, texture bindings) is stated as numbers and identifiers, and
 * the client renderer ({@code com.hbm.client.render.RenderDoorGeneric}) reads them. The original's
 * {@code getSEDNARenderer()} returned a client object; here it is the {@link Renderer} key the client
 * maps onto its per-door drawing routine.
 */
public abstract class DoorDecl {

    /** Which client drawing routine a door uses; {@link #NONE} is the generic OBJ part-transform path. */
    public enum Renderer {
        NONE, VAULT, FIRE, SLIDING_BLAST, SEAL, SECURE, AIRLOCK, SLIDING, CONTAINMENT, WATER, VEHICLE, CARGO
    }

    private static Identifier tex(String path) {
        return Ids.of(Constants.MOD_ID, "textures/models/" + path + ".png");
    }

    private static Identifier obj(String path) {
        return Ids.of(Constants.MOD_ID, "models/" + path + ".obj");
    }

    public static final Identifier TRANSITION_SEAL_TEX = tex("doors/transition_seal");
    public static final Identifier SILO_HATCH_TEX = tex("doors/silo_hatch");
    public static final Identifier SILO_HATCH_LARGE_TEX = tex("doors/silo_hatch_large");
    public static final Identifier SILO_HATCH_OBJ = obj("doors/silo_hatch");
    public static final Identifier SILO_HATCH_LARGE_OBJ = obj("doors/silo_hatch_large");
    /** The Collada seal converted to an OBJ plus an animation sidecar (see {@code port-brief} report). */
    public static final Identifier TRANSITION_SEAL_OBJ = obj("doors/seal");
    public static final Identifier TRANSITION_SEAL_ANIM = Ids.of(Constants.MOD_ID, "models/doors/seal_animation.json");

    public static final Identifier PHEO_FIRE_DOOR_TEX = tex("pheodoors/fire_door");
    public static final Identifier PHEO_FIRE_DOOR_BLACK_TEX = tex("pheodoors/fire_door_black");
    public static final Identifier PHEO_FIRE_DOOR_ORANGE_TEX = tex("pheodoors/fire_door_orange");
    public static final Identifier PHEO_FIRE_DOOR_YELLOW_TEX = tex("pheodoors/fire_door_yellow");
    public static final Identifier PHEO_FIRE_DOOR_TREFOIL_TEX = tex("pheodoors/fire_door_trefoil");
    public static final Identifier PHEO_FIRE_DOOR_OBJ = obj("pheodoors/fire_door");
    public static final Identifier PHEO_AIRLOCK_DOOR_TEX = tex("pheodoors/airlock_door");
    public static final Identifier PHEO_AIRLOCK_DOOR_CLEAN_TEX = tex("pheodoors/airlock_door_clean");
    public static final Identifier PHEO_AIRLOCK_DOOR_GREEN_TEX = tex("pheodoors/airlock_door_green");
    public static final Identifier PHEO_AIRLOCK_DOOR_OBJ = obj("pheodoors/airlock_door");
    public static final Identifier PHEO_BLAST_DOOR_TEX = tex("pheodoors/blast_door");
    public static final Identifier PHEO_BLAST_DOOR_INDUSTRIAL_TEX = tex("pheodoors/blast_door_industrial");
    public static final Identifier PHEO_BLAST_DOOR_OBJ = obj("pheodoors/blast_door");
    public static final Identifier PHEO_CONTAINMENT_DOOR_TEX = tex("pheodoors/containment_door");
    public static final Identifier PHEO_CONTAINMENT_DOOR_TREFOIL_TEX = tex("pheodoors/containment_door_trefoil");
    public static final Identifier PHEO_CONTAINMENT_DOOR_TREFOIL_YELLOW_TEX = tex("pheodoors/containment_door_trefoil_yellow");
    public static final Identifier PHEO_CONTAINMENT_DOOR_OBJ = obj("pheodoors/containment_door");
    public static final Identifier PHEO_SEAL_DOOR_TEX = tex("pheodoors/seal_door");
    public static final Identifier PHEO_SEAL_DOOR_CLEAN_TEX = tex("pheodoors/seal_door_clean");
    public static final Identifier PHEO_SEAL_DOOR_TREFOIL_TEX = tex("pheodoors/seal_door_trefoil");
    public static final Identifier PHEO_SEAL_DOOR_OBJ = obj("pheodoors/seal_door");
    public static final Identifier PHEO_SECURE_DOOR_TEX = tex("pheodoors/secure_door");
    public static final Identifier PHEO_SECURE_DOOR_GREY_TEX = tex("pheodoors/secure_door_grey");
    public static final Identifier PHEO_SECURE_DOOR_BLACK_TEX = tex("pheodoors/secure_door_black");
    public static final Identifier PHEO_SECURE_DOOR_YELLOW_TEX = tex("pheodoors/secure_door_yellow");
    public static final Identifier PHEO_SECURE_DOOR_OBJ = obj("pheodoors/secure_door");
    public static final Identifier PHEO_SLIDING_DOOR_TEX = tex("pheodoors/sliding_door");
    public static final Identifier PHEO_SLIDING_DOOR_BLACK_TEX = tex("pheodoors/sliding_door_black");
    public static final Identifier PHEO_SLIDING_DOOR_GRID_TEX = tex("pheodoors/sliding_door_grid");
    public static final Identifier PHEO_SLIDING_DOOR_PLATE_TEX = tex("pheodoors/sliding_door_plate");
    public static final Identifier PHEO_SLIDING_DOOR_WHITE_TEX = tex("pheodoors/sliding_door_white");
    public static final Identifier PHEO_SLIDING_DOOR_OBJ = obj("pheodoors/sliding_door");
    public static final Identifier PHEO_VEHICLE_DOOR_TEX = tex("pheodoors/vehicle_door");
    public static final Identifier PHEO_VEHICLE_DOOR_BLACK_TEX = tex("pheodoors/vehicle_door_black");
    public static final Identifier PHEO_VEHICLE_DOOR_BLUE_TEX = tex("pheodoors/vehicle_door_blue");
    public static final Identifier PHEO_VEHICLE_DOOR_RED_TEX = tex("pheodoors/vehicle_door_red");
    public static final Identifier PHEO_VEHICLE_DOOR_OBJ = obj("pheodoors/vehicle_door");
    public static final Identifier PHEO_WATER_DOOR_TEX = tex("pheodoors/water_door");
    public static final Identifier PHEO_WATER_DOOR_CLEAN_TEX = tex("pheodoors/water_door_clean");
    public static final Identifier PHEO_WATER_DOOR_OBJ = obj("pheodoors/water_door");
    public static final Identifier PHEO_VAULT_DOOR_3 = tex("pheodoors/vault/vault_door_3");
    public static final Identifier PHEO_VAULT_DOOR_4 = tex("pheodoors/vault/vault_door_4");
    public static final Identifier PHEO_VAULT_DOOR_S = tex("pheodoors/vault/vault_door_s");
    public static final Identifier PHEO_LABEL_2 = tex("pheodoors/vault/label_2");
    public static final Identifier PHEO_LABEL_81 = tex("pheodoors/vault/label_81");
    public static final Identifier PHEO_LABEL_87 = tex("pheodoors/vault/label_87");
    public static final Identifier PHEO_LABEL_99 = tex("pheodoors/vault/label_99");
    public static final Identifier PHEO_LABEL_101 = tex("pheodoors/vault/label_101");
    public static final Identifier PHEO_LABEL_106 = tex("pheodoors/vault/label_106");
    public static final Identifier PHEO_LABEL_111 = tex("pheodoors/vault/label_111");
    public static final Identifier PHEO_VAULT_DOOR_OBJ = obj("pheodoors/vault_door");
    public static final Identifier PHEO_CARGO_DOOR_TEX = tex("pheodoors/cargo_door");
    public static final Identifier PHEO_CARGO_DOOR_OBJ = obj("pheodoors/cargo_door");

    private static final AABB EMPTY = new AABB(0, 0, 0, 0, 0, 0);
    private static final AABB FULL = new AABB(0, 0, 0, 1, 1, 1);

    private static AABB box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new AABB(x0, y0, z0, x1, y1, z1);
    }

    /** {@code Library.smoothstep} from the original. */
    public static float smoothstep(float t, float edge0, float edge1) {
        t = Mth.clamp((t - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static double smoothstep(double t, double edge0, double edge1) {
        t = Mth.clamp((t - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    public static final DoorDecl TRANSITION_SEAL = new DoorDecl() {

        @Override public @Nullable RegistryObject<SoundEvent> getOpenSoundStart() { return ModDoorSounds.TRANSITION_SEAL_OPEN; }
        @Override public float getSoundVolume() { return 6; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if (!partName.equals("base")) {
                set(trans, 0, 3.5F * getNormTime(openTicks), 0);
            } else {
                super.getTranslation(partName, openTicks, child, trans);
            }
        }

        @Override public float[] getOffsetTransform() { return new float[] {0, 0, 0.5F}; }
        @Override public int timeToOpen() { return 480; }

        @Override
        public int[][] getDoorOpenRanges() {
            // 3 is tall
            // 4 is wide
            return new int[][] { { -9, 2, 0, 20, 20, 1 } };
        }

        @Override public int[] getDimensions() { return new int[] { 23, 0, 0, 0, 13, 12 }; }
        @Override public Identifier getTextureForPart(int skinIndex, String partName) { return TRANSITION_SEAL_TEX; }
        /** The animated Collada model; there is no static OBJ path for this door. */
        @Override public boolean hasAnimatedModel() { return true; }
        @Override public @Nullable Identifier getModel() { return null; }
    };

    public static final DoorDecl VAULT_DOOR = new DoorDecl() {

        @Override public Renderer getRenderer() { return Renderer.VAULT; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation()
                    .addBus("PULL", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 0, 1, 2_000, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequence().addPos(0, 0, 0, 2000).addPos(1, 0, 0, 4_000));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation()
                    .addBus("PULL", new BusAnimationSequence().setPos(0, 0, 1).addPos(0, 0, 1, 4_000).addPos(0, 0, 0, 2_000, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequence().setPos(1, 0, 0).addPos(0, 0, 0, 4_000));
            return null;
        }

        @Override public int getSkinCount() { return 7; }
        @Override public int timeToOpen() { return 120; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { {-1, 1, 0, 3, 3, 2} }; }
        @Override public int[] getDimensions() { return new int[] { 4, 0, 0, 0, 2, 2 }; }
        @Override public int[][] getExtraDimensions() { return new int[][] { {0, 0, 1, -1, 2, 2} }; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (!open || y == 0) return FULL;
            else return super.getBlockBound(x, y, z, open, forCollision);
        }

        public final Consumer<TileEntityDoorGeneric> onUpdate = (door) -> {
            if (door.getLevel() == null || door.getLevel().isClientSide()) return;

            if (door.state == TileEntityDoorGeneric.STATE_OPENING) {
                if (door.openTicks == 0) door.playServerSound(ModDoorSounds.VAULT_SCRAPE_NEW.get(), 1.0F, 1.0F);
                for (int i = 45; i <= 115; i += 10)
                    if (door.openTicks == i) door.playServerSound(ModDoorSounds.VAULT_THUD_NEW.get(), 1.0F, 1.0F);
            } else if (door.state == TileEntityDoorGeneric.STATE_CLOSING) {
                if (door.openTicks == 30) door.playServerSound(ModDoorSounds.VAULT_SCRAPE_NEW.get(), 1.0F, 1.0F);
                for (int i = 45; i <= 115; i += 10)
                    if (door.openTicks == i) door.playServerSound(ModDoorSounds.VAULT_THUD_NEW.get(), 1.0F, 1.0F);
            }
        };

        @Override public Consumer<TileEntityDoorGeneric> onDoorUpdate() { return onUpdate; }
    };

    public static final DoorDecl FIRE_DOOR = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.WGH_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.WGH_START; }
        @Override public RegistryObject<SoundEvent> getSoundLoop2() { return ModDoorSounds.ALARM6; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.FIRE; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 1, 0, this.timeToOpen() * 50));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, this.timeToOpen() * 50));
            return null;
        }

        private final Identifier[] skins = {
                PHEO_FIRE_DOOR_TEX, PHEO_FIRE_DOOR_BLACK_TEX, PHEO_FIRE_DOOR_ORANGE_TEX,
                PHEO_FIRE_DOOR_YELLOW_TEX, PHEO_FIRE_DOOR_TREFOIL_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 5; }
        @Override public int timeToOpen() { return 160; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { -1, 0, 0, 3, 4, 1 } }; }
        @Override public int[] getDimensions() { return new int[] { 2, 0, 0, 0, 2, 1 }; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (!open) return FULL;
            if (z == 1) return box(0.5, 0, 0, 1, 1, 1);
            else if (z == -2) return box(0, 0, 0, 0.5, 1, 1);
            else if (y > 1) return box(0, 0.75, 0, 1, 1, 1);
            else if (y == 0) return box(0, 0, 0, 1, forCollision ? 0 : 0.1, 1);
            else return super.getBlockBound(x, y, z, open, forCollision);
        }
    };

    public static final DoorDecl SLIDE_DOOR = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.SLIDING_DOOR_OPENED; }
        @Override public RegistryObject<SoundEvent> getCloseSoundEnd() { return ModDoorSounds.SLIDING_DOOR_SHUT; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.SLIDING_DOOR_OPENING; }
        @Override public RegistryObject<SoundEvent> getSoundLoop2() { return ModDoorSounds.SLIDING_DOOR_OPENING; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.SLIDING_BLAST; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation()
                    .addBus("LOCK", new BusAnimationSequence().setPos(0, 0, 0).addPos(1, 0, 0, 200))
                    .addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 0, 0, 350).addPos(0, 0.05, 0, 200).addPos(0, 1, 0, 650, IType.SIN_UP));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation()
                    .addBus("LOCK", new BusAnimationSequence().setPos(1, 0, 0).addPos(1, 0, 0, 1000).addPos(0, 0, 0, 200))
                    .addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0.05, 0, 650, IType.SIN_UP).addPos(0, 0, 0, 200));
            return null;
        }

        private final Identifier[] skins = { PHEO_BLAST_DOOR_TEX, PHEO_BLAST_DOOR_INDUSTRIAL_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 2; }

        @Override public int timeToOpen() { return 24; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { -2, 0, 0, 4, 5, 1 } }; }
        @Override public int[] getDimensions() { return new int[] { 3, 0, 0, 0, 3, 3 }; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (open) {
                if (y == 3) return box(0, 0.5, 0, 1, 1, 1);
                else if (y == 0) return box(0, 0, 0, 1, forCollision ? 0 : 0.08, 1);
            }
            return super.getBlockBound(x, y, z, open, forCollision);
        }
    };

    public static final DoorDecl SLIDING_SEAL_DOOR = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.SLIDING_SEAL_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundStart() { return ModDoorSounds.SLIDING_SEAL_OPEN; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.SEAL; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 1, 0, this.timeToOpen() * 50));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, this.timeToOpen() * 50));
            return null;
        }

        private final Identifier[] skins = { PHEO_SEAL_DOOR_TEX, PHEO_SEAL_DOOR_CLEAN_TEX, PHEO_SEAL_DOOR_TREFOIL_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 3; }

        @Override public int timeToOpen() { return 20; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (forCollision && open) return EMPTY;
            else return box(0, 0, 1 - 0.25, 1, 1, 1);
        }

        @Override public int[][] getDoorOpenRanges() { return new int[][] { { 0, 0, 0, 1, 2, 2 } }; }
        @Override public int[] getDimensions() { return new int[] { 1, 0, 0, 0, 0, 0 }; }
    };

    public static final DoorDecl SECURE_ACCESS_DOOR = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getCloseSoundLoop() { return ModDoorSounds.GARAGE_MOVE; }
        @Override public RegistryObject<SoundEvent> getCloseSoundEnd() { return ModDoorSounds.GARAGE_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.GARAGE_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.GARAGE_MOVE; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.SECURE; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 1, 0, this.timeToOpen() * 50));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, this.timeToOpen() * 50));
            return null;
        }

        @Override public int timeToOpen() { return 120; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { -2, 1, 0, 4, 5, 1 } }; }
        @Override public int[] getDimensions() { return new int[] { 4, 0, 0, 0, 2, 2 }; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (!open) {
                if (y > 0) return box(0, 0, 0.375, 1, 1, 0.625);
                return FULL;
            }
            if (y == 1) return box(0, 0, 0, 1, forCollision ? 0 : 0.0625, 1);
            else if (y == 4) return box(0, 0.5, 0.15, 1, 1, 0.85);
            else if (y == 0) return FULL;
            else return super.getBlockBound(x, y, z, open, forCollision);
        }

        private final Identifier[] skins = {
                PHEO_SECURE_DOOR_TEX, PHEO_SECURE_DOOR_GREY_TEX, PHEO_SECURE_DOOR_BLACK_TEX, PHEO_SECURE_DOOR_YELLOW_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 4; }
    };

    public static final DoorDecl ROUND_AIRLOCK_DOOR = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.GARAGE_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.GARAGE_MOVE; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.AIRLOCK; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 1, 0, this.timeToOpen() * 50));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, this.timeToOpen() * 50));
            return null;
        }

        private final Identifier[] skins = {
                PHEO_AIRLOCK_DOOR_TEX, PHEO_AIRLOCK_DOOR_CLEAN_TEX, PHEO_AIRLOCK_DOOR_GREEN_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 3; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (!open) return super.getBlockBound(x, y, z, open, forCollision);
            if (z == 1) return box(0.4, 0, 0, 1, 1, 1);
            else if (z == -2) return box(0, 0, 0, 0.6, 1, 1);
            else if (y == 3) return box(0, 0.5, 0, 1, 1, 1);
            else if (y == 0) return box(0, 0, 0, 1, forCollision ? 0 : 0.0625, 1);
            return super.getBlockBound(x, y, z, open, forCollision);
        }

        @Override public int timeToOpen() { return 60; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { 0, 0, 0, -2, 4, 2 }, { 0, 0, 0, 3, 4, 2 } }; }
        @Override public int[] getDimensions() { return new int[] { 3, 0, 0, 0, 2, 1 }; }
    };

    public static final DoorDecl QE_SLIDING = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.QE_SLIDING_OPENED; }
        @Override public RegistryObject<SoundEvent> getCloseSoundEnd() { return ModDoorSounds.QE_SLIDING_SHUT; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.QE_SLIDING_OPENING; }
        @Override public float getSoundVolume() { return 2; }
        @Override public int timeToOpen() { return 10; }
        @Override public Renderer getRenderer() { return Renderer.SLIDING; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 1, 0, this.timeToOpen() * 50));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, this.timeToOpen() * 50));
            return null;
        }

        private final Identifier[] skins = { PHEO_SLIDING_DOOR_TEX, PHEO_SLIDING_DOOR_BLACK_TEX, PHEO_SLIDING_DOOR_GRID_TEX,
                PHEO_SLIDING_DOOR_PLATE_TEX, PHEO_SLIDING_DOOR_WHITE_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 5; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (forCollision && open) {
                if (z == 0) return box(1 - 0.125, 0, 1 - 0.1875, 1, 1, 1);
                else return box(0, 0, 1 - 0.1875, 0.125, 1, 1);
            } else {
                return box(0, 0, 1 - 0.1875, 1, 1, 1);
            }
        }

        @Override public int[][] getDoorOpenRanges() { return new int[][] { { 0, 0, 0, 2, 2, 2 } }; }
        @Override public int[] getDimensions() { return new int[] { 1, 0, 0, 0, 1, 0 }; }
    };

    public static final DoorDecl QE_CONTAINMENT = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.WGH_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.WGH_START; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.CONTAINMENT; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 1, 0, this.timeToOpen() * 50));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, this.timeToOpen() * 50));
            return null;
        }

        private final Identifier[] skins = {
                PHEO_CONTAINMENT_DOOR_TEX, PHEO_CONTAINMENT_DOOR_TREFOIL_TEX, PHEO_CONTAINMENT_DOOR_TREFOIL_YELLOW_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 3; }
        @Override public int timeToOpen() { return 160; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { -1, 0, 0, 3, 3, 1 } }; }
        @Override public int[] getDimensions() { return new int[] { 2, 0, 0, 0, 1, 1 }; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (!open) return box(0, 0, 0.5, 1, 1, 1);
            if (y > 1) return box(0, 0.25, 0.5, 1, 1, 1);
            else if (y == 0) return box(0, 0, 0.5, 1, forCollision ? 0 : 0.125, 1);
            return super.getBlockBound(x, y, z, open, forCollision);
        }
    };

    public static final DoorDecl WATER_DOOR = new DoorDecl() {

        @Override public boolean neverDemandRedstone() { return true; }
        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.WGH_BIG_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.WGH_BIG_START; }
        @Override public RegistryObject<SoundEvent> getOpenSoundStart() { return ModDoorSounds.LEVER; }
        @Override public @Nullable RegistryObject<SoundEvent> getCloseSoundStart() { return null; }
        @Override public RegistryObject<SoundEvent> getCloseSoundEnd() { return ModDoorSounds.LEVER; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.WATER; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation()
                    .addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 0, 0, 1500).addPos(0, 1, 0, 1500, IType.SIN_FULL))
                    .addBus("BOLT", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 0, 1, 1500, IType.SIN_FULL));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation()
                    .addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, 1500, IType.SIN_FULL))
                    .addBus("BOLT", new BusAnimationSequence().setPos(0, 0, 1).addPos(0, 0, 1, 1200).addPos(0, 0, 0, 1500, IType.SIN_FULL));
            return null;
        }

        private final Identifier[] skins = { PHEO_WATER_DOOR_TEX, PHEO_WATER_DOOR_CLEAN_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 2; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (!open) return box(0, 0, 0.75, 1, 1, 1);
            else if (y > 1) return box(0, 0.85, 0.75, 1, 1, 1);
            else if (y == 0) return box(0, 0, 0.75, 1, forCollision ? 0 : 0.15, 1);
            return super.getBlockBound(x, y, z, open, forCollision);
        }

        @Override public int timeToOpen() { return 60; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { 1, 0, 0, -3, 3, 2 } }; }
        @Override public float getDoorRangeOpenTime(int ticks, int idx) { return getNormTime(ticks, 35, 40); }
        @Override public int[] getDimensions() { return new int[] { 2, 0, 0, 0, 1, 1 }; }
    };

    public static final DoorDecl SILO_HATCH = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.WGH_BIG_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.WGH_BIG_START; }
        @Override public @Nullable RegistryObject<SoundEvent> getOpenSoundStart() { return null; }
        @Override public @Nullable RegistryObject<SoundEvent> getCloseSoundStart() { return null; }
        @Override public RegistryObject<SoundEvent> getCloseSoundEnd() { return ModDoorSounds.WGH_BIG_STOP; }
        @Override public float getSoundVolume() { return 2; }
        @Override public boolean remoteControllable() { return true; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("Hatch".equals(partName)) {
                set(trans, 0, 0.25F * smoothstep(getNormTime(openTicks, 0, 10), 0, 1), 0);
            } else {
                set(trans, 0, 0, 0);
            }
        }

        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("Hatch".equals(partName)) {
                set(orig, 0F, 0.875F, -1.875F);
                return;
            }
            set(orig, 0, 0, 0);
            super.getOrigin(partName, orig);
        }

        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if ("Hatch".equals(partName)) {
                set(rot, smoothstep(getNormTime(openTicks, 20, 100), 0, 1) * -240, 0, 0);
                return;
            }
            super.getRotation(partName, openTicks, rot);
        }

        @Override public int timeToOpen() { return 60; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { 1, 0, 1, -3, 3, 0 }, { 0, 0, 1, -3, 3, 0 }, { -1, 0, 1, -3, 3, 0 } }; }
        @Override public float getDoorRangeOpenTime(int ticks, int idx) { return getNormTime(ticks, 20, 20); }
        @Override public int getBlockOffset() { return 2; }
        @Override public int[] getDimensions() { return new int[] { 0, 0, 2, 2, 2, 2 }; }
        @Override public Identifier getTextureForPart(int skinIndex, String partName) { return SILO_HATCH_TEX; }
        @Override public Identifier getModel() { return SILO_HATCH_OBJ; }
    };

    public static final DoorDecl SILO_HATCH_LARGE = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.WGH_BIG_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.WGH_BIG_START; }
        @Override public @Nullable RegistryObject<SoundEvent> getOpenSoundStart() { return null; }
        @Override public @Nullable RegistryObject<SoundEvent> getCloseSoundStart() { return null; }
        @Override public RegistryObject<SoundEvent> getCloseSoundEnd() { return ModDoorSounds.WGH_BIG_STOP; }
        @Override public float getSoundVolume() { return 2; }
        @Override public boolean remoteControllable() { return true; }

        @Override
        public void getTranslation(String partName, float openTicks, boolean child, float[] trans) {
            if ("Hatch".equals(partName)) {
                set(trans, 0, 0.25F * smoothstep(getNormTime(openTicks, 0, 10), 0, 1), 0);
            } else {
                set(trans, 0, 0, 0);
            }
        }

        @Override
        public void getOrigin(String partName, float[] orig) {
            if ("Hatch".equals(partName)) {
                set(orig, 0F, 0.875F, -2.875F);
                return;
            }
            set(orig, 0, 0, 0);
            super.getOrigin(partName, orig);
        }

        @Override
        public void getRotation(String partName, float openTicks, float[] rot) {
            if ("Hatch".equals(partName)) {
                set(rot, smoothstep(getNormTime(openTicks, 20, 100), 0, 1) * -240, 0, 0);
                return;
            }
            super.getRotation(partName, openTicks, rot);
        }

        @Override public int timeToOpen() { return 60; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { 2, 0, 1, -3, 3, 0 }, { 1, 0, 2, -5, 3, 0 }, { 0, 0, 2, -5, 3, 0 }, { -1, 0, 2, -5, 3, 0 }, { -2, 0, 1, -3, 3, 0 } }; }
        @Override public float getDoorRangeOpenTime(int ticks, int idx) { return getNormTime(ticks, 20, 20); }
        @Override public int getBlockOffset() { return 3; }
        @Override public int[] getDimensions() { return new int[] { 0, 0, 3, 3, 3, 3 }; }
        @Override public Identifier getTextureForPart(int skinIndex, String partName) { return SILO_HATCH_LARGE_TEX; }
        @Override public Identifier getModel() { return SILO_HATCH_LARGE_OBJ; }
    };

    public static final DoorDecl LARGE_VEHICLE_DOOR = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.GARAGE_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.GARAGE_MOVE; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.VEHICLE; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 1, 0, this.timeToOpen() * 50));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation().addBus("DOOR", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, this.timeToOpen() * 50));
            return null;
        }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (!open) return super.getBlockBound(x, y, z, open, forCollision);
            if (z == 3) return box(0.4, 0, 0, 1, 1, 1);
            else if (z == -3) return box(0, 0, 0, 0.6, 1, 1);
            else if (y == 0) return box(0, 0, 0, 1, forCollision ? 0 : 0.0625, 1);
            return super.getBlockBound(x, y, z, open, forCollision);
        }

        private final Identifier[] skins = { PHEO_VEHICLE_DOOR_TEX, PHEO_VEHICLE_DOOR_BLACK_TEX, PHEO_VEHICLE_DOOR_BLUE_TEX,
                PHEO_VEHICLE_DOOR_RED_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 4; }

        @Override public int timeToOpen() { return 60; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { 0, 0, 0, -4, 6, 2 }, { 0, 0, 0, 4, 6, 2 } }; }
        @Override public int[] getDimensions() { return new int[] { 5, 0, 0, 0, 3, 3 }; }
    };

    public static final DoorDecl CARGO_DOOR = new DoorDecl() {

        @Override public RegistryObject<SoundEvent> getCloseSoundLoop() { return ModDoorSounds.GARAGE_MOVE; }
        @Override public RegistryObject<SoundEvent> getCloseSoundEnd() { return ModDoorSounds.GARAGE_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundEnd() { return ModDoorSounds.GARAGE_STOP; }
        @Override public RegistryObject<SoundEvent> getOpenSoundLoop() { return ModDoorSounds.GARAGE_MOVE; }
        @Override public float getSoundVolume() { return 2; }
        @Override public Renderer getRenderer() { return Renderer.CARGO; }

        @Override
        public BusAnimation getBusAnimation(byte state, byte skinIndex) {
            int half = this.timeToOpen() * 25;
            if (state == TileEntityDoorGeneric.STATE_OPENING) return new BusAnimation()
                    .addBus("BOT", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 1, 0, half * 2))
                    .addBus("TOP", new BusAnimationSequence().setPos(0, 0, 0).addPos(0, 0, 0, half).addPos(0, 1, 0, half));
            if (state == TileEntityDoorGeneric.STATE_CLOSING) return new BusAnimation()
                    .addBus("BOT", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 0, 0, half * 2))
                    .addBus("TOP", new BusAnimationSequence().setPos(0, 1, 0).addPos(0, 1, 0, half).addPos(0, 0, 0, half));
            return null;
        }

        @Override public int timeToOpen() { return 60; }
        @Override public int[][] getDoorOpenRanges() { return new int[][] { { -1, -1, 0, 3, 3, 1 } }; }
        @Override public int[] getDimensions() { return new int[] { 2, 0, 0, 0, 1, 1 }; }

        @Override
        public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
            if (!open) return box(0, 0, 0.375, 1, 1, 0.625);
            if (y > 1) return box(0, 0.25, 0.375, 1, 1, 0.625);
            else if (y == 0) return box(0, 0, 0.375, 1, forCollision ? 0 : 0.125, 0.625);
            return super.getBlockBound(x, y, z, open, forCollision);
        }

        private final Identifier[] skins = { PHEO_CARGO_DOOR_TEX };

        @Override public Identifier[] getSEDNASkins() { return skins; }
        @Override public int getSkinCount() { return 1; }
    };

    /**
     * Format: x, y, z, tangent amount 1 (how long the door would be if it moved up), tangent amount 2
     * (door places blocks in this direction), axis (0-x, 1-y, 2-z). Kept exactly as the original
     * documents it, oddities included: the sign of {@code [3]} is the direction along the axis.
     */
    public abstract int[][] getDoorOpenRanges();
    public abstract int[] getDimensions();
    public int[][] getExtraDimensions() { return null; }

    public int getBlockOffset() { return 0; }
    public boolean remoteControllable() { return false; }

    public float getDoorRangeOpenTime(int ticks, int idx) {
        return getNormTime(ticks);
    }

    public int timeToOpen() { return 20; }

    public float getNormTime(float time) {
        return getNormTime(time, 0, timeToOpen());
    }

    /** {@code BobMathUtil.remap01_clamp}. */
    public float getNormTime(float time, float min, float max) {
        return Mth.clamp((time - min) / (max - min), 0, 1);
    }

    public @Nullable Identifier getTextureForPart(String partName) {
        return getTextureForPart(0, partName);
    }

    public @Nullable Identifier getTextureForPart(int skinIndex, String partName) { return null; }
    /** The OBJ drawn by the generic part-transform path, or null. */
    public @Nullable Identifier getModel() { return null; }
    /** True for the transition seal, whose model is the animated Collada scene. */
    public boolean hasAnimatedModel() { return false; }

    public void getTranslation(String partName, float openTicks, boolean child, float[] trans) { set(trans, 0, 0, 0); }
    public void getRotation(String partName, float openTicks, float[] rot) { set(rot, 0, 0, 0); }
    public void getOrigin(String partName, float[] orig) { set(orig, 0, 0, 0); }
    public boolean doesRender(String partName, boolean child) { return true; }

    private static final String[] NOTHING = new String[] {};

    public String[] getChildren(String partName) { return NOTHING; }
    /** Clip planes as {@code a, b, c, d} rows in model space, applied by the generic OBJ path. */
    public double[][] getClippingPlanes() { return new double[][] {}; }
    /** The translation the original's {@code doOffsetTransform} applied before drawing. */
    public float[] getOffsetTransform() { return new float[] {0, 0, 0}; }

    public AABB getBlockBound(int x, int y, int z, boolean open, boolean forCollision) {
        return open ? EMPTY : FULL;
    }

    /** Hand-operated even under {@code ULTRA_LARP_MODE}. */
    public boolean neverDemandRedstone() { return false; }
    public boolean isLadder(boolean open) { return false; }
    public @Nullable RegistryObject<SoundEvent> getOpenSoundLoop() { return null; }

    // Hack
    public @Nullable RegistryObject<SoundEvent> getSoundLoop2() { return null; }
    public @Nullable RegistryObject<SoundEvent> getCloseSoundLoop() { return getOpenSoundLoop(); }
    public @Nullable RegistryObject<SoundEvent> getOpenSoundStart() { return null; }
    public @Nullable RegistryObject<SoundEvent> getCloseSoundStart() { return getOpenSoundStart(); }
    public @Nullable RegistryObject<SoundEvent> getOpenSoundEnd() { return null; }
    public @Nullable RegistryObject<SoundEvent> getCloseSoundEnd() { return getOpenSoundEnd(); }

    public float getSoundVolume() { return 1; }

    public SoundSource getSoundSource() { return SoundSource.BLOCKS; }

    public float[] set(float[] f, float x, float y, float z) {
        f[0] = x;
        f[1] = y;
        f[2] = z;
        return f;
    }

    public Identifier @Nullable [] getSEDNASkins() { return null; }
    public boolean hasSkins() { return getSkinCount() > 0; }

    public int getSkinCount() { return 0; }

    /** The original's inventory preview cycles the skins once per second on the wall clock. */
    public Identifier getCyclingSkins(long millis) {
        Identifier[] skins = this.getSEDNASkins();
        int index = (int) ((millis % (skins.length * 1000L)) / 1000L);
        return skins[index];
    }

    public Identifier getSkinFromIndex(int index) {
        Identifier[] skins = this.getSEDNASkins();
        return skins[Math.abs(index) % skins.length];
    }

    // keyframe animation system sneakily stitched into the door decl
    public Renderer getRenderer() { return Renderer.NONE; }
    public @Nullable BusAnimation getBusAnimation(byte state, byte skinIndex) { return null; }

    public @Nullable Consumer<TileEntityDoorGeneric> onDoorUpdate() { return null; }
}
