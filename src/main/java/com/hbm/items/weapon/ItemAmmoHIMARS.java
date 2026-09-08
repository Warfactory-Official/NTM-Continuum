package com.hbm.items.weapon;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSlag;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ItemEnumMulti;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.potion.ModEffects;
import com.hbm.sound.ModSounds;
import dev.golem.core.Ids;
import dev.golem.core.world.Effects;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The eight HIMARS rocket pods. 1.12.2 keeps them as a damage-indexed array of anonymous classes; here
 * they are enum constants carrying the same behaviour, selected by {@code ITEM_VARIANT}.
 *
 * <p>Two deviations, both about blocks this slice does not have. {@link HIMARSRocketType#SMALL_LAVA}
 * fills its crater with magma rather than upstream's {@code volcanic_lava_block}, which is a finite
 * fluid on a whole system that is not ported — vanilla flowing lava would run out of the crater and
 * across the map, which is exactly what the finite fluid exists to avoid. And the white-phosphorus
 * round's ten scattered haze puffs have no ported particle, so it composes the mushroom effect alone.
 */
public class ItemAmmoHIMARS extends ItemEnumMulti {

    public ItemAmmoHIMARS(Properties properties) {
        super(HIMARSRocketType.class, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, dev.golem.core.world.CompatTooltipContext context,
                                         java.util.List<Component> tooltip, TooltipFlag flag) {
        String desc = getDescriptionId() + "." + getType(stack).name().toLowerCase(Locale.US) + ".desc.";
        switch ((HIMARSRocketType) getType(stack)) {
            case SMALL -> {
                line(tooltip, desc, 0, ChatFormatting.YELLOW);
                line(tooltip, desc, 1, ChatFormatting.YELLOW);
                line(tooltip, desc, 2, ChatFormatting.BLUE);
            }
            case SMALL_WP -> {
                line(tooltip, desc, 0, ChatFormatting.YELLOW);
                line(tooltip, desc, 1, ChatFormatting.YELLOW);
                line(tooltip, desc, 2, ChatFormatting.RED);
                line(tooltip, desc, 3, ChatFormatting.BLUE);
            }
            case SMALL_HE, SMALL_TB, LARGE, LARGE_TB -> {
                line(tooltip, desc, 0, ChatFormatting.YELLOW);
                line(tooltip, desc, 1, ChatFormatting.YELLOW);
                line(tooltip, desc, 2, ChatFormatting.RED);
            }
            case SMALL_MINI_NUKE, SMALL_LAVA -> {
                line(tooltip, desc, 0, ChatFormatting.YELLOW);
                line(tooltip, desc, 1, ChatFormatting.RED);
                line(tooltip, desc, 2, ChatFormatting.RED);
            }
        }
    }

    private static void line(java.util.List<Component> tooltip, String desc, int index, ChatFormatting color) {
        tooltip.add(Component.translatable(desc + index).withStyle(color));
    }

    public enum HIMARSRocketType {

        SMALL("standard", "himars_standard", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult hit) {
                standardExplosion(rocket, hit, 20F, 3F, false, SLAG_CRACKED);
                ExplosionCreator.composeEffectStandard(rocket.level(), hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z);
            }
        },
        LARGE("single", "himars_single", 1) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult hit) {
                standardExplosion(rocket, hit, 50F, 5F, true, SLAG_CRACKED);
                ExplosionCreator.composeEffectLarge(rocket.level(), hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z);
            }
        },
        SMALL_HE("standard_he", "himars_standard_he", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult hit) {
                standardExplosion(rocket, hit, 20F, 3F, true, SLAG_CRACKED);
                ExplosionCreator.composeEffectStandard(rocket.level(), hit.getLocation().x, hit.getLocation().y,
                        hit.getLocation().z);
            }
        },
        SMALL_WP("standard_wp", "himars_standard_wp", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult hit) {
                Level level = rocket.level();
                Vec3 where = hit.getLocation();
                double x = rocket.getX();
                double y = rocket.getY();
                double z = rocket.getZ();

                playBoom(rocket);
                standardExplosion(rocket, hit, 20F, 3F, false, SLAG_CRACKED);
                ExplosionLarge.spawnShrapnels(level, where.x, where.y, where.z, 30);
                ExplosionChaos.burn(level, BlockPos.containing(where.x, where.y, where.z), 20);

                int radius = 30;
                for (Entity e : level.getEntities((Entity) null,
                        new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius),
                        entity -> true)) {
                    Effects.ignite(e, 5);
                    if (e instanceof LivingEntity living) {
                        Effects.addRegistered(living, ModEffects.PHOSPHORUS.get(), 30 * 20, 0);
                    }
                }

                ExplosionCreator.composeEffectRBMKMush(level, where.x, where.y, where.z, 15F);
            }
        },
        SMALL_TB("standard_tb", "himars_standard_tb", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult hit) {
                Level level = rocket.level();
                Vec3 where = hit.getLocation();
                playBoom(rocket);
                standardExplosion(rocket, hit, 20F, 10F, true, SLAG_CRACKED);
                ExplosionLarge.spawnShrapnels(level, where.x, where.y, where.z, 30);
                ExplosionCreator.composeEffectRBMKMush(level, where.x, where.y, where.z, 20F);
            }
        },
        LARGE_TB("single_tb", "himars_single_tb", 1) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult hit) {
                Level level = rocket.level();
                Vec3 where = hit.getLocation();
                playBoom(rocket);
                standardExplosion(rocket, hit, 50F, 12F, true, SLAG_CRACKED);
                ExplosionLarge.spawnShrapnels(level, where.x, where.y, where.z, 30);
                ExplosionCreator.composeEffectRBMKMush(level, where.x, where.y, where.z, 35F);
            }
        },
        SMALL_MINI_NUKE("standard_mini_nuke", "himars_standard_mini_nuke", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult hit) {
                Level level = rocket.level();
                Vec3 impact = behindImpact(rocket, hit);
                rocket.killAndClear();
                ExplosionNukeSmall.explode(level, impact.x, impact.y, impact.z, ExplosionNukeSmall.PARAMS_MEDIUM);
            }
        },
        SMALL_LAVA("standard_lava", "himars_standard_lava", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult hit) {
                standardExplosion(rocket, hit, 20F, 3F, true, MAGMA);
            }
        };

        public final String name;
        /** The skin the renderer paints on the rocket in flight. */
        public final Identifier texture;
        /** Six rockets to a pod of the small ones, one of the big. */
        public final int amount;
        /** 0 = the sixfold pod, 1 = the single rocket. */
        public final int modelType;

        HIMARSRocketType(String name, String texture, int type) {
            this.name = name;
            this.texture = Ids.of(com.hbm.Constants.MOD_ID, "textures/models/projectiles/" + texture + ".png");
            this.amount = type == 0 ? 6 : 1;
            this.modelType = type;
        }

        public abstract void onImpact(EntityArtilleryRocket rocket, HitResult hit);

        public void onUpdate(EntityArtilleryRocket rocket) {}
    }

    private static final Supplier<BlockState> SLAG_CRACKED =
            () -> ModBlocks.block_slag.get().defaultBlockState().setValue(BlockSlag.CRACKED, true);

    private static final Supplier<BlockState> MAGMA = () -> Blocks.MAGMA_BLOCK.defaultBlockState();

    public static HIMARSRocketType byIndex(int index) {
        HIMARSRocketType[] values = HIMARSRocketType.values();
        return values[Math.abs(index) % values.length];
    }

    private static void playBoom(EntityArtilleryRocket rocket) {
        rocket.level().playSound(null, rocket.getX(), rocket.getY(), rocket.getZ(),
                ModSounds.EXPLOSION_SMALL_NEAR.get(), SoundSource.BLOCKS, 20.0F,
                0.9F + rocket.level().getRandom().nextFloat() * 0.2F);
    }

    /** Upstream backs the blast off one motion step so a rocket never detonates inside what it hit. */
    private static Vec3 behindImpact(EntityArtilleryRocket rocket, HitResult hit) {
        Vec3 motion = rocket.getDeltaMovement();
        return hit.getLocation().subtract(motion.lengthSqr() < 1.0E-8D ? Vec3.ZERO : motion.normalize());
    }

    public static void standardExplosion(EntityArtilleryRocket rocket, HitResult hit, float size, float rangeMod,
            boolean breaksBlocks, Supplier<BlockState> slag) {
        Vec3 impact = behindImpact(rocket, hit);
        ExplosionVNT xnt = new ExplosionVNT(rocket.level(), impact.x, impact.y, impact.z, size);
        if (breaksBlocks) {
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop()
                    .withBlockEffect(new BlockMutatorDebris(slag)));
        }
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(rangeMod));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        rocket.killAndClear();
    }
}
