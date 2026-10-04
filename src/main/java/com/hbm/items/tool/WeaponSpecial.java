package com.hbm.items.tool;

import com.hbm.entity.projectile.EntityRubble;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.main.MainRegistry;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.potion.ModEffects;
import com.hbm.sound.ModGunSounds;
import dev.golem.core.world.CompatSwordItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.7 {@code WeaponSpecial}: a vanilla sword whose hit, right-click and tooltip each branch on which item
 * it is. The {@code this == ModItems.x} comparisons are the {@link Kind} passed at registration.
 *
 * <p>1.7's {@code hitEntity} returned {@code false} without calling {@code ItemSword}'s, so none of these
 * wear on an ordinary hit (only the caber's explicit 505). The sword's attack modifier is 1.7
 * {@code ItemSword}'s {@code 4 + material damage}. The Fiend achievement check in {@code onUpdate} is the
 * {@code fiend}/{@code fiend2} advancements' equipment criterion ({@code HbmAdvancementDeclarations}; the same test
 * as {@code ArmorUtil.checkForFiend}).
 */
public class WeaponSpecial extends CompatSwordItem {

    public enum Kind {
        SCHRABIDIUM_HAMMER, ULLAPOOL_CABER, SHIMMER_SLEDGE, SHIMMER_AXE, WOOD_GAVEL, LEAD_GAVEL, DIAMOND_GAVEL,
        MEMESPOON, STOPSIGN, SOPSIGN, CHERNOBYLSIGN, WRENCH_FLIPPED, BOTTLE_OPENER
    }

    private final Kind kind;

    public WeaponSpecial(Properties properties, ToolTier tier, Kind kind) {
        super(properties, tier, 4.0F + tier.attackDamageBonus(), Tools.SWORD_SPEED, movement(kind), false);
        this.kind = kind;
    }

    /** 1.7 {@code getItemAttributeModifiers}: the hammer, the shimmers and the flipped wrench slow the wielder. */
    private static double movement(Kind kind) {
        return switch (kind) {
            case SCHRABIDIUM_HAMMER -> -0.5;
            case SHIMMER_SLEDGE, SHIMMER_AXE -> -0.2;
            case WRENCH_FLIPPED -> -0.1;
            default -> 0.0;
        };
    }

    public Kind kind() {
        return kind;
    }

    private static void playAt(Level level, LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    protected void compatHurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity attacker) {
        Level world = entity.level();
        if (!(world instanceof ServerLevel server)) return;

        switch (kind) {
            case SCHRABIDIUM_HAMMER -> {
                entity.setHealth(0.0F);
                playAt(world, entity, ModMeleeKitItems.BONK.get(), 3.0F, 1.0F);
            }
            case ULLAPOOL_CABER -> {
                // createExplosion(null, x, y, z, 7.5, true): a smoking (block-breaking) blast, no fire
                world.explode(null, entity.getX(), entity.getY(), entity.getZ(), 7.5F, false, Level.ExplosionInteraction.TNT);
                Tools.hurt(stack, 505, attacker);
            }
            case SHIMMER_SLEDGE -> {
                push(entity, attacker.getLookAngle().scale(5));
                playAt(world, entity, ModMeleeKitItems.BANG.get(), 3.0F, 1.0F);
            }
            case SHIMMER_AXE -> {
                entity.setHealth(entity.getHealth() / 2);
                playAt(world, entity, ModMeleeKitItems.SLICE.get(), 3.0F, 1.0F);
            }
            case WOOD_GAVEL -> playAt(world, entity, ModGunSounds.WHACK.get(), 3.0F, 1.0F);
            case LEAD_GAVEL -> {
                playAt(world, entity, ModGunSounds.WHACK.get(), 3.0F, 1.0F);
                dev.golem.core.world.Effects.addRegistered(entity, ModEffects.LEAD.get(), 15 * 20, 4);
            }
            case DIAMOND_GAVEL -> {
                float ded = entity.getMaxHealth() / 3;
                entity.setHealth(entity.getHealth() - ded);
                playAt(world, entity, ModGunSounds.WHACK.get(), 3.0F, 1.0F);
            }
            case MEMESPOON -> memespoon(server, entity, attacker);
            case STOPSIGN, SOPSIGN -> playAt(world, entity, ModMeleeKitItems.STOP.get(), 1.0F, 1.0F);
            case BOTTLE_OPENER -> {
                // 1.7: one in seven hits blinds (5 min), slows III (5 min), fatigues III (5 min) or nauseates (1 min)
                int i = world.getRandom().nextInt(7);
                if (i == 0) dev.golem.core.world.Effects.add(entity, net.minecraft.world.effect.MobEffects.BLINDNESS, 5 * 60 * 20, 0);
                if (i == 1) dev.golem.core.world.Effects.add(entity, net.minecraft.world.effect.MobEffects.SLOWNESS, 5 * 60 * 20, 2);
                if (i == 2) dev.golem.core.world.Effects.add(entity, net.minecraft.world.effect.MobEffects.MINING_FATIGUE, 5 * 60 * 20, 2);
                if (i == 3) dev.golem.core.world.Effects.add(entity, net.minecraft.world.effect.MobEffects.NAUSEA, 1 * 60 * 20, 0);
                // 1.7 NTMSounds.VANILLA_ANVIL ("random.anvil_land")
                playAt(world, entity, net.minecraft.sounds.SoundEvents.ANVIL_LAND, 3.0F, 1.0F);
            }
            default -> { }
        }
    }

    /** The Market Gardener: crits (a flat 50) while falling, a VNT blast when falling far in survival. */
    private static void memespoon(ServerLevel world, LivingEntity entity, LivingEntity attacker) {
        if (!(attacker instanceof Player player)) return;

        if (attacker.fallDistance >= 2) {
            playAt(world, entity, ModMeleeKitItems.BANG.get(), 3.0F, 0.75F);
            Compat.hurt(entity, world, world.damageSources().playerAttack(player), 50F);
        }

        if (attacker.fallDistance >= 20 && !player.isCreative()) {
            double y = entity.getY() + entity.getBbHeight() / 2D;
            ExplosionVNT vnt = new ExplosionVNT(world, entity.getX(), y, entity.getZ(), 15, attacker);
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 150).setupPiercing(25, 0.5F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            ExplosionCreator.composeEffectSmall(world, entity.getX(), y, entity.getZ());
            vnt.explode();
        }
    }

    /** 1.7 added straight onto {@code motionX/Y/Z}; a player target only moves if the server says so. */
    static void push(LivingEntity entity, Vec3 delta) {
        entity.setDeltaMovement(entity.getDeltaMovement().add(delta));
        entity.hurtMarked = true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (kind == Kind.SHIMMER_SLEDGE) {
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000 && player != null) {
                if (!world.isClientSide()) {
                    EntityRubble rubble = new EntityRubble(world, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F);
                    rubble.setBlockState(state);
                    rubble.setDeltaMovement(rubble.getDeltaMovement().add(player.getLookAngle().scale(5)));
                    world.playSound(null, rubble.getX(), rubble.getY(), rubble.getZ(), ModMeleeKitItems.BANG.get(),
                            SoundSource.PLAYERS, 3.0F, 1.0F);
                    world.addFreshEntity(rubble);
                    world.destroyBlock(pos, false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (kind == Kind.SHIMMER_AXE) {
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModMeleeKitItems.KAPENG.get(),
                    SoundSource.PLAYERS, 3.0F, 1.0F);
            if (!world.isClientSide()) {
                timber(world, pos);
                timber(world, pos.above());
                timber(world, pos.below());
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    private static void timber(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isAir() && state.getBlock().getExplosionResistance() < 6000) world.destroyBlock(pos, false);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        switch (kind) {
            case SCHRABIDIUM_HAMMER -> {
                list.add(Component.literal("Even though it says \"+1000000000"));
                list.add(Component.literal("damage\", it's actually \"onehit anything\""));
            }
            case BOTTLE_OPENER -> {
                list.add(Component.literal("My very own bottle opener."));
                list.add(Component.literal("Use with caution!"));
            }
            case ULLAPOOL_CABER -> {
                list.add(Component.literal("High-yield Scottish face removal."));
                list.add(Component.literal("A sober person would throw it..."));
            }
            case SHIMMER_SLEDGE -> {
                if (MainRegistry.polaroidID == 11) {
                    list.add(Component.literal("shimmer no"));
                    list.add(Component.literal("drop that hammer"));
                    list.add(Component.literal("you're going to hurt somebody"));
                    list.add(Component.literal("shimmer no"));
                    list.add(Component.literal("shimmer pls"));
                } else {
                    list.add(Component.literal("Breaks everything, even portals."));
                }
            }
            case SHIMMER_AXE -> list.add(Component.literal(MainRegistry.polaroidID == 11
                    ? "shim's toolbox does an e-x-p-a-n-d" : "Timber!"));
            case WRENCH_FLIPPED -> list.add(Component.literal("Wrench 2: The Wrenchening"));
            case MEMESPOON -> {
                list.add(Component.literal("Level 10 Shovel").withStyle(ChatFormatting.DARK_GRAY));
                list.add(Component.literal("Deals crits while the wielder is rocket jumping").withStyle(ChatFormatting.AQUA));
                list.add(Component.literal("20% slower firing speed").withStyle(ChatFormatting.RED));
                list.add(Component.literal("No random critical hits").withStyle(ChatFormatting.RED));
            }
            case WOOD_GAVEL -> list.add(Component.literal("Thunk!"));
            case LEAD_GAVEL -> list.add(Component.literal("You are hereby sentenced to lead poisoning."));
            case DIAMOND_GAVEL -> {
                list.add(Component.literal("The joke! It makes sense now!!"));
                list.add(Component.literal(""));
                list.add(Component.literal("Deals as much damage as it needs to.").withStyle(ChatFormatting.BLUE));
            }
            default -> { }
        }
    }
}
