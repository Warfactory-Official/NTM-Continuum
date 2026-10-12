package com.hbm.handler.ability;

import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.potion.ModEffects;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemStacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code IWeaponAbility}: what a hit does besides damage. */
public interface IWeaponAbility extends IBaseAbility {

    // Note: tool is currently unused in weapon abilities
    void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool);

    int SORT_ORDER_BASE = 200;

    /** 1.7 {@code entityDropItem(stack, 0F)}: at the entity's feet, with the default pickup delay. */
    static void drop(LivingEntity living, ItemStack stack) {
        drop(living, stack, 0F);
    }

    /** 1.7 {@code entityDropItem(stack, yOffset)}. */
    static void drop(LivingEntity living, ItemStack stack, float yOffset) {
        ItemEntity item = new ItemEntity(living.level(), living.getX(), living.getY() + yOffset, living.getZ(), stack);
        item.setDefaultPickUpDelay();
        living.level().addFreshEntity(item);
    }

    IWeaponAbility NONE = new IWeaponAbility() {
        @Override public String getName() { return ""; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 0; }
        @Override public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) { }
    };

    IWeaponAbility RADIATION = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.radiation"; }

        public final float[] radAtLevel = { 15F, 50F, 500F };

        @Override public int levels() { return radAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + radAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 1; }

        @Override
        public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) {
            if (victim instanceof LivingEntity living)
                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, radAtLevel[level]);
        }
    };

    IWeaponAbility VAMPIRE = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.vampire"; }

        public final float[] amountAtLevel = { 2F, 3F, 5F, 10F, 50F };

        @Override public int levels() { return amountAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + amountAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 2; }

        @Override
        public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) {
            float amount = amountAtLevel[level];

            if (victim instanceof LivingEntity living) {
                if (living.getHealth() <= 0) return;
                living.setHealth(living.getHealth() - amount);
                // 1.7 called onDeath(DamageSource.magic) itself once the health it set ran out.
                if (living.getHealth() <= 0) living.die(world.damageSources().magic());
                player.heal(amount);
            }
        }
    };

    IWeaponAbility STUN = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.stun"; }

        public final int[] durationAtLevel = { 2, 3, 5, 10, 15 };

        @Override public int levels() { return durationAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + durationAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 3; }

        @Override
        public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) {
            int duration = durationAtLevel[level];

            if (victim instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration * 20, 4));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration * 20, 4));
            }
        }
    };

    IWeaponAbility PHOSPHORUS = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.phosphorus"; }

        public final int[] durationAtLevel = { 60, 90 };

        @Override public int levels() { return durationAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + durationAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 4; }

        @Override
        public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) {
            int duration = durationAtLevel[level];

            if (victim instanceof LivingEntity living) {
                Effects.addRegistered(living, ModEffects.PHOSPHORUS.get(), duration * 20, 4);
            }
        }
    };

    IWeaponAbility FIRE = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.fire"; }

        public final int[] durationAtLevel = { 5, 10 };

        @Override public int levels() { return durationAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + durationAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 6; }

        @Override
        public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) {
            if (victim instanceof LivingEntity) {
                // 1.7 setFire(seconds): Fire Protection shortens it (vanilla dampener) and it never shortens a longer burn.
                dev.golem.core.world.Effects.ignite(victim, durationAtLevel[level]);
            }
        }
    };

    IWeaponAbility CHAINSAW = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.chainsaw"; }

        public final int[] dividerAtLevel = { 15, 10 };

        @Override public int levels() { return dividerAtLevel.length; }
        @Override public String getExtension(int level) { return " (1:" + dividerAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 7; }

        @Override
        public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) {
            int divider = dividerAtLevel[level];

            if (victim instanceof LivingEntity living) {
                if (living.getHealth() <= 0.0F) {
                    // safeguard to prevent funnies from bosses with obscene health
                    int count = Math.min((int) Math.ceil(living.getMaxHealth() / divider), 250);

                    for (int i = 0; i < count; i++) {
                        drop(living, new ItemStack(ModItems.nitra_small.get()), 1F); // 1.7 entityDropItem(stack, 1)
                        world.addFreshEntity(new ExperienceOrb(world, living.getX(), living.getY(), living.getZ(), 1));
                    }

                    ConfettiUtil.gib(living);
                    world.playSound(null, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(),
                            ModAbilityToolItems.CHAINSAW_SOUND.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
                }
            }
        }
    };

    IWeaponAbility BEHEADER = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.beheader"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 8; }

        @Override
        public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) {
            if (victim instanceof LivingEntity living && living.getHealth() <= 0.0F) {
                EntityType<?> type = living.getType();

                // 1.7 EntitySkeleton covered both skeleton types (0 normal, 1 wither); strays are the
                // modern normal-skeleton variant. EntityZombie covered zombie villagers and pigmen, which
                // the modern Zombie class still does.
                if (type == EntityType.SKELETON || type == EntityType.STRAY) {
                    drop(living, new ItemStack(Items.SKELETON_SKULL));
                } else if (type == EntityType.WITHER_SKELETON) {
                    if (world.getRandom().nextInt(20) == 0)
                        drop(living, new ItemStack(Items.WITHER_SKELETON_SKULL));
                    else
                        drop(living, new ItemStack(Items.COAL, 3));
                } else if (living instanceof net.minecraft.world.entity.monster.zombie.Zombie) {
                    drop(living, new ItemStack(Items.ZOMBIE_HEAD));
                } else if (type == EntityType.CREEPER) {
                    drop(living, new ItemStack(Items.CREEPER_HEAD));
                } else if (type == EntityType.MAGMA_CUBE) {
                    drop(living, new ItemStack(Items.MAGMA_CREAM, 3));
                } else if (type == EntityType.SLIME) {
                    drop(living, new ItemStack(Items.SLIME_BALL, 3));
                } else if (living instanceof Player victimPlayer) {
                    drop(living, ItemStacks.playerHead(victimPlayer));
                } else {
                    drop(living, new ItemStack(Items.ROTTEN_FLESH, 3));
                    drop(living, new ItemStack(Items.BONE, 2));
                }
            }
        }
    };

    IWeaponAbility BOBBLE = new IWeaponAbility() {
        @Override public String getName() { return "weapon.ability.bobble"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 9; }

        @Override
        public void onHit(int level, ServerLevel world, Player player, Entity victim, Item tool) {
            if (victim instanceof net.minecraft.world.entity.monster.Monster mob && mob.getHealth() <= 0.0F) {
                int chance = 1000;
                if (mob.getMaxHealth() > 20) chance = 750;

                // 1.7: a random bobblehead, never the NONE type (mob.entityDropItem at the mob's feet)
                if (world.getRandom().nextInt(chance) == 0) {
                    int type = world.getRandom().nextInt(com.hbm.blocks.generic.BlockBobble.BobbleType.VALUES.length - 1) + 1;
                    drop(mob, new net.minecraft.world.item.ItemStack(com.hbm.blocks.ModDecoCrateBlocks.bobblehead_items.get(type).get()));
                }
            }
        }
    };

    /**
     * Every ability of this kind by its name ({@link #getName()}, which tool presets save), 1.7's in 1.7 order. An
     * addon ability registers here ({@code REGISTRY.register(MY_ABILITY)}) at mod construction so a saved preset finds
     * it again; giving it to a tool is the tool's own {@code addAbility}.
     */
    com.hbm.api.registry.NamedRegistry<IWeaponAbility> REGISTRY = com.hbm.api.registry.NamedRegistry.of(
            "weapon ability", IWeaponAbility::getName, NONE, RADIATION, VAMPIRE, STUN, PHOSPHORUS, FIRE, CHAINSAW, BEHEADER, BOBBLE);

    static IWeaponAbility getByName(String name) {
        IWeaponAbility ability = REGISTRY.get(name);
        return ability != null ? ability : NONE;
    }
}
