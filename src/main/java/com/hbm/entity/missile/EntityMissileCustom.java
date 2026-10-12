package com.hbm.entity.missile;

import com.hbm.entity.ModNtmEntities;
import com.hbm.blocks.bomb.ModBombBlocks;
import dev.golem.core.data.SynchedData;
import com.hbm.blocks.bomb.BlockTaint;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.client.ClientEffects;
import com.hbm.handler.MissileStruct;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart.WarheadType;
import com.hbm.items.weapon.sedna.factory.XFactoryWarhead;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 entity/missile/EntityMissileCustom - modular missile assembled from custom parts (warhead/fuselage/fins/thruster);
 * flight math lives on the delta-ported base, this class carries fuel burn plus the per-warhead impact table.
 *
 * 1.7 synced legacy item ids on datawatcher slots 9-12; here the part ItemStacks ride the synched data directly
 * (numeric ids are not stable across 26.x registry reloads).
 */
public class EntityMissileCustom extends EntityMissileBaseNT {

    // 1.7 datawatcher slots 9/10/11/12
    private static final EntityDataAccessor<ItemStack> WARHEAD =
            SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> FUSELAGE =
            SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> FINS =
            SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> THRUSTER =
            SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.ITEM_STACK);

    public float fuel;
    public float consumption;

    public EntityMissileCustom(EntityType<?> type, Level level) {
        super(type, level);
        // 1.7: ignoreFrustumCheck = true - renderer-side on 26.x; shouldRenderAtSqrDistance covers culling
    }

    /** This port's launch constructor: the base does the position/motion/vector math. */
    public EntityMissileCustom(Level level, float x, float y, float z, int targetX, int targetZ,
                               ItemStack warhead, ItemStack fuselage, ItemStack fins, ItemStack thruster) {
        super(ModNtmEntities.MISSILE_CUSTOM.get(), level, x, y, z, targetX, targetZ);
        installParts(warhead, fuselage, fins, thruster);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(WARHEAD, ItemStack.EMPTY);
        builder.define(FUSELAGE, ItemStack.EMPTY);
        builder.define(FINS, ItemStack.EMPTY);
        builder.define(THRUSTER, ItemStack.EMPTY);
    }

    /**
     * 1.7 spawn ctor (World, x, y, z, targetX, targetZ, MissileStruct template); position/motion/vector math is
     * identical to every other missile and lives on the base launch(). Part stacks land on the synched slots.
     */
    private void installParts(ItemStack warhead, ItemStack fuselage, ItemStack fins, ItemStack thruster) {
        setParts(warhead, fuselage, fins, thruster);

        ItemCustomMissilePart fuselagePart = getPart(FUSELAGE);
        ItemCustomMissilePart thrusterPart = getPart(THRUSTER);
        this.fuel = fuselagePart != null ? fuselagePart.fuelAmount() : 0F;
        this.consumption = thrusterPart != null ? thrusterPart.consumption() : 0F;
        // 1.7: setSize(1.5F, 1.5F) - belongs on the EntityType registration (.sized(1.5F, 1.5F))
    }

    /** The four slots without a launch behind them, for the assembly rack's unspawned GUI preview. */
    public void setParts(ItemStack warhead, ItemStack fuselage, ItemStack fins, ItemStack thruster) {
        entityData.set(WARHEAD, warhead == null ? ItemStack.EMPTY : warhead);
        entityData.set(FUSELAGE, fuselage == null ? ItemStack.EMPTY : fuselage);
        entityData.set(FINS, fins == null ? ItemStack.EMPTY : fins);
        entityData.set(THRUSTER, thruster == null ? ItemStack.EMPTY : thruster);
    }

    private ItemCustomMissilePart getPart(EntityDataAccessor<ItemStack> slot) {
        return ItemCustomMissilePart.part(entityData.get(slot));
    }

    /**
     * The four synched slots as one struct. Raw, not {@link MissileStruct#sanitised()}: 1.7's renderer gated slot
     * type inside the pronter, so a misplaced part drops out of the draw alone.
     */
    public MissileStruct getStruct() {
        return new MissileStruct(getPart(WARHEAD), getPart(FUSELAGE), getPart(FINS), getPart(THRUSTER));
    }

    /** The assembled warhead's type, or null when the missile carries no warhead part. */
    public WarheadType getWarheadType() {
        ItemCustomMissilePart part = getPart(WARHEAD);
        return part != null ? part.warheadType() : null;
    }

    public float getWarheadStrength() {
        ItemCustomMissilePart part = getPart(WARHEAD);
        return part != null ? part.warheadStrength() : 0F;
    }

    @Override
    public boolean hasPropulsion() {
        return this.fuel > 0;
    }

    @Override
    public void tick() {

        // 1.7 onUpdate head: run the warhead trait hook before movement, both sides
        WarheadType warhead = getWarheadType();
        if(warhead != null && warhead.updateCustom != null) {
            warhead.updateCustom.accept(this);
        }

        if(!level().isClientSide()) {
            if(this.hasPropulsion()) this.fuel -= this.consumption;
        }

        super.tick();
    }

    // 1.7 killMissile override was explode(5) + shrapnel shower with no extra debris; the ported base does exactly
    // that and getDebris()/getDebrisRareDrop() below stay empty, so no override needed.

    @Override
    protected void spawnContrail() {
        // this port's base has no step argument; the tail runs back along the last tick's displacement
        Vec3 step = new Vec3(getX() - xo, getY() - yo, getZ() - zo);
        Vec3 v = step.lengthSqr() < 1.0e-8 ? new Vec3(0, -1, 0) : step.normalize();

        ItemCustomMissilePart fuselage = getPart(FUSELAGE);
        FuelType fuel = fuselage != null ? fuselage.fuelType() : null;
        // 1.7's ion drive is the one fuel with no visible plume at all; 1.7 cannot reach a null fuselage here
        if(fuel == null) return;

        ClientEffects.Contrail contrail = fuel.contrail; // 1.7: kerosene/solid/hydrogen/balefire, xenon none
        if(contrail == null) return;

        // 1.7 iterated i < velocity; flight is server-authoritative now (this.velocity stays client-zero), so the tail
        // length comes off the tick step like the base contrail does.
        double len = Math.max(step.length(), 1D);
        int count = (int) Math.min(len, 10);
        for(int i = 0; i < count; i++) {
            ClientEffects.spawnContrail(level(), getX() - v.x * i, getY() - v.y * i, getZ() - v.z * i, contrail);
        }
    }

    /**
     * 1.7's Torex factories spawned the cloud themselves; the port's only build it. 1.7's custom missile
     * did not consult enableNukeClouds. Each child claims its chunk before this missile's ticket goes.
     */
    private void spawnCloud(EntityNukeTorex torex) {
        if (level().addFreshEntity(torex)) torex.startChunkLoading();
    }

    private void spawnBlast(EntityNukeExplosionMK5 blast) {
        if (level().addFreshEntity(blast)) blast.startChunkLoading();
    }

    // 1.7 EntityMissileCustom:165, verbatim:
    @Override
    public void onMissileImpact(HitResult mop) { //TODO: demolish this steaming pile of shit

        WarheadType type = getWarheadType();
        float strength = getWarheadStrength();

        if(type == null) return;

        if(type.impactCustom != null) {
            type.impactCustom.accept(this);
            return;
        }

        if (type == WarheadType.HE) {
            ExplosionLarge.explode(level(), getX(), getY(), getZ(), strength, true, false, true);
            ExplosionLarge.jolt(level(), getX(), getY(), getZ(), strength, (int) (strength * 50), 0.25);
        } else if (type == WarheadType.INC) {
            ExplosionLarge.explodeFire(level(), getX(), getY(), getZ(), strength, true, false, true);
            ExplosionLarge.jolt(level(), getX(), getY(), getZ(), strength * 1.5, (int) (strength * 50), 0.25);
        } else if (type == WarheadType.CLUSTER) {
            // 1.7: no impact effect
        } else if (type == WarheadType.BUSTER) {
            ExplosionLarge.buster(level(), getX(), getY(), getZ(),
                    new Vec3(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z),
                    strength, (int) (strength * 4));
        } else if (type == WarheadType.NUCLEAR || type == WarheadType.TX) {
            spawnBlast(EntityNukeExplosionMK5.statFac(level(), (int) strength, getX(), getY(), getZ()));
            spawnCloud(EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), strength));
        } else if (type == WarheadType.BALEFIRE) {
            EntityBalefire bf = new EntityBalefire(ModNtmEntities.BALEFIRE.get(), level());
            bf.setPos(getX(), getY(), getZ());
            bf.destructionRange = (int) strength;
            level().addFreshEntity(bf);
            spawnCloud(EntityNukeTorex.statFacBale(level(), getX(), getY(), getZ(), strength));
        } else if (type == WarheadType.N2) {
            spawnBlast(EntityNukeExplosionMK5.statFacNoRad(level(), (int) strength, getX(), getY(), getZ()));
            spawnCloud(EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), strength));
        } else if (type == WarheadType.TAINT) {
            int r = (int) strength;
            // 1.7: nextInt(r) - (r/2 - 1) on (int)-truncated coords - a window r wide, centered half a radius out
            for (int i = 0; i < r * 10; i++) {
                BlockPos pos = new BlockPos(
                        random.nextInt(r) + (int) getX() - (r / 2 - 1),
                        random.nextInt(r) + (int) getY() - (r / 2 - 1),
                        random.nextInt(r) + (int) getZ() - (r / 2 - 1));
                BlockState hit = level().getBlockState(pos);
                if (!dev.golem.core.world.Compat.isSolidRender(hit) || hit.isAir()) continue;
                level().setBlock(pos, ModBombBlocks.taint.get().defaultBlockState()
                        .setValue(BlockTaint.TAINTAGE, random.nextInt(3) + 4), Block.UPDATE_CLIENTS);
            }
        } else if (type == WarheadType.CLOUD) {
            // 1.7 playAuxSFX(2002): splash-potion break FX
            level().levelEvent(2002, BlockPos.containing(getX(), getY(), getZ()), 0);
            ExplosionChaos.spawnPoisonCloud(level(), getX() - getDeltaMovement().x, getY() - getDeltaMovement().y,
                    getZ() - getDeltaMovement().z, 750, 2.5, 2);
        } else if (type == WarheadType.TURBINE) {
            ExplosionLarge.explode(level(), getX(), getY(), getZ(), 10, true, false, true);
            int count = (int) strength;
            // 1.7 fanned the blades out on a rotating 0.5-radius vector, one motion step behind the missile.
            // The legacy BulletConfigSyncingUtil.TURBINE config is re-expressed on sedna as XFactoryWarhead.
            Vec3 fan = new Vec3(0.5D, 0D, 0D);
            for(int i = 0; i < count; i++) {
                level().addFreshEntity(new EntityBulletBaseMK4(level(), null, XFactoryWarhead.warhead_turbine,
                        XFactoryWarhead.TURBINE_BLADE_DAMAGE, 0F,
                        getX() - getDeltaMovement().x,
                        getY() - getDeltaMovement().y + random.nextGaussian(),
                        getZ() - getDeltaMovement().z,
                        fan.x, 0D, fan.z));
                fan = fan.yRot((float) (Math.PI * 2F / count));
            }
        }
    }

    @Override
    public List<ItemStack> getDebris() { return new ArrayList<>(); }
    @Override public ItemStack getDebrisRareDrop() { return ItemStack.EMPTY; }

    // 1.7: new ItemStack(ModItems.missile_custom), a bare one without its parts
    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(com.hbm.items.ModCustomMissileItems.missile_custom.get());
    }

    // the blip is the fuselage's silhouette: which of the five 1.7 size pairs the craft assembles into
    public String getRadarName() {
        ItemCustomMissilePart fuselage = getPart(FUSELAGE);
        if(fuselage == null) return "radar.target.custom";
        if(fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_10) return "radar.target.custom10";
        if(fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_15) return "radar.target.custom1015";
        if(fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_15) return "radar.target.custom15";
        if(fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_20) return "radar.target.custom1520";
        if(fuselage.top == PartSize.SIZE_20 && fuselage.bottom == PartSize.SIZE_20) return "radar.target.custom20";
        return "radar.target.custom";
    }

    public int getBlipLevel() {
        ItemCustomMissilePart fuselage = getPart(FUSELAGE);
        if(fuselage == null) return IRadarDetectableNT.TIER1;
        if(fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_10) return IRadarDetectableNT.TIER10;
        if(fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_15) return IRadarDetectableNT.TIER10_15;
        if(fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_15) return IRadarDetectableNT.TIER15;
        if(fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_20) return IRadarDetectableNT.TIER15_20;
        if(fuselage.top == PartSize.SIZE_20 && fuselage.bottom == PartSize.SIZE_20) return IRadarDetectableNT.TIER20;
        return IRadarDetectableNT.TIER1;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        fuel = input.getFloatOr("fuel", fuel);
        consumption = input.getFloatOr("consumption", consumption);
        input.read("warhead", dev.golem.core.world.ItemStacks.optionalCodec()).ifPresent(s -> entityData.set(WARHEAD, s));
        input.read("fuselage", dev.golem.core.world.ItemStacks.optionalCodec()).ifPresent(s -> entityData.set(FUSELAGE, s));
        input.read("fins", dev.golem.core.world.ItemStacks.optionalCodec()).ifPresent(s -> entityData.set(FINS, s));
        input.read("thruster", dev.golem.core.world.ItemStacks.optionalCodec()).ifPresent(s -> entityData.set(THRUSTER, s));
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putFloat("fuel", fuel);
        output.putFloat("consumption", consumption);
        output.store("warhead", dev.golem.core.world.ItemStacks.optionalCodec(), entityData.get(WARHEAD));
        output.store("fuselage", dev.golem.core.world.ItemStacks.optionalCodec(), entityData.get(FUSELAGE));
        output.store("fins", dev.golem.core.world.ItemStacks.optionalCodec(), entityData.get(FINS));
        output.store("thruster", dev.golem.core.world.ItemStacks.optionalCodec(), entityData.get(THRUSTER));
    }
}
