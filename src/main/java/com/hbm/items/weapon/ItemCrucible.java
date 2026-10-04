package com.hbm.items.weapon;

import com.hbm.Constants;
import com.hbm.config.GeneralConfig;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * old-hbm {@code ItemCrucible}: the demon-core sword, an {@link ItemSwordCutter} with {@value #DEFAULT_MAX} charges
 * ({@code GeneralConfig.crucibleMaxCharges}), replacing the 1.7 three-charge sword.
 *
 * <ul>
 *   <li>The charge is the stack's {@code charges} count; every hit spends one (except a hit while a cut is being
 *       drawn: {@code doSpecialClick}) and every cut that hits something spends one. The tooltip bar and the
 *       durability bar show it. The creative tab holds a charged crucible; the recipe makes an empty one (item damage
 *       3 of the CRUCIBLE material's 10,000). The tungsten crate's laser recharges one holding fewer than three.</li>
 *   <li>Charged: 5000 attack damage (1.7 value; effective in full because CommonClass raises vanilla's 2048 ATTACK_DAMAGE bound) and +100% movement; empty: 5 damage and +80% movement. The stack-sensitive
 *       {@code getAttributeModifiers} override named no attack speed, so the player's base speed applies.</li>
 *   <li>Every hit also wears the sword by two (1.12 {@code ItemSwordAbility.hitEntity} and {@code ItemSword.hitEntity}
 *       each damaged it by one); the durability bar shows the charge instead, as in old-hbm.</li>
 *   <li>Equip: the deploy animation and, when charged, {@code weapon.cDeploy} at volume 5. Swing: the random swing
 *       animation unless drawing a cut (in first person the 1.7 crucible swing; in third person vanilla's arm swing,
 *       which is no longer cancelled), and {@code weapon.crucible_swing} when charged.</li>
 *   <li>A charged hit that kills bursts the victim into redstone-block dust ({@code ceil(maxHealth / 3)} capped at 250,
 *       four particles each).</li>
 * </ul>
 */
public class ItemCrucible extends ItemSwordCutter implements dev.golem.core.world.HeldItemState {

    public static final int DEFAULT_MAX = 16;
    /** old-hbm NBT {@code charges}. */
    public static final GolemComponent<Integer> CHARGES = GolemComponent.integer(Constants.MOD_ID, "charges");
    /** old-hbm {@code cSwing}: SWING_ROT/SWING_TRANS over 75 + 150 + 500 ms. */
    public static final int CSWING_MILLIS = 725;

    /**
     * {@code ItemCrucible.doSpecialClick}, sent every client tick from {@code updateClient} (AuxButton 1000). old-hbm kept
     * one static flag for the whole server, which only made sense in single player; the port keeps it per player.
     */
    private static final Map<UUID, Boolean> SPECIAL_CLICK = new ConcurrentHashMap<>();

    /**
     * Players whose last hit was free ({@code doSpecialClick}) and whose cut has not arrived yet. The flag is client
     * state the server cannot check, and old-hbm honoured it unconditionally, so a client that always sent it hit for
     * free forever. An honest free hit is the left click that also sends the drawn cut (attack first, then the swing
     * that sends {@code PacketMobSlicer}), so it is always followed by its cut: one free hit per cut is exactly what an
     * honest client can use, and a further hit before the cut arrives spends a charge as usual.
     */
    private static final java.util.Set<UUID> FREE_HIT_OPEN = ConcurrentHashMap.newKeySet();

    /** old-hbm {@code getAttributeModifiers(slot, stack)} named no attack speed: the player's base speed (no modifier). */
    public ItemCrucible(Properties properties, float damage, double movement, ToolTier material) {
        super(properties, damage, 0F, movement, material);
    }

    /** Forces the component declaration before the registries flush. */
    public static void init() {}

    public static int maxCharges() {
        return GeneralConfig.crucibleMaxCharges <= 0 ? DEFAULT_MAX : GeneralConfig.crucibleMaxCharges;
    }

    public static int getCharges(ItemStack stack) {
        return CHARGES.getOr(stack, 0);
    }

    /** {@code charge}: full charge. */
    public static ItemStack charge(ItemStack stack) {
        CHARGES.set(stack, maxCharges());
        syncModifiers(stack);
        return stack;
    }

    /** {@code discharge}: one charge less, never below zero. */
    public static void discharge(ItemStack stack) {
        CHARGES.set(stack, Math.max(0, getCharges(stack) - 1));
        syncModifiers(stack);
    }

    public static void setSpecialClick(Player player, boolean clicked) {
        if (player.getMainHandItem().getItem() instanceof ItemCrucible) SPECIAL_CLICK.put(player.getUUID(), clicked);
    }

    /**
     * The client's flag, honoured only while the main hand holds a charged crucible. old-hbm's flag went stale when the
     * last charge was spent mid-cut (the client stops reporting at zero charges), which hid the empty crucible's swing
     * until it was recharged; an empty crucible is never "drawing a cut" here.
     */
    public static boolean doSpecialClick(LivingEntity entity) {
        ItemStack held = entity.getMainHandItem();
        return held.getItem() instanceof ItemCrucible && getCharges(held) > 0
                && Boolean.TRUE.equals(SPECIAL_CLICK.get(entity.getUUID()));
    }

    /** Forgets the flag (the last charge was spent). */
    static void clearSpecialClick(LivingEntity entity) {
        SPECIAL_CLICK.remove(entity.getUUID());
    }

    /** A cut arrived from {@code player}: the free hit it opened, if any, is accounted for. */
    static void cutArrived(LivingEntity player) {
        FREE_HIT_OPEN.remove(player.getUUID());
    }

    /** Takes the one free hit a cut allows; false if one is already waiting for its cut. */
    private static boolean takeFreeHit(LivingEntity attacker) {
        return FREE_HIT_OPEN.add(attacker.getUUID());
    }

    static boolean hasPlayerState() {
        return !SPECIAL_CLICK.isEmpty() || !FREE_HIT_OPEN.isEmpty();
    }

    /** Drops the state of players {@code absent} says are gone (logout). */
    static void forgetPlayers(java.util.function.Predicate<UUID> absent) {
        SPECIAL_CLICK.keySet().removeIf(absent);
        FREE_HIT_OPEN.removeIf(absent);
    }

    /**
     * The stack-sensitive {@code getAttributeModifiers}: the registered modifiers are the charged ones; an empty stack
     * carries an override with {@code damage 5} and {@code movement * 0.8}.
     */
    public static void syncModifiers(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemCrucible crucible)) return;
        if (getCharges(stack) > 0) {
            ItemStacks.suppressAttributeModifiers(stack, false);
        } else {
            ItemStacks.overrideMainHandAttributes(stack, 5F, 0F, crucible.movement * 0.8F);
        }
    }

    /** {@code getSubItems}: the creative tab holds a charged crucible. */
    public ItemStack creativeStack() {
        return charge(new ItemStack(this));
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, net.minecraft.world.entity.Entity owner,
                                       net.minecraft.world.entity.EquipmentSlot slot, boolean held) {
        super.compatInventoryTick(stack, level, owner, slot, held);
        syncModifiers(stack);
    }

    /**
     * {@code onEquip(player, hand)}: the cutter's deploy animation, then (charged only) {@code cDeploy} at the player's
     * block position, volume 5.
     */
    @Override
    public void onEquip(Player player, ItemStack stack) {
        super.onEquip(player, stack);
        if (getCharges(stack) == 0) return;
        if (!(player instanceof ServerPlayer)) return;
        var pos = player.blockPosition();
        player.level().playSound(null, pos, ModAbilityToolItems.CRUCIBLE_DEPLOY.get(), SoundSource.PLAYERS, 5.0F, 1.0F);
    }

    /**
     * {@code onEquip(player, OFF_HAND)}: old-hbm sent the deploy animation to the off-hand slot (which its renderer never
     * draws: an off-hand crucible is invisible in first person unless the main hand holds one too) and, charged, played
     * {@code cDeploy}. The sound is the observable part, so that is what happens here.
     */
    @Override
    public void onEquipOffHand(Player player, ItemStack stack) {
        if (getCharges(stack) == 0 || !(player instanceof ServerPlayer)) return;
        player.level().playSound(null, player.blockPosition(), ModAbilityToolItems.CRUCIBLE_DEPLOY.get(), SoundSource.PLAYERS,
                5.0F, 1.0F);
    }

    /**
     * 1.12 fired {@code onEquip} whenever the held stack's item <em>or damage</em> changed ({@code areItemsEqual}); a
     * hit wears the crucible, so the deploy (animation and sound) replays after every hit, as it did in old-hbm.
     */
    @Override
    public Object equipKey(ItemStack stack) {
        return List.of(stack.getItem(), stack.getDamageValue());
    }

    /**
     * {@code onEntitySwing}: the client half of the cutter (send a drawn cut). On the server: unless a cut is being
     * drawn the attack animation ({@code cSwing} cue; the 1.7 swing bus, see {@code CrucibleClient.swing17}) is started
     * on the holder's client, and a charged crucible plays {@code weapon.crucible_swing} at the swinger.
     *
     * <p>User decision (2026-10-02): vanilla's arm swing is <em>not</em> cancelled any more (old-hbm returned true on
     * both sides; 1.7's crucible returned false). Third person (F5, and every other player through the server's swing
     * broadcast) shows the normal sword swing; first person still shows only the crucible's own animation, because its
     * first-person renderer clears the local swing as 1.7's did ({@code isSwingInProgress = false}).
     */
    @Override
    public boolean compatOnEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof ServerPlayer player)) {
            super.compatOnEntitySwing(stack, entity, hand);
            return false;
        }
        if (!doSpecialClick(player) && hand == InteractionHand.MAIN_HAND) {
            com.hbm.packet.CrucibleAnimPayload.send(player, com.hbm.packet.CrucibleAnimPayload.CSWING);
            // the client starts cSwing only while no swing runs (SWING_ROT x back at 0)
            if (!busRunning(player)) busStarted(player, CSWING_MILLIS);
        }
        if (getCharges(stack) > 0) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModAbilityToolItems.CRUCIBLE_SWING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return false;
    }

    /**
     * old-hbm drew the first-person crucible from {@code RenderSpecificHandEvent} at equip progress 0 whatever the hand
     * renderer was doing, so a charge spent (a component change) never lowered it: the same item is the same raise.
     * (The deploy still replays on a hit, through {@link #equipKey} on the server, as in 1.12.)
     */
    @Override
    public boolean compatIsSameHeldItem(ItemStack shown, ItemStack held) {
        return shown.getItem() == held.getItem();
    }

    /** ... and fully raised: no modern attack-cooldown dip (Core's tick-time raise, not a renderer pin). */
    @Override
    public boolean compatHoldsRaised(ItemStack held) {
        return true;
    }

    @Override
    public byte getTexId() {
        return 1;
    }

    /**
     * {@code hitEntity}: unless drawing a cut, spend a charge, and a charged kill bursts into redstone dust; then the
     * sword ability's hit (no abilities on the crucible) and the extra point of wear {@code ItemSwordAbility} added.
     */
    @Override
    protected void compatHurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        boolean server = !attacker.level().isClientSide();
        if (!(doSpecialClick(attacker) && (!server || takeFreeHit(attacker)))) {
            discharge(stack);
            if (server && getCharges(stack) == 0) clearSpecialClick(attacker);
            if (!attacker.level().isClientSide() && getCharges(stack) > 0 && !victim.isAlive()) {
                int count = Math.min((int) Math.ceil(victim.getMaxHealth() / 3D), 250);
                CompoundTag data = new CompoundTag();
                data.putInt("count", count * 4);
                data.putDouble("motion", 0.1D);
                data.putInt("block", BuiltInRegistries.BLOCK.getId(Blocks.REDSTONE_BLOCK));
                double y = victim.getY() + victim.getBbHeight() * 0.5;
                for (ServerPlayer player : ((ServerLevel) attacker.level()).players()) {
                    if (player.distanceToSqr(victim.getX(), y, victim.getZ()) > 50D * 50D) continue;
                    HbmEffectNT.VanillaBurst_BlockDust.sendTo(player, victim.getX(), y, victim.getZ(), data);
                }
            }
        }
        super.compatHurtEnemy(stack, victim, attacker);
        Tools.hurt(stack, 1, attacker);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        StringBuilder charge = new StringBuilder("Charge [");
        int charges = getCharges(stack);
        for (int i = 0; i < maxCharges(); i++) charge.append(charges > i ? "||||||" : "   ");
        charge.append("]");
        list.add(Component.literal(charge.toString()).withStyle(ChatFormatting.RED));
    }

    /** {@code showDurabilityBar}: always. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    /** {@code getDurabilityForDisplay = 1 - charges / max}, drawn {@code round(13 - health * 13)} wide. */
    @Override
    public int getBarWidth(ItemStack stack) {
        double health = 1D - (double) getCharges(stack) / maxCharges();
        return (int) Math.round(13.0D - health * 13.0D);
    }

    /** 1.12 {@code getRGBDurabilityForDisplay}: hue {@code (1 - health) / 3}. */
    @Override
    public int getBarColor(ItemStack stack) {
        float health = (float) (1D - (double) getCharges(stack) / maxCharges());
        return Mth.hsvToRgb(Math.max(0.0F, 1.0F - health) / 3.0F, 1.0F, 1.0F);
    }
}
