package com.hbm.items.weapon;

import com.hbm.Constants;
import com.hbm.items.tool.ItemSwordAbility;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ToolTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * 1.7 {@code ItemCrucible}: the demon-core sword with three charges.
 *
 * <p>1.7 kept the charge in the item damage (0 full .. 3 empty, the CRUCIBLE material's three uses) and
 * never let a hit take it past empty. Vanilla wear breaks an item as soon as its damage reaches the
 * maximum, so the port counts the same 0..3 in {@link #SPENT} on an unbreakable sword, and shows it as the
 * durability bar 1.7 drew.
 *
 * <p>1.7 {@code getAttributeModifiers} returned no modifiers at all once the charge was spent (damage and speed
 * both gone, a bare-hand hit). A modern item's attributes are fixed per item, so the spent stack carries an
 * empty modifier override ({@code ItemStacks.suppressAttributeModifiers}), set the moment the last charge is used
 * and re-checked every inventory tick (a recharge in the tungsten crate returns a fresh stack anyway).
 *
 * <p>The deploy (on equip, {@code cDeploy} sound) and swing animations start from the server as in 1.7 ({@code onEquip},
 * {@code onEntitySwing} -> {@code IAnimatedItem.playAnimation}); the tables are {@code ToolAnimations.crucible}.
 * Not ported: the Tankish recharge easter egg.
 */
public class ItemCrucible extends ItemSwordAbility
        implements dev.golem.core.world.SwingReceiver, com.hbm.items.IAnimatedItem, com.hbm.items.IEquipReceiver {

    /** 1.7 item damage: 0 is full, {@link #MAX_SPENT} is empty. */
    public static final GolemComponent<Integer> SPENT = GolemComponent.integer(Constants.MOD_ID, "crucible_spent");
    public static final int MAX_SPENT = 3;

    public ItemCrucible(Properties properties, float damage, double movement, ToolTier material) {
        super(properties, damage, movement, material);
    }

    /** Forces the component declaration before the registries flush. */
    public static void init() {}

    public static int spent(ItemStack stack) {
        return SPENT.getOr(stack, 0);
    }

    public static ItemStack withSpent(ItemStack stack, int spent) {
        SPENT.set(stack, Math.max(0, Math.min(MAX_SPENT, spent)));
        syncModifiers(stack);
        return stack;
    }

    /** 1.7: the modifiers exist only while {@code getItemDamage() < getMaxDamage()}. */
    public static void syncModifiers(ItemStack stack) {
        boolean empty = spent(stack) >= MAX_SPENT;
        if (empty != dev.golem.core.world.ItemStacks.attributeModifiersSuppressed(stack)) {
            dev.golem.core.world.ItemStacks.suppressAttributeModifiers(stack, empty);
        }
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.entity.Entity owner,
                                       net.minecraft.world.entity.EquipmentSlot slot, boolean held) {
        super.compatInventoryTick(stack, level, owner, slot, held);
        syncModifiers(stack);
    }

    @Override
    protected boolean canOperate(ItemStack stack) {
        return spent(stack) < MAX_SPENT;
    }

    @Override
    protected void compatHurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        boolean active = canOperate(stack);

        if (active) {
            attacker.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.PLAYERS, 1.0F,
                    0.75F + victim.getRandom().nextFloat() * 0.2F);

            if (attacker.level() instanceof ServerLevel level && !victim.isAlive()) {
                int count = Math.min((int) Math.ceil(victim.getMaxHealth() / 3D), 250);
                // 1.7 "vanillaburst" of redstone-block dust: count * 4 particles at motion 0.1.
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), count * 4,
                        0.0D, 0.0D, 0.0D, 0.1D);
            }

            super.compatHurtEnemy(stack, victim, attacker);
            if (!attacker.level().isClientSide()) withSpent(stack, spent(stack) + 1);
        } else if (!attacker.level().isClientSide() && attacker instanceof Player player) {
            player.sendSystemMessage(Component.literal("Not enough energy.").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        StringBuilder charge = new StringBuilder("Charge [");
        for (int i = 2; i >= 0; i--) charge.append(spent(stack) <= i ? "||||||" : "   ");
        charge.append("]");
        list.add(Component.literal(charge.toString()).withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return spent(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - spent(stack) * 13.0F / MAX_SPENT);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0F, (float) (MAX_SPENT - spent(stack)) / MAX_SPENT) / 3.0F, 1.0F, 1.0F);
    }

    // ---- G-C: 1.7 IEquipReceiver / onEntitySwing (animation cues) ----

    /** 1.7 {@code onEquip}: a charged crucible plays the deploy sound at the player and the deploy animation. */
    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer)) return;
        if (canOperate(stack)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.hbm.items.ModAbilityToolItems.CRUCIBLE_DEPLOY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            playAnimation(player, com.hbm.items.ToolAnimation.EQUIP, stack);
        }
    }

    /** 1.7 {@code onEntitySwing}: a charged crucible swings (the client picks the random arc); never cancels. */
    @Override
    public boolean compatOnEntitySwing(ItemStack stack, LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)
                || hand != net.minecraft.world.InteractionHand.MAIN_HAND) return false;
        if (!canOperate(stack)) return false;
        playAnimation(player, com.hbm.items.ToolAnimation.SWING, stack);
        return false;
    }
}
