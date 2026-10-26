package com.hbm.entity.projectile;

import com.hbm.api.environment.Environment;
import dev.golem.core.data.Tags;

import com.hbm.capability.HbmLivingProps;
import com.hbm.client.ChemicalClientEffects;
import com.hbm.handler.ArmorUtil;
import com.hbm.util.EnchantmentUtil;
import com.hbm.entity.ModEntities;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Effects;
import java.util.List;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.interfaces.IRepairable;
import com.hbm.util.LockTargets;
import dev.golem.core.world.LightLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The original chemical gun splash. Fluid effects follow 1.7's {@code affect} order: antimatter / ion gel beams,
 * boil damage at 100 °C and up, cold and delicious for liquid or gas sprays, oil soaking, extinguishing,
 * flamethrower damage for burning liquids and burning gases, corrosion, vent radiation, poison, toxin, pheromone,
 * then experience juice and ender juice.
 */
public class EntityChemical extends EntityThrowableNT {

    /** Space's fork seeds only the struck block; plain HBM retains the surrounding cube. */
    public static boolean seedSlurrySingleBlock = false;

    private static final EntityDataAccessor<Integer> FLUID_TYPE =
            SynchedEntityData.defineId(EntityChemical.class, EntityDataSerializers.INT);

    public EntityChemical(EntityType<? extends EntityChemical> type, Level level) {
        super(type, level);
    }

    public EntityChemical(Level level, LivingEntity thrower, double sideOffset, double heightOffset,
            double frontOffset) {
        this(ModEntities.CHEMICAL.get(), level);
        this.thrower = thrower;
        setPos(thrower.getX(), thrower.getEyeY(), thrower.getZ());
        var offset = new Vec3(sideOffset, heightOffset, frontOffset)
                .xRot(-thrower.getXRot() * ((float) Math.PI / 180F))
                .yRot(-thrower.getYRot() * ((float) Math.PI / 180F));
        setPos(getX() + offset.x, getY() + offset.y, getZ() + offset.z);
        double yaw = thrower.getYRot() * Math.PI / 180D;
        double pitch = thrower.getXRot() * Math.PI / 180D;
        setThrowableHeading(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch), 1F, 0F);
    }

    public EntityChemical setFluid(FluidType fluid) {
        entityData.set(FLUID_TYPE, Fluids.networkId(fluid == null ? Fluids.NONE : fluid));
        return this;
    }

    public FluidType getFluid() { return Fluids.byNetworkId(entityData.get(FLUID_TYPE)); }

    @Override protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(FLUID_TYPE, 0); // Fluids.NONE's network id
    }

    @Override public int selfDamageDelay() { return 2; }

    @Override public void tick() {
        if (!level().isClientSide() && tickCount > getMaxAge()) {
            discard();
            return;
        }

        if (level().isClientSide()) {
            ChemicalClientEffects.tick(this);
        } else if (FluidTraitSimple.isGaseousAny(getFluid())) {
            double intensity = 1D - (double) tickCount / (double) getMaxAge();
            List<Entity> affected = level().getEntities(this,
                    getBoundingBox().inflate(intensity * 2.5D),
                    entity -> entity != thrower && isAffected(entity));
            for (Entity entity : affected) affect(entity, intensity);
        }
        super.tick();
    }

    /** 1.7: only GAS has 0.95; GASFLAME falls through to the 0.99 default. */
    @Override public float getAirDrag() {
        return switch (style()) {
            case GAS -> 0.95F;
            case AMAT, LIGHTNING -> 1F;
            default -> 0.99F;
        };
    }

    /** 1.7: AMAT, LIGHTNING and GAS keep full speed in water; everything else, GASFLAME included, gets 0.8. */
    @Override public float getWaterDrag() {
        return switch (style()) {
            case GAS, AMAT, LIGHTNING -> 1F;
            default -> 0.8F;
        };
    }

    @Override public double getGravityVelocity() {
        return switch (style()) {
            case AMAT, LIGHTNING, GAS -> 0F;
            case GASFLAME -> -0.01F;
            default -> 0.03F;
        };
    }

    public int getMaxAge() {
        return switch (style()) {
            case AMAT -> 100;
            case LIGHTNING -> 5;
            case BURNING -> 600;
            case GAS -> 60;
            case GASFLAME -> 20;
            case LIQUID -> 600;
            default -> 100;
        };
    }

    public ChemicalStyle style() { return getStyleFromType(getFluid()); }

    /** 1.7 {@code getStyleFromType}, on the real gaseous / liquid / antimatter traits. */
    public static ChemicalStyle getStyleFromType(FluidType type) {
        if (type == Fluids.IONGEL) return ChemicalStyle.LIGHTNING;
        if (type.isAntimatter()) return ChemicalStyle.AMAT;
        if (FluidTraitSimple.isGaseousAny(type)) {
            return FluidCombustion.isFlammable(type) || FluidCombustion.getCombustible(type) != null
                    ? ChemicalStyle.GASFLAME : ChemicalStyle.GAS;
        }
        if (type.hasTrait(FluidTraitSimple.FT_Liquid.class)) {
            return FluidCombustion.getCombustible(type) != null ? ChemicalStyle.BURNING : ChemicalStyle.LIQUID;
        }
        return ChemicalStyle.NULL;
    }

    @Override protected void onImpact(HitResult hit) {
        if (level().isClientSide()) return;
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            affect(entityHit.getEntity(), 1D - (double) tickCount / getMaxAge());
            // The old chemical entity continues its cloud/spray lifetime after touching an entity;
            // only a block impact consumes the projectile.
            return;
        } else if (hit instanceof BlockHitResult blockHit) {
            blockImpact(blockHit);
            discard();
        }
    }

    /**
     * Whether a spray acts on {@code e}. 1.7 has no creative filter (its scan excludes only the thrower), so a
     * creative player gets pheromone buffs, XP juice and ender juice too; only spectators, which 1.7 lacks, are skipped.
     */
    public static boolean isAffected(Entity e) {
        return !e.isSpectator();
    }

    /** 1.7 {@code affect} (protected there); public so GameTests can apply one hit at a known intensity. */
    public void affect(Entity entity, double intensity) {
        if (!isAffected(entity)) return;
        FluidType type = getFluid();
        LivingEntity living = entity instanceof LivingEntity value ? value : null;
        if (style() == ChemicalStyle.LIQUID || style() == ChemicalStyle.BURNING) intensity = 1D;
        switch (style()) {
            case AMAT -> {
                if (level() instanceof ServerLevel server) {
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, ModDamageSources.radiation(server), 1F);
                    if (living != null) ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                            ContaminationUtil.ContaminationType.CREATIVE, 50F * (float) intensity);
                }
                if (living != null) return;
            }
            case LIGHTNING -> {
                if (level() instanceof ServerLevel server)
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, ModDamageSources.electricity(server), 0.5F);
                if (living != null) {
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 9));
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 9));
                }
                if (living != null) return;
            }
            default -> { }
        }

        ServerLevel server = level() instanceof ServerLevel value ? value : null;

        if (type.getTemperature() >= 100) {
            // 1.7: 0.25 damage at 100 °C with one extra damage every 1000 °C, capped at 15.
            if (server != null) EntityDamageUtil.attackEntityFromIgnoreIFrame(entity,
                    damage(server, ModDamageSources.BOIL), Math.min(0.25F + (type.getTemperature() - 100) * 0.001F, 15F));
            if (type.getTemperature() >= 500) Effects.ignite(entity, 10); // afterburn for 10 seconds
        }

        boolean delicious = type.hasTrait(FluidTraitSimple.FT_Delicious.class) && living != null;
        if (style() == ChemicalStyle.LIQUID || style() == ChemicalStyle.GAS) {
            // 1.7 sprays cold with the BOIL source (s_boil), not the cryolator's: kept as written.
            if (type.getTemperature() < -20 && living != null && server != null) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, damage(server, ModDamageSources.BOIL),
                        Math.min(0.25F + (-type.getTemperature()) * 0.01F, 2F));
            }
            // 1.7 heals in its LIQUID|GAS branch and again in its LIQUID branch, so a delicious liquid heals twice.
            if (delicious && living.isAlive()) living.heal(2F * (float) intensity);
        }
        if (style() == ChemicalStyle.LIQUID && FluidCombustion.isFlammable(type) && living != null)
            HbmLivingProps.setOil(living, 300); // doused in oil for 15 seconds
        if (delicious && style() == ChemicalStyle.LIQUID && living.isAlive()) living.heal(2F * (float) intensity);
        if (isExtinguishing() && server != null) entity.clearFire();

        if (style() == ChemicalStyle.BURNING) {
            FluidCombustion.Combustible combustible = FluidCombustion.getCombustible(type);
            if (server != null) EntityDamageUtil.attackEntityFromIgnoreIFrame(entity,
                    damage(server, ModDamageSources.FLAMETHROWER),
                    0.2F + (combustible != null ? Math.min(combustible.energyPerBucket() / 100_000F, 15F) : 0F));
            Effects.ignite(entity, 5);
        }

        if (style() == ChemicalStyle.GASFLAME) {
            FluidCombustion.Combustible combustible = FluidCombustion.getCombustible(type);
            float heat = Math.max(FluidCombustion.isFlammable(type) ? FluidCombustion.getFlammableEnergy(type) / 50_000F : 0F,
                    combustible != null ? Math.min(combustible.energyPerBucket() / 100_000F, 15F) : 0F);
            heat *= (float) intensity;
            if (server != null) EntityDamageUtil.attackEntityFromIgnoreIFrame(entity,
                    damage(server, ModDamageSources.FLAMETHROWER), (0.2F + heat) * (float) intensity);
            Effects.ignite(entity, (int) Math.ceil(5D * intensity));
        }

        FT_Corrosive corrosive = type.getTrait(FT_Corrosive.class);
        if (corrosive != null && living != null && server != null) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(living, damage(server, ModDamageSources.ACID_PLAYER),
                    corrosive.getRating() / 50F);
            ArmorUtil.damageWholeSuit(living, corrosive.getRating() / 40);
        }

        // 1.7 order: corrosive, vent radiation, poison, toxin, then pheromone.
        FT_VentRadiation vent = type.getTrait(FT_VentRadiation.class);
        if (vent != null && server != null) {
            if (living != null) ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.CREATIVE, vent.getRadPerMB() * 5);
            ChunkRadiationManager.proxy.incrementRad(server,
                    BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()), vent.getRadPerMB() * 5);
        }

        FT_Poison poison = type.getTrait(FT_Poison.class);
        if (poison != null && living != null && level() instanceof ServerLevel) {
            living.addEffect(new MobEffectInstance(poison.isWithering() ? MobEffects.WITHER : MobEffects.POISON,
                    (int) (5 * 20 * intensity)));
        }

        FT_Toxin toxin = type.getTrait(FT_Toxin.class);
        if (toxin != null && living != null && level() instanceof ServerLevel) toxin.affect(living, intensity);

        FT_Pheromone pheromone = type.getTrait(FT_Pheromone.class);
        if (pheromone != null && living != null && level() instanceof ServerLevel) applyPheromone(living, pheromone);

        // 1.7: experience juice gives a player one point (with the level-up sound) and is used up on them.
        if (type == Fluids.XPJUICE && entity instanceof Player player && server != null) {
            EnchantmentUtil.addExperience(player, 1, false);
            discard();
        }

        if (type == Fluids.ENDERJUICE && server != null) teleportRandomly(this, this.random, entity);
    }

    /** Re-seeds this spray's own random (the one {@link #teleportRandomly} draws from), for GameTests. */
    public void seedRandom(long seed) {
        this.random.setSeed(seed);
    }

    /** 1.7 {@code getDamage}: indirect (this spray, credited to whoever fired it) when there is a thrower. */
    private DamageSource damage(ServerLevel server, ResourceKey<DamageType> key) {
        return thrower != null ? ModDamageSources.source(server, key, this, thrower) : ModDamageSources.source(server, key);
    }

    /**
     * 1.7 {@code teleportRandomly} ("terribly copy-pasted from EntityEnderman"): a random target within 32 blocks
     * of {@code origin} on each horizontal axis and -32..+31 vertically. Shared with {@link
     * com.hbm.entity.effect.EntityMist}, which carried a second copy of the same code in 1.7.
     */
    public static boolean teleportRandomly(Entity origin, RandomSource rand, Entity e) {
        double x = origin.getX() + (rand.nextDouble() - 0.5D) * 64.0D;
        double y = origin.getY() + (double) (rand.nextInt(64) - 32);
        double z = origin.getZ() + (rand.nextDouble() - 0.5D) * 64.0D;
        return teleportTo(e, x, y, z);
    }

    /**
     * 1.7 {@code teleportTo}: from the target, fall until the block below blocks movement (giving up at the world
     * floor), then accept the spot only if the entity collides with nothing there and touches no liquid; otherwise
     * stay put. The 128 portal particles of 1.7 are not carried: 1.7 spawned them through the SERVER world, which
     * shows nothing, and this path only ever runs on the server. Both ender sounds are (old and new position).
     */
    public static boolean teleportTo(Entity e, double x, double y, double z) {
        Level level = e.level();
        double fromX = e.getX();
        double fromY = e.getY();
        double fromZ = e.getZ();
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, y, z).mutable();
        boolean placed = false;

        if (level.isLoaded(pos)) {
            boolean ground = false;
            while (!ground && pos.getY() > level.getMinY()) {
                if (level.getBlockState(pos.below()).blocksMotion()) {
                    ground = true;
                } else {
                    y--;
                    pos.move(Direction.DOWN);
                }
            }
            if (ground) {
                e.setPos(x, y, z);
                placed = level.noCollision(e) && !level.containsAnyLiquid(e.getBoundingBox());
            }
        }

        e.setPos(fromX, fromY, fromZ);
        if (!placed) return false;
        // teleportTo rather than setPos so a player's client follows the move.
        e.teleportTo(x, y, z);
        level.playSound(null, fromX, fromY, fromZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        e.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        return true;
    }

    /**
     * 1.7 {@code EntityChemical}'s pheromone table (not {@code EntityMist}'s): everything sprayed gets resistance,
     * speed and haste; a glyphid sprayed with glyphid pheromone (type 1) also strength V, fire resistance and
     * absorption XX, a player sprayed with the modified one (type 2) strength III.
     */
    public static void applyPheromone(LivingEntity living, FT_Pheromone pheromone) {
        Effects.add(living, Effects.resistance(), 2 * 60 * 20, 2);
        Effects.add(living, Effects.speed(), 5 * 60 * 20, 1);
        Effects.add(living, Effects.haste(), 2 * 60 * 20, 4);

        if (living instanceof EntityGlyphid && pheromone.getType() == 1) {
            Effects.add(living, Effects.strength(), 5 * 60 * 20, 4);
            living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60 * 20, 0));
            living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60 * 20, 19));
        } else if (living instanceof Player && pheromone.getType() == 2) {
            Effects.add(living, Effects.strength(), 2 * 60 * 20, 2);
        }
    }

    /**
     * 1.7 {@code onImpact}'s block branch, in its order: vent radiation, fire (balefire for balefire fluid) on the
     * six air neighbours of a burning spray, extinguishing of neighbouring fire, multiblock extinguishing for CO2
     * and water-like fluids (water liquid sprays also wash fallout off a 5x2x5 area), and seed slurry greening a
     * 3x3x3 area. 1.7 runs the fire loop twice; the second pass can never find air the first left, so it is one.
     * Public so GameTests can drive one impact; {@link #onImpact} discards the spray afterwards.
     */
    public void blockImpact(BlockHitResult hit) {
        Level level = level();
        BlockPos pos = hit.getBlockPos();
        FluidType type = getFluid();
        ChemicalStyle style = style();
        FT_VentRadiation vent = type.getTrait(FT_VentRadiation.class);
        if (vent != null) ChunkRadiationManager.proxy.incrementRad(level, pos, vent.getRadPerMB() * 5);
        if (style == ChemicalStyle.BURNING || style == ChemicalStyle.GASFLAME) {
            BlockState fire = type == Fluids.BALEFIRE ? ModBombBlocks.balefire.get().defaultBlockState()
                    : Blocks.FIRE.defaultBlockState();
            for (Direction dir : Direction.values()) {
                BlockPos adjacent = pos.relative(dir);
                if (level.getBlockState(adjacent).isAir()) level.setBlockAndUpdate(adjacent, fire);
            }
        }
        if (isExtinguishing()) {
            for (Direction dir : Direction.values()) {
                BlockPos adjacent = pos.relative(dir);
                if (level.getBlockState(adjacent).is(Blocks.FIRE))
                    level.setBlockAndUpdate(adjacent, Blocks.AIR.defaultBlockState());
            }
        }

        IRepairable.EnumExtinguishType fext = getExtinguishingType(type);
        if (fext != null) {
            BlockEntity core = LockTargets.coreAt(level, pos);
            if (core instanceof IRepairable repairable) repairable.tryExtinguish(level, pos, fext);
            if (fext == IRepairable.EnumExtinguishType.WATER && style == ChemicalStyle.LIQUID) {
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 1, 2))) {
                    if (level.getBlockState(p).is(ModFalloutBlocks.fallout.get()))
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
            }
        }

        if (type == Fluids.SEEDSLURRY) seedSlurry(level, pos);
        if (level instanceof ServerLevel server) Environment.chemicalImpact(server, pos, type);
    }

    /** 1.7 {@code getExtinguishingType}: CO2, or WATER for water, heavy water and coolant. */
    public static IRepairable.EnumExtinguishType getExtinguishingType(FluidType type) {
        if (type == Fluids.CARBONDIOXIDE) return IRepairable.EnumExtinguishType.CO2;
        if (type == Fluids.WATER || type == Fluids.HEAVYWATER || type == Fluids.COOLANT)
            return IRepairable.EnumExtinguishType.WATER;
        return null;
    }

    /**
     * 1.7's seed slurry impact over the 3x3x3 cube around the hit block. Dirt (1.7's one dirt block covers coarse
     * dirt and podzol), waste earth, dead dirt and oily dirt turn to grass when the block above has combined light
     * 9+ and opacity 2 or less; waste earth turns to grass regardless; cobblestone, stone bricks, concrete bricks
     * and their slab and stairs become the mossy variants. 1.7's double slab was a separate block it did not
     * touch, so a double slab is left alone here too.
     */
    public static void seedSlurry(Level level, BlockPos centre) {
        var radius = seedSlurrySingleBlock ? 0 : 1;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            BlockPos at = p.immutable();
            BlockState state = level.getBlockState(at);
            boolean wasteEarth = state.is(ModFalloutBlocks.waste_earth.get());
            if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || wasteEarth
                    || state.is(ModFallingBlocks.dirt_dead.get()) || state.is(ModFallingBlocks.dirt_oily.get())) {
                BlockPos above = at.above();
                // hbm-space: only under an atmosphere grass can live in, turning its CO2 into air
                // (com.hbm.api.environment.Environment.GROWTH; always on plain HBM)
                if (LightLevels.combined(level, above) >= 9 && LightLevels.opacity(level, above) <= 2
                        && Environment.canGrow(level, at))
                    level.setBlockAndUpdate(at, Blocks.GRASS_BLOCK.defaultBlockState());
            }
            if (state.is(Blocks.COBBLESTONE)) level.setBlockAndUpdate(at, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            if (state.is(Blocks.STONE_BRICKS)) level.setBlockAndUpdate(at, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
            if (wasteEarth) level.setBlockAndUpdate(at, Blocks.GRASS_BLOCK.defaultBlockState());
            if (state.is(ModHazardBlocks.brick_concrete.get()))
                level.setBlockAndUpdate(at, ModHazardBlocks.brick_concrete_mossy.get().defaultBlockState());
            if (state.is(ModBlocks.brick_concrete_slab.get()) && state.hasProperty(SlabBlock.TYPE)
                    && state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE)
                level.setBlockAndUpdate(at, ModBlocks.brick_concrete_mossy_slab.get().withPropertiesOf(state));
            if (state.is(ModBlocks.brick_concrete_stairs.get()))
                level.setBlockAndUpdate(at, ModBlocks.brick_concrete_mossy_stairs.get().withPropertiesOf(state));
        }
    }

    private boolean isExtinguishing() {
        FluidType type = getFluid();
        return style() == ChemicalStyle.LIQUID && type.getTemperature() < 50 && !FluidCombustion.isFlammable(type);
    }

    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        Fluids.write(output, "fluid", getFluid()); // registry name; the synched int is session-only
    }

    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        entityData.set(FLUID_TYPE, Fluids.networkId(Fluids.read(input, "fluid")));
    }

    public enum ChemicalStyle { AMAT, LIGHTNING, LIQUID, GAS, GASFLAME, BURNING, NULL }
}
