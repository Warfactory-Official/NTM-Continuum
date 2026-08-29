package com.hbm.items.weapon.sedna;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.IKeybindReceiver;
import com.hbm.items.ModDataComponents;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import dev.golem.core.data.Tags;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The base class for every Sedna-generation gun.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.ItemGunBaseNT}. The gun itself holds no
 * mutable state: everything lives on the {@link ItemStack}, so two copies of the same gun are
 * independent and a gun keeps its magazine when dropped, traded or stored.
 *
 * <h2>Where the state lives</h2>
 * Upstream keeps one NBT compound per stack and writes to it every tick on the server. That is kept —
 * the same key names, the same per-config-index suffixes — but split across two components:
 *
 * <ul>
 *   <li>{@link ModDataComponents#GUN_STATE} — the state machine's scratch space: timer, state,
 *       animation timer, button flags. Rewritten constantly.
 *   <li>{@link ModDataComponents#GUN_DATA} — what a player would be upset to lose: wear, fire mode,
 *       magazine contents.
 * </ul>
 *
 * <p>Both go through the {@code getValue*}/{@code setValue*} choke point below, which is also where the
 * <b>write-only-on-change</b> guard lives. Upstream can afford a blind write per tick; a data component
 * write marks the stack dirty and re-syncs it to the client, so a gun sitting idle in a hotbar would
 * otherwise generate a packet every tick forever.
 *
 * <p>Not ported in this slice: the tooltip's weapon-mod and quality lines, {@code getItemStackDisplayName}'s
 * name mutator, {@code renderHUD} (see the HUD components), smoke nodes, and the looped-sound map.
 */
public class ItemGunBaseNT extends dev.golem.core.world.CompatItem implements IKeybindReceiver {

    public static final List<ItemGunBaseNT> INSTANCES = new ArrayList<>();

    /** Timestamp of the last shot per config index, for muzzle flash and smoke on the client. */
    public long[] lastShot;

    public static final String KEY_AIMING = "aiming";
    public static final String KEY_MODE = "mode_";
    public static final String KEY_WEAR = "wear_";
    public static final String KEY_TIMER = "timer_";
    public static final String KEY_STATE = "state_";
    public static final String KEY_PRIMARY = "mouse1_";
    public static final String KEY_SECONDARY = "mouse2_";
    public static final String KEY_TERTIARY = "mouse3_";
    public static final String KEY_RELOAD = "reload_";
    public static final String KEY_LASTANIM = "lastanim_";
    public static final String KEY_ANIMTIMER = "animtimer_";
    public static final String KEY_LOCKONTARGET = "lockontarget";
    public static final String KEY_LOCKEDON = "lockedon";
    public static final String KEY_CANCELRELOAD = "cancel";
    /** Spelled as upstream spells it, typo included, so saved stacks stay readable across the port. */
    public static final String KEY_EQUIPPED = "eqipped";

    /** How far the aim transition has progressed on the client, [0;1]. */
    public static float prevAimingProgress;
    public static float aimingProgress;

    /** Gen-3 camera recoil accumulator, kept in the same global shape as upstream. */
    public static float recoilVertical;
    public static float recoilHorizontal;
    public static float recoilDecay = 0.75F;
    public static float recoilRebound = 0.25F;
    public static float offsetVertical;
    public static float offsetHorizontal;

    protected final GunConfig[] configs_DNA;
    public final WeaponQuality quality;

    public ItemGunBaseNT(Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties.stacksTo(1));
        this.configs_DNA = cfg;
        this.quality = quality;
        this.lastShot = new long[cfg.length];
        for (int i = 0; i < cfg.length; i++) cfg[i].index = i;
        INSTANCES.add(this);
    }

    public GunConfig getConfig(ItemStack stack, int index) {
        return configs_DNA[index];
    }

    /** Guards client payloads before they can reach a config or its magazine. */
    public boolean isValidConfigIndex(int index) {
        return index >= 0 && index < configs_DNA.length;
    }

    public int getConfigCount() {
        return configs_DNA.length;
    }

    public enum WeaponQuality {
        A_SIDE, B_SIDE, LEGENDARY, UTILITY, SPECIAL, SECRET, DEBUG
    }

    /** Upstream exposes only these qualities in the ordinary weapon creative tab. */
    public boolean isCreativeTabVisible() {
        return quality == WeaponQuality.A_SIDE || quality == WeaponQuality.SPECIAL
                || quality == WeaponQuality.UTILITY;
    }

    public enum GunState {
        /** Forced delay in which nothing can be done. */
        DRAWING,
        /** Ready to fire or reload. */
        IDLE,
        /** Forced delay after firing, which may refire. */
        COOLDOWN,
        /** Forced delay resolving into a reload action; may be cancelled. */
        RELOADING,
        /** Forced delay from a jam. */
        JAMMED;

        public static final GunState[] VALUES = values();

        public static GunState byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : DRAWING;
        }
    }

    /* KEYBINDS */

    @Override
    public boolean canHandleKeybind(Player player, ItemStack stack, EnumKeybind keybind) {
        return keybind == EnumKeybind.GUN_PRIMARY || keybind == EnumKeybind.GUN_SECONDARY
                || keybind == EnumKeybind.GUN_TERTIARY || keybind == EnumKeybind.RELOAD;
    }

    @Override
    public void handleKeybind(Player player, ItemStack stack, EnumKeybind keybind, boolean newState) {
        handleKeybind(player, player.getInventory(), stack, keybind, newState);
    }

    /**
     * Routes one key edge to the matching press/release lambda on every config.
     *
     * <p>The stored button flag is what makes this edge-triggered: the client sends state, and a press
     * handler only runs when the flag actually changes, so holding a key does not re-fire the handler
     * every tick — sustained fire is the decider's job, not the keybind's.
     */
    public void handleKeybind(LivingEntity entity, Container inventory, ItemStack stack,
            EnumKeybind keybind, boolean newState) {
        for (int i = 0; i < configs_DNA.length; i++) {
            GunConfig config = getConfig(stack, i);
            LambdaContext ctx = new LambdaContext(config, entity, inventory, i);

            switch (keybind) {
                case GUN_PRIMARY -> edge(stack, i, newState, getPrimary(stack, i),
                        config.getPressPrimary(stack), config.getReleasePrimary(stack), ctx,
                        (s, v) -> setPrimary(s, ctx.configIndex, v));
                case GUN_SECONDARY -> edge(stack, i, newState, getSecondary(stack, i),
                        config.getPressSecondary(stack), config.getReleaseSecondary(stack), ctx,
                        (s, v) -> setSecondary(s, ctx.configIndex, v));
                case GUN_TERTIARY -> edge(stack, i, newState, getTertiary(stack, i),
                        config.getPressTertiary(stack), config.getReleaseTertiary(stack), ctx,
                        (s, v) -> setTertiary(s, ctx.configIndex, v));
                case RELOAD -> edge(stack, i, newState, getReloadKey(stack, i),
                        config.getPressReload(stack), config.getReleaseReload(stack), ctx,
                        (s, v) -> setReloadKey(s, ctx.configIndex, v));
                default -> { }
            }
        }
    }

    private void edge(ItemStack stack, int index, boolean newState, boolean oldState,
            BiConsumer<ItemStack, LambdaContext> onPress, BiConsumer<ItemStack, LambdaContext> onRelease,
            LambdaContext ctx, BiConsumer<ItemStack, Boolean> store) {
        if (newState == oldState) return;
        BiConsumer<ItemStack, LambdaContext> handler = newState ? onPress : onRelease;
        if (handler != null) handler.accept(stack, ctx);
        store.accept(stack, newState);
    }

    /** Resets button state and plays the draw animation when the gun is brought out. */
    public void onEquip(Player player, ItemStack stack) {
        for (int i = 0; i < configs_DNA.length; i++) {
            playAnimation(player, stack, GunAnimation.EQUIP, i);
            setPrimary(stack, i, false);
            setSecondary(stack, i, false);
            setTertiary(stack, i, false);
            setReloadKey(stack, i, false);
        }
    }

    public static void playAnimation(Player player, ItemStack stack, GunAnimation type, int index) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.hbm.packet.HbmPackets.sendGunAnimation(serverPlayer, stack, type, index);
            setLastAnim(stack, index, type);
            setAnimTimer(stack, index, 0);
        }
    }

    /** Adds one upstream recoil impulse; the client tick consumes the accumulated values. */
    public static void setupRecoil(float vertical, float horizontal, float decay, float rebound) {
        recoilVertical += vertical;
        recoilHorizontal += horizontal;
        recoilDecay = decay;
        recoilRebound = rebound;
    }

    public static void setupRecoil(float vertical, float horizontal) {
        setupRecoil(vertical, horizontal, 0.75F, 0.25F);
    }

    /** Clears the client-only recoil accumulators when visual recoil is disabled. */
    public static void clearRecoil() {
        offsetVertical = 0;
        offsetHorizontal = 0;
        recoilVertical = 0;
        recoilHorizontal = 0;
    }

    /** The frame-interpolated ADS value shared by the weapon pose and camera FOV. */
    public static float aimingProgress(float partialTick) {
        return prevAimingProgress + (aimingProgress - prevAimingProgress) * partialTick;
    }

    /** Applies one upstream renderer's ADS FOV factor without involving client-only state. */
    public static float applyAimingFov(float fov, float partialTick, float zoom) {
        return fov * (1F - aimingProgress(partialTick) * zoom);
    }

    /**
     * The per-tick driver, and the only place gun state advances.
     *
     * <p>Server-side only — Golem's item facade already filters the client out, which suits this design
     * exactly: everything is decided on the server and the client learns about it through the stack's
     * synced components plus the animation payload. Upstream's client half of {@code onUpdate}, the aim
     * transition, lives in the client tick handler instead.
     */
    @Override
    protected void compatInventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
            Entity entity, net.minecraft.world.entity.EquipmentSlot slotType, boolean isHeld) {
        serverTick(stack, level, entity, isHeld);
    }

    /**
     * Advances one gun by one tick. Split out from the vanilla hook so tests — and anything else that
     * has to drive a gun without an inventory tick behind it, such as a turret — can call it directly.
     */
    public void serverTick(ItemStack stack, net.minecraft.server.level.ServerLevel level,
            Entity entity, boolean isHeld) {
        if (!(entity instanceof LivingEntity living)) return;
        Player player = entity instanceof Player p ? p : null;

        int confNo = configs_DNA.length;
        GunConfig[] configs = new GunConfig[confNo];
        LambdaContext[] ctx = new LambdaContext[confNo];
        for (int i = 0; i < confNo; i++) {
            configs[i] = getConfig(stack, i);
            ctx[i] = new LambdaContext(configs[i], living, player != null ? player.getInventory() : null, i);
        }

        if (player != null && !getIsEquipped(stack) && isHeld) onEquip(player, stack);
        setIsEquipped(stack, isHeld);

        // Put the gun away and it re-draws when next selected — you cannot dodge a reload by
        // hotbar-swapping. A jam is deliberately not cleared this way.
        if (!isHeld) {
            for (int i = 0; i < confNo; i++) {
                if (getState(stack, i) != GunState.JAMMED) {
                    setState(stack, i, GunState.DRAWING);
                    setTimer(stack, i, configs[i].getDrawDuration(stack));
                }
                // Prevents a freshly created gun from starting life at (DRAWING, 0).
                setLastAnim(stack, i, GunAnimation.CYCLE);
            }
            setIsAiming(stack, false);
            setReloadCancel(stack, false);
            return;
        }

        for (int i = 0; i < confNo; i++) {
            setAnimTimer(stack, i, getAnimTimer(stack, i) + 1);

            int timer = getTimer(stack, i);
            if (timer > 0) setTimer(stack, i, timer - 1);
            // The decider runs on the tick the timer reaches its end, and every tick while idle at 0.
            if (timer <= 1 && configs[i].getDecider(stack) != null) {
                configs[i].getDecider(stack).accept(stack, ctx[i]);
            }
        }
    }

    /**
     * A held gun must never break a block: the left button is the trigger.
     *
     * <p>This is only half the story — it stops the block from breaking, but the client still plays the
     * swing and the cracking overlay. Suppressing the attack key itself is the client-side half, in the
     * keybind handler.
     */
    @Override
    public boolean compatCanDestroyBlock(ItemStack stack,
            net.minecraft.world.level.block.state.BlockState state, Level level,
            net.minecraft.core.BlockPos pos, LivingEntity user) {
        return false;
    }

    /* STATE ACCESSORS — all per config index. */

    public static int getTimer(ItemStack stack, int index) { return getValueInt(stack, KEY_TIMER + index); }
    public static void setTimer(ItemStack stack, int index, int value) { setValueInt(stack, KEY_TIMER + index, value); }
    public static GunState getState(ItemStack stack, int index) { return GunState.byOrdinal(getValueInt(stack, KEY_STATE + index)); }
    public static void setState(ItemStack stack, int index, GunState value) { setValueInt(stack, KEY_STATE + index, value.ordinal()); }
    public static int getMode(ItemStack stack, int index) { return getValueInt(stack, KEY_MODE + index); }
    public static void setMode(ItemStack stack, int index, int value) { setValueInt(stack, KEY_MODE + index, value); }
    public static boolean getIsAiming(ItemStack stack) { return getValueBool(stack, KEY_AIMING); }
    public static void setIsAiming(ItemStack stack, boolean value) { setValueBool(stack, KEY_AIMING, value); }
    public static float getWear(ItemStack stack, int index) { return getValueFloat(stack, KEY_WEAR + index); }
    public static void setWear(ItemStack stack, int index, float value) { setValueFloat(stack, KEY_WEAR + index, value); }
    public static int getLockonTarget(ItemStack stack) { return getValueInt(stack, KEY_LOCKONTARGET); }
    public static void setLockonTarget(ItemStack stack, int value) { setValueInt(stack, KEY_LOCKONTARGET, value); }
    public static boolean getIsLockedOn(ItemStack stack) { return getValueBool(stack, KEY_LOCKEDON); }
    public static void setIsLockedOn(ItemStack stack, boolean value) { setValueBool(stack, KEY_LOCKEDON, value); }
    public static GunAnimation getLastAnim(ItemStack stack, int index) { return GunAnimation.byOrdinal(getValueInt(stack, KEY_LASTANIM + index)); }
    public static void setLastAnim(ItemStack stack, int index, GunAnimation value) { setValueInt(stack, KEY_LASTANIM + index, value.ordinal()); }
    public static int getAnimTimer(ItemStack stack, int index) { return getValueInt(stack, KEY_ANIMTIMER + index); }
    public static void setAnimTimer(ItemStack stack, int index, int value) { setValueInt(stack, KEY_ANIMTIMER + index, value); }

    public static boolean getPrimary(ItemStack stack, int index) { return getValueBool(stack, KEY_PRIMARY + index); }
    public static void setPrimary(ItemStack stack, int index, boolean value) { setValueBool(stack, KEY_PRIMARY + index, value); }
    public static boolean getSecondary(ItemStack stack, int index) { return getValueBool(stack, KEY_SECONDARY + index); }
    public static void setSecondary(ItemStack stack, int index, boolean value) { setValueBool(stack, KEY_SECONDARY + index, value); }
    public static boolean getTertiary(ItemStack stack, int index) { return getValueBool(stack, KEY_TERTIARY + index); }
    public static void setTertiary(ItemStack stack, int index, boolean value) { setValueBool(stack, KEY_TERTIARY + index, value); }
    public static boolean getReloadKey(ItemStack stack, int index) { return getValueBool(stack, KEY_RELOAD + index); }
    public static void setReloadKey(ItemStack stack, int index, boolean value) { setValueBool(stack, KEY_RELOAD + index, value); }
    public static boolean getReloadCancel(ItemStack stack) { return getValueBool(stack, KEY_CANCELRELOAD); }
    public static void setReloadCancel(ItemStack stack, boolean value) { setValueBool(stack, KEY_CANCELRELOAD, value); }
    public static boolean getIsEquipped(ItemStack stack) { return getValueBool(stack, KEY_EQUIPPED); }
    public static void setIsEquipped(ItemStack stack, boolean value) { setValueBool(stack, KEY_EQUIPPED, value); }

    /* THE CHOKE POINT */

    /**
     * Which bucket a key belongs to. Everything the state machine scribbles on goes to the volatile
     * component; anything a player would notice losing goes to the durable one.
     */
    private static boolean isPersistent(String key) {
        return key.startsWith(KEY_WEAR) || key.startsWith(KEY_MODE) || key.startsWith("mag");
    }

    private static CompoundTag read(ItemStack stack, String key) {
        return (isPersistent(key) ? ModDataComponents.GUN_DATA : ModDataComponents.GUN_STATE)
                .getOr(stack, EMPTY);
    }

    /**
     * Writes one key back, but only when the value actually changed.
     *
     * <p>This guard is the reason the per-tick writes upstream performs are affordable here: setting a
     * component marks the stack dirty and re-syncs it, so an unconditional write would put a gun that is
     * merely <i>being held</i> into a permanent packet loop.
     */
    private static void write(ItemStack stack, String key, java.util.function.Consumer<CompoundTag> mutator) {
        var component = isPersistent(key) ? ModDataComponents.GUN_DATA : ModDataComponents.GUN_STATE;
        CompoundTag current = component.getOr(stack, EMPTY);
        CompoundTag next = current.copy();
        mutator.accept(next);
        if (!next.equals(current)) component.set(stack, next);
    }

    private static final CompoundTag EMPTY = new CompoundTag();

    public static int getValueInt(ItemStack stack, String name) { return Tags.getIntOr(read(stack, name), name, 0); }
    public static void setValueInt(ItemStack stack, String name, int value) { write(stack, name, tag -> tag.putInt(name, value)); }

    public static float getValueFloat(ItemStack stack, String name) { return Tags.getFloatOr(read(stack, name), name, 0F); }
    public static void setValueFloat(ItemStack stack, String name, float value) { write(stack, name, tag -> tag.putFloat(name, value)); }

    public static boolean getValueBool(ItemStack stack, String name) { return Tags.getBooleanOr(read(stack, name), name, false); }
    public static void setValueBool(ItemStack stack, String name, boolean value) { write(stack, name, tag -> tag.putBoolean(name, value)); }

    public static String getValueString(ItemStack stack, String name) { return Tags.getStringOr(read(stack, name), name, ""); }
    public static void setValueString(ItemStack stack, String name, String value) { write(stack, name, tag -> tag.putString(name, value)); }

    /** Extra context handed to every gun lambda. */
    public static class LambdaContext {
        public final GunConfig config;
        public final LivingEntity entity;
        public final Container inventory;
        public final int configIndex;

        public LambdaContext(GunConfig config, LivingEntity entity, Container inventory, int configIndex) {
            this.config = config;
            this.entity = entity;
            this.inventory = inventory;
            this.configIndex = configIndex;
        }

        public Player getPlayer() {
            return entity instanceof Player player ? player : null;
        }
    }

    /** The magazine of the given config's primary receiver, for HUD and tooltip use. */
    public IMagazine<BulletConfig> getPrimaryMagazine(ItemStack stack, int index) {
        Receiver[] receivers = getConfig(stack, index).getReceivers(stack);
        return receivers.length == 0 ? null : receivers[0].getMagazine(stack);
    }
}
