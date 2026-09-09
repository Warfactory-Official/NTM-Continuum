package com.hbm.items.weapon;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSlag;
import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnumMulti;
import com.hbm.items.ModDataComponents;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.potion.ModEffects;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The twelve 16-inch shells. 1.12.2 holds them as an array of anonymous subclasses indexed by item
 * damage; here the behaviour lives on the enum constants and the selector is {@code ITEM_VARIANT}.
 *
 * <p>Two things upstream does here are absent, both by standing project decision rather than by
 * oversight. Spent casings: this port's ammunition carries no casing, so the shells declare none.
 * And pollution: the chemical shells push heavy-metal and poison loads into the pollution system,
 * which is deferred wholesale — see {@link EntityMist} for what that costs the gas rounds.
 */
public class ItemAmmoArty extends ItemEnumMulti {

    public ItemAmmoArty(Properties properties) {
        super(ArtilleryShellType.class, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        String desc = getDescriptionId() + "." + getType(stack).name().toLowerCase(Locale.US) + ".desc.";
        switch ((ArtilleryShellType) getType(stack)) {
            case NORMAL, CLASSIC -> {
                line(tooltip, desc, 0, ChatFormatting.YELLOW);
                line(tooltip, desc, 1, ChatFormatting.YELLOW);
                line(tooltip, desc, 2, ChatFormatting.BLUE);
            }
            case EXPLOSIVE -> {
                line(tooltip, desc, 0, ChatFormatting.YELLOW);
                line(tooltip, desc, 1, ChatFormatting.YELLOW);
                line(tooltip, desc, 2, ChatFormatting.RED);
            }
            case PHOSPHORUS -> {
                line(tooltip, desc, 0, ChatFormatting.YELLOW);
                line(tooltip, desc, 1, ChatFormatting.YELLOW);
                line(tooltip, desc, 2, ChatFormatting.RED);
                line(tooltip, desc, 3, ChatFormatting.BLUE);
            }
            case PHOSPHORUS_MULTI, MINI_NUKE_MULTI -> line(tooltip, desc, 0, ChatFormatting.RED);
            case MINI_NUKE -> {
                line(tooltip, desc, 0, ChatFormatting.YELLOW);
                line(tooltip, desc, 1, ChatFormatting.RED);
                line(tooltip, desc, 2, ChatFormatting.RED);
            }
            case NUKE -> {
                line(tooltip, desc, 0, ChatFormatting.RED);
                line(tooltip, desc, 1, ChatFormatting.RED);
                line(tooltip, desc, 2, ChatFormatting.RED);
            }
            case CARGO -> {
                ItemStack cargo = ModDataComponents.ARTY_CARGO.getOr(stack, ItemStack.EMPTY);
                if (!cargo.isEmpty()) {
                    tooltip.add(cargo.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
                } else {
                    line(tooltip, desc, 0, ChatFormatting.RED);
                }
            }
            default -> { }
        }
    }

    private static void line(List<Component> tooltip, String desc, int index, ChatFormatting color) {
        tooltip.add(Component.translatable(desc + index).withStyle(color));
    }

    public enum ArtilleryShellType {

        NORMAL("ammo_arty") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                standardExplosion(shell, hit, 10F, 3F, false);
                ExplosionCreator.composeEffectStandard(shell.level(), hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z);
            }
        },
        CLASSIC("ammo_arty_classic") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                standardExplosion(shell, hit, 15F, 5F, false);
                ExplosionCreator.composeEffectStandard(shell.level(), hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z);
            }
        },
        EXPLOSIVE("ammo_arty_he") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                standardExplosion(shell, hit, 15F, 3F, true);
                ExplosionCreator.composeEffectStandard(shell.level(), hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z);
            }
        },
        MINI_NUKE("ammo_arty_mini_nuke") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                Level level = shell.level();
                Vec3 impact = behindImpact(shell, hit);
                shell.killAndClear();
                EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFac(level, 20,
                        impact.x, impact.y, impact.z);
                if (level.addFreshEntity(blast)) blast.startChunkLoading();
                EntityNukeTorex torex = EntityNukeTorex.statFac(level, impact.x, impact.y, impact.z, 20F);
                if (level.addFreshEntity(torex)) torex.startChunkLoading();
            }
        },
        NUKE("ammo_arty_nuke") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                Level level = shell.level();
                Vec3 where = hit.getLocation();
                EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFac(level, BombConfig.missileRadius,
                        where.x, where.y, where.z);
                if (level.addFreshEntity(blast)) blast.startChunkLoading();
                EntityNukeTorex torex = EntityNukeTorex.statFac(level, where.x, where.y, where.z,
                        BombConfig.missileRadius);
                if (level.addFreshEntity(torex)) torex.startChunkLoading();
                shell.killAndClear();
            }
        },
        PHOSPHORUS("ammo_arty_phosphorus") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                Level level = shell.level();
                Vec3 where = hit.getLocation();
                double x = shell.getX();
                double y = shell.getY();
                double z = shell.getZ();

                playBoom(shell);
                standardExplosion(shell, hit, 10F, 3F, false);
                ExplosionLarge.spawnShrapnels(level, where.x, where.y, where.z, 15);
                ExplosionChaos.burn(level, BlockPos.containing(where.x, where.y, where.z), 12);

                int radius = 15;
                for (Entity e : level.getEntities((Entity) null,
                        new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius),
                        entity -> true)) {
                    Effects.ignite(e, 5);
                    if (e instanceof LivingEntity living) {
                        Effects.addRegistered(living, ModEffects.PHOSPHORUS.get(), 30 * 20, 0);
                    }
                }

                ExplosionCreator.composeEffectRBMKMush(level, where.x, where.y, where.z, 10F);
            }
        },
        MINI_NUKE_MULTI("ammo_arty_mini_nuke_multi") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                MINI_NUKE.onImpact(shell, hit);
            }

            @Override
            public void onUpdate(EntityArtilleryShell shell) {
                standardCluster(shell, MINI_NUKE, 5, 300D, 5D);
            }
        },
        PHOSPHORUS_MULTI("ammo_arty_phosphorus_multi") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                PHOSPHORUS.onImpact(shell, hit);
            }

            @Override
            public void onUpdate(EntityArtilleryShell shell) {
                standardCluster(shell, PHOSPHORUS, 10, 300D, 5D);
            }
        },
        /** The one shell that does not explode: it sticks where it lands and waits to be emptied. */
        CARGO("ammo_arty_cargo") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                if (hit instanceof BlockHitResult block) {
                    Vec3 where = hit.getLocation();
                    shell.setPos(where.x, where.y, where.z);
                    shell.getStuck(block.getBlockPos(), block.getDirection().ordinal());
                }
            }
        },
        CHLORINE("ammo_arty_chlorine") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                Level level = shell.level();
                Vec3 impact = behindImpact(shell, hit);
                shell.killAndClear();
                level.explode(shell, impact.x, impact.y, impact.z, 0F, Level.ExplosionInteraction.NONE);

                EntityMist mist = new EntityMist(level);
                mist.setType(Fluids.CHLORINE);
                mist.setPos(impact.x, impact.y - 3D, impact.z);
                mist.setArea(15F, 7.5F);
                level.addFreshEntity(mist);
            }
        },
        PHOSGENE("ammo_arty_phosgene") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                Level level = shell.level();
                Vec3 impact = behindImpact(shell, hit);
                shell.killAndClear();
                level.explode(shell, impact.x, impact.y, impact.z, 5F, Level.ExplosionInteraction.NONE);
                gasCloud(shell, impact, Fluids.PHOSGENE, 3, 15D, 15F, 10F);
            }
        },
        MUSTARD("ammo_arty_mustard_gas") {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult hit) {
                Level level = shell.level();
                Vec3 impact = behindImpact(shell, hit);
                shell.killAndClear();
                level.explode(shell, impact.x, impact.y, impact.z, 5F, Level.ExplosionInteraction.NONE);
                gasCloud(shell, impact, Fluids.MUSTARDGAS, 5, 25D, 20F, 10F);
            }
        };

        public final String name;

        ArtilleryShellType(String name) {
            this.name = name;
        }

        public abstract void onImpact(EntityArtilleryShell shell, HitResult hit);

        public void onUpdate(EntityArtilleryShell shell) {}
    }

    private static final Supplier<BlockState> SLAG_CRACKED =
            () -> ModBlocks.block_slag.get().defaultBlockState().setValue(BlockSlag.CRACKED, true);

    public static ArtilleryShellType byIndex(int index) {
        ArtilleryShellType[] values = ArtilleryShellType.values();
        return index >= 0 && index < values.length ? values[index] : values[0];
    }

    private static void playBoom(EntityArtilleryShell shell) {
        shell.level().playSound(null, shell.getX(), shell.getY(), shell.getZ(),
                ModSounds.EXPLOSION_SMALL_NEAR.get(), SoundSource.BLOCKS, 20.0F,
                0.9F + shell.level().getRandom().nextFloat() * 0.2F);
    }

    /** Upstream backs the blast off one motion step so a shell never detonates inside what it hit. */
    private static Vec3 behindImpact(EntityArtilleryShell shell, HitResult hit) {
        Vec3 motion = shell.getDeltaMovement();
        return hit.getLocation().subtract(motion.lengthSqr() < 1.0E-8D ? Vec3.ZERO : motion.normalize());
    }

    /** A scattered pattern of clouds, the first on the impact point and the rest drifted off it. */
    private static void gasCloud(EntityArtilleryShell shell, Vec3 impact, FluidType fluid, int count,
            double spread, float width, float height) {
        for (int i = 0; i < count; i++) {
            EntityMist mist = new EntityMist(shell.level());
            mist.setType(fluid);
            double x = impact.x;
            double z = impact.z;
            if (i > 0) {
                x += shell.level().getRandom().nextGaussian() * spread;
                z += shell.level().getRandom().nextGaussian() * spread;
            }
            mist.setPos(x, impact.y - 5D, z);
            mist.setArea(width, height);
            shell.level().addFreshEntity(mist);
        }
    }

    public static void standardExplosion(EntityArtilleryShell shell, HitResult hit, float size, float rangeMod,
            boolean breaksBlocks) {
        Vec3 impact = behindImpact(shell, hit);
        ExplosionVNT xnt = new ExplosionVNT(shell.level(), impact.x, impact.y, impact.z, size);
        if (breaksBlocks) {
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop()
                    .withBlockEffect(new BlockMutatorDebris(SLAG_CRACKED)));
        }
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(rangeMod));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        shell.killAndClear();
    }

    /**
     * Splits a carrier shell into submunitions once it is past apogee and within {@code splitHeight} of
     * its target altitude — so a cluster opens on the way down, not on the way up.
     */
    public static void standardCluster(EntityArtilleryShell shell, ArtilleryShellType clusterType, int amount,
            double splitHeight, double deviation) {
        if (!shell.getWhistle() || shell.getDeltaMovement().y > 0D) return;
        if (shell.getTargetHeight() + splitHeight < shell.getY()) return;

        Level level = shell.level();
        Vec3 motion = shell.getDeltaMovement();
        double[] target = shell.getTarget();
        boolean passOnWhistle = shell.getWhistle() && !shell.didWhistle();
        shell.killAndClear();

        for (int i = 0; i < amount; i++) {
            EntityArtilleryShell cluster = new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), level);
            cluster.setType(clusterType.ordinal());
            cluster.setPos(shell.getX(), shell.getY(), shell.getZ());
            cluster.setYRot(shell.getYRot());
            cluster.setXRot(shell.getXRot());
            cluster.setDeltaMovement(
                    i == 0 ? motion.x : motion.x + shell.level().getRandom().nextGaussian() * deviation,
                    motion.y,
                    i == 0 ? motion.z : motion.z + shell.level().getRandom().nextGaussian() * deviation);
            cluster.setTarget(target[0], target[1], target[2]);
            cluster.setWhistle(passOnWhistle);
            level.addFreshEntity(cluster);
        }
    }
}
