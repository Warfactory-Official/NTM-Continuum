package com.hbm.items.weapon.sedna;

import com.hbm.config.GeneralConfig;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.items.IKeybindReceiver;
import com.hbm.items.ModDataComponents;
import com.hbm.items.armor.ArmorTrenchmaster;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineEnergy;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.items.weapon.sedna.mags.MagazineInfinite;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.ModBatteryItems;
import com.hbm.lib.Library;
import com.hbm.packet.HbmPackets;
import com.hbm.util.SavedIds;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.Supplier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The base class for every Sedna-generation gun.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.ItemGunBaseNT}. The gun itself holds no
 * mutable state: everything lives on the {@link ItemStack}, so two copies of the same gun are
 * independent and a gun keeps its magazine when dropped, traded or stored.
 *
 * <h2>Where the state lives</h2>
 * Upstream keeps one NBT compound per stack and writes to it every tick on the server. That is kept:
 * the same key names, the same per-config-index suffixes, but split across two components:
 *
 * <ul>
 *   <li>{@link ModDataComponents#GUN_STATE}: the state machine's scratch space: timer, state,
 *       animation timer, button flags. Rewritten constantly.
 *   <li>{@link ModDataComponents#GUN_DATA}: what a player would be upset to lose: wear, fire mode,
 *       magazine contents.
 * </ul>
 *
 * <p>Both go through the {@code getValue*}/{@code setValue*} choke point below, which is also where the
 * <b>write-only-on-change</b> guard lives. Upstream can afford a blind write per tick; a data component
 * write marks the stack dirty and re-syncs it to the client, so a gun sitting idle in a hotbar would
 * otherwise generate a packet every tick forever.
 *
 * <p>Still outside this shared class: the tooltip's client HUD rendering and looped-sound map. Modifier
 * evaluation itself lives in {@code XWeaponModManager}; smoke node evolution is shared here so renderers
 * only consume the source-shaped attached list.
 */
public class ItemGunBaseNT extends CompatItem implements IKeybindReceiver {

    public static final List<ItemGunBaseNT> INSTANCES = new ArrayList<>();
    public static final List<Item> secrets = new ArrayList<>();

    /** Timestamp of the last shot per hand and config index, for muzzle flash and smoke on the client. */
    public long[][] lastShotByHand;
    /** Per-shot client rotation seed, copied from the source animation payload path. */
    public double[] shotRandByHand = new double[2];

    public static final String KEY_AIMING = "aiming";
    public static final String KEY_DRAWN = "drawn";
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
    public static final String O_GUNCONFIG = "O_GUNCONFIG_";

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

    /** {@code "#.##"} with US symbols, as the original {@code FORMAT_DMG}. */
    private static final DecimalFormat FORMAT_DMG = new DecimalFormat("#.##",
            DecimalFormatSymbols.getInstance(Locale.US));

    /** The original {@code addInformation}: one diagnostic block per receiver configuration, then the quality line. */
    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        Player player = ClientPlayerAccess.player();
        Container inventory = player == null ? null : player.getInventory();

        for (int configIndex = 0; configIndex < configs_DNA.length; configIndex++) {
            GunConfig config = getConfig(stack, configIndex);
            Receiver[] receivers = config == null ? null : config.getReceivers(stack);
            if (receivers == null) continue;
            for (Receiver receiver : receivers) {
                if (receiver == null) continue;
                IMagazine<?> magazine = receiver.getMagazine(stack);
                if (magazine == null) continue;
                if (!(magazine instanceof MagazineInfinite)) {
                    ItemStack icon = player != null ? magazine.getIconForHUD(stack, player)
                            : tooltipAmmoIcon(magazine, stack, magazine.getType(stack, null));
                    String state = player != null ? magazine.reportAmmoStateForHUD(stack, player)
                            : tooltipAmmoState(magazine, stack);
                    tooltip.add(Component.translatable("gui.weapon.ammo").append(": ")
                            .append(icon == null || icon.isEmpty() ? Component.empty() : icon.getHoverName())
                            .append(" " + state));
                }
                float damage = receiver.getBaseDamage(stack);
                tooltip.add(Component.translatable("gui.weapon.baseDamage").append(": " + FORMAT_DMG.format(damage)));
                if (magazine.getType(stack, inventory) instanceof BulletConfig bullet) {
                    int minimum = (int) (bullet.projectilesMin * receiver.getSplitProjectiles(stack));
                    int maximum = (int) (bullet.projectilesMax * receiver.getSplitProjectiles(stack));
                    tooltip.add(Component.translatable("gui.weapon.damageWithAmmo")
                            .append(": " + FORMAT_DMG.format(damage * bullet.damageMult)
                                    + (minimum > 1 ? (" x" + (minimum != maximum ? (minimum + "-" + maximum) : minimum)) : "")));
                }
            }
            float maximum = config.getDurability(stack);
            if (maximum > 0) {
                int condition = Math.max(0, Math.min(100,
                        (int) ((maximum - getWear(stack, configIndex)) * 100 / maximum)));
                tooltip.add(Component.translatable("gui.weapon.condition").append(": " + condition + "%"));
            }
            for (ItemStack modifier : XWeaponModManager.getUpgradeItems(stack, configIndex)) {
                if (modifier != null && !modifier.isEmpty()) {
                    tooltip.add(modifier.getHoverName().copy().withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        boolean blink = System.currentTimeMillis() % 1000 < 500; // BobMathUtil.getBlink()
        switch (quality) {
            case A_SIDE -> tooltip.add(Component.translatable("gui.weapon.quality.aside").withStyle(ChatFormatting.YELLOW));
            case B_SIDE -> tooltip.add(Component.translatable("gui.weapon.quality.bside").withStyle(ChatFormatting.GOLD));
            case LEGENDARY -> tooltip.add(Component.translatable("gui.weapon.quality.legendary").withStyle(ChatFormatting.RED));
            case SPECIAL -> tooltip.add(Component.translatable("gui.weapon.quality.special").withStyle(ChatFormatting.AQUA));
            case UTILITY -> tooltip.add(Component.translatable("gui.weapon.quality.utility").withStyle(ChatFormatting.GREEN));
            case SECRET -> tooltip.add(Component.translatable("gui.weapon.quality.secret")
                    .withStyle(blink ? ChatFormatting.DARK_RED : ChatFormatting.RED));
            case DEBUG -> tooltip.add(Component.translatable("gui.weapon.quality.debug")
                    .withStyle(blink ? ChatFormatting.YELLOW : ChatFormatting.GOLD));
        }

        // Only while the weapon table is open: every modifier this gun has a binding for.
        if (ClientPlayerAccess.weaponTableOpen()) {
            List<ItemStack> recognized = XWeaponModManager.recognizedMods(stack);
            if (!recognized.isEmpty()) {
                tooltip.add(Component.translatable("gui.weapon.accepts").append(":").withStyle(ChatFormatting.RED));
                for (ItemStack mod : recognized) {
                    tooltip.add(Component.literal("  ").append(mod.getHoverName()).withStyle(ChatFormatting.RED));
                }
            }
        }
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    private static ItemStack tooltipAmmoIcon(IMagazine<?> magazine, ItemStack stack, Object ammo) {
        if (ammo instanceof BulletConfig bullet) return bullet.toStack();
        if (ammo instanceof FluidType fluid && fluid != Fluids.NONE) return ItemFluidIcon.make(fluid);
        if (magazine instanceof MagazineEnergy
                || magazine instanceof MagazineElectricEngine) {
            return new ItemStack(ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_REDSTONE).get());
        }
        if (magazine instanceof MagazineFluid fluidMagazine) {
            FluidType fluid = fluidMagazine.getType(stack, null);
            return fluid == null || fluid == Fluids.NONE ? ItemStack.EMPTY : ItemFluidId.make(fluid);
        }
        return ItemStack.EMPTY;
    }

    private static String tooltipAmmoState(IMagazine<?> magazine, ItemStack stack) {
        if (magazine instanceof MagazineInfinite) return "∞";
        if (magazine instanceof MagazineEnergy
                || magazine instanceof MagazineElectricEngine) {
            return Library.getShortNumber(magazine.getAmount(stack, null)) + "/"
                    + Library.getShortNumber(magazine.getCapacity(stack)) + "HE";
        }
        if (magazine instanceof MagazineFluid) {
            return magazine.getAmount(stack, null) + "mB";
        }
        if (magazine instanceof MagazineLiquidEngine) {
            return magazine.getAmount(stack, null) + "/" + magazine.getCapacity(stack) + "mB";
        }
        if (magazine instanceof MagazineBelt) {
            return "x" + magazine.getAmount(stack, null);
        }
        return magazine.getAmount(stack, null) + " / " + magazine.getCapacity(stack);
    }

    public Supplier<? extends Item> defaultAmmo;
    public int defaultAmmoAmount;
    public boolean isDefaultExpensive;
    public Function<ItemStack, String> LAMBDA_NAME_MUTATOR;

    public ItemGunBaseNT(Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties.stacksTo(1));
        this.configs_DNA = cfg;
        this.quality = quality;
        this.lastShotByHand = new long[2][cfg.length];
        for (int i = 0; i < cfg.length; i++) cfg[i].index = i;
        INSTANCES.add(this);
        if (quality == WeaponQuality.LEGENDARY || quality == WeaponQuality.SECRET) secrets.add(this);
    }

    /** ntm-next constructor order retained for factory compatibility. */
    public ItemGunBaseNT(WeaponQuality quality, Properties properties, GunConfig... cfg) {
        this(properties, quality, cfg);
    }

    public GunConfig getConfig(ItemStack stack, int index) {
        return XWeaponModManager.eval(configs_DNA[index], stack, O_GUNCONFIG + index, this, index);
    }

    public ItemGunBaseNT setDefaultAmmo(Supplier<? extends Item> ammo, int amount) {
        this.defaultAmmo = ammo;
        this.defaultAmmoAmount = amount;
        return this;
    }

    public ItemGunBaseNT setDefaultAmmoExpensive(Supplier<? extends Item> ammo, int amount) {
        this.isDefaultExpensive = true;
        return setDefaultAmmo(ammo, amount);
    }

    public ItemGunBaseNT setNameMutator(Function<ItemStack, String> lambda) {
        this.LAMBDA_NAME_MUTATOR = lambda;
        return this;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (LAMBDA_NAME_MUTATOR != null) {
            String key = LAMBDA_NAME_MUTATOR.apply(stack);
            if (key != null) return Component.translatable(key);
        }
        return super.getName(stack);
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

    public static boolean getIsDrawn(ItemStack stack) { return getValueBool(stack, KEY_DRAWN); }
    public static void setIsDrawn(ItemStack stack, boolean value) { setValueBool(stack, KEY_DRAWN, value); }

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
     * every tick. Sustained fire is the decider's job, not the keybind's.
     */
    public void handleKeybind(LivingEntity entity, Container inventory, ItemStack stack,
            EnumKeybind keybind, boolean newState) {
        if (!GeneralConfig.enableGuns) return; // 1.7 1.20_enableGuns
        for (int i = 0; i < configs_DNA.length; i++) {
            GunConfig config = getConfig(stack, i);
            LambdaContext ctx = new LambdaContext(config, entity, inventory, handOf(entity, stack), i);

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
        onEquip(player, stack, handOf(player, stack));
    }

    /** Resets one hand's button state and plays its draw animation. */
    public void onEquip(Player player, ItemStack stack, InteractionHand hand) {
        for (int i = 0; i < configs_DNA.length; i++) {
            playAnimation(player, stack, GunAnimation.EQUIP, hand, i);
            setPrimary(stack, i, false);
            setSecondary(stack, i, false);
            setTertiary(stack, i, false);
            setReloadKey(stack, i, false);
        }
    }

    public static void playAnimation(Player player, ItemStack stack, GunAnimation type, int index) {
        playAnimation(player, stack, type, handOf(player, stack), index);
    }

    public static void playAnimation(Player player, ItemStack stack, GunAnimation type, InteractionHand hand,
            int index) {
        if (player instanceof ServerPlayer serverPlayer) {
            HbmPackets.sendGunAnimation(serverPlayer, stack, type, hand, 0, index);
        }
        // a mob shooter has no player; its orchestra still plays from these
        setLastAnim(stack, index, type);
        setAnimTimer(stack, index, 0);
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

    /** Runs each held gun's client-owned portion once, including hand-local smoke node evolution. */
    public static void clientHeldTick(Player player) {
        if (player == null) return;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack aimingStack = main.getItem() instanceof ItemGunBaseNT ? main : off;

        prevAimingProgress = aimingProgress;
        boolean aiming = aimingStack.getItem() instanceof ItemGunBaseNT && getIsAiming(aimingStack);
        if (aiming && aimingProgress < 1F) aimingProgress += 0.25F;
        if (!aiming && aimingProgress > 0F) aimingProgress -= 0.25F;
        aimingProgress = Math.max(0F, Math.min(1F, aimingProgress));

        clientHeldTick(player, InteractionHand.MAIN_HAND);
        // A stack is normally present in only one hand; do not evolve one history twice if a caller or
        // fixture temporarily aliases the same stack into both slots.
        if (off != main) clientHeldTick(player, InteractionHand.OFF_HAND);
    }

    private static void clientHeldTick(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return;

        for (int i = 0; i < gun.configs_DNA.length; i++) {
            GunConfig config = gun.getConfig(stack, i);
            LambdaContext context = new LambdaContext(config, player, player.getInventory(), hand, i);
            BiConsumer<ItemStack, LambdaContext> smoke = config.getSmokeHandler(stack);
            if (smoke != null) smoke.accept(stack, context);
            BiConsumer<ItemStack, LambdaContext> orchestra = config.getOrchestra(stack);
            if (orchestra != null) orchestra.accept(stack, context);
        }
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
     * <p>Server-side only: Golem's item facade filters the client out. Everything is decided on the
     * server and the client learns about it through the stack's synced components plus the animation
     * payload. Upstream's client half of {@code onUpdate}, the aim transition, lives in the client tick
     * handler.
     */
    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level,
            Entity entity, EquipmentSlot slotType, boolean isHeld) {
        serverTick(stack, level, entity, isHeld,
                slotType == EquipmentSlot.OFFHAND
                        ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    /**
     * Advances one gun by one tick. Split out from the vanilla hook so tests, and anything else that
     * has to drive a gun without an inventory tick behind it, such as a turret, can call it directly.
     */
    public void serverTick(ItemStack stack, ServerLevel level,
            Entity entity, boolean isHeld) {
        serverTick(stack, level, entity, isHeld, handOf(entity, stack));
    }

    public void serverTick(ItemStack stack, ServerLevel level,
            Entity entity, boolean isHeld, InteractionHand hand) {
        if (!(entity instanceof LivingEntity living)) return;
        Player player = entity instanceof Player p ? p : null;

        int confNo = configs_DNA.length;
        GunConfig[] configs = new GunConfig[confNo];
        LambdaContext[] ctx = new LambdaContext[confNo];
        for (int i = 0; i < confNo; i++) {
            configs[i] = getConfig(stack, i);
            ctx[i] = new LambdaContext(configs[i], living, player != null ? player.getInventory() : null,
                    hand, i);
        }

        if (player != null && !getIsEquipped(stack) && isHeld) onEquip(player, stack, hand);
        setIsEquipped(stack, isHeld);

        // Put the gun away and it re-draws when next selected: you cannot dodge a reload by
        // hotbar-swapping. A jam is not cleared this way.
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

        // 1.7: a Trenchmaster runs a reloading config's tick twice (the k loop), which is his faster reload.
        for (int i = 0; i < confNo; i++) for (int k = 0; k == 0 || (k < 2
                && ArmorTrenchmaster.isTrenchMaster(player)
                && getState(stack, i) == GunState.RELOADING); k++) {
            // Keep the upstream order: cue zero is played on the tick before the animation timer moves.
            if (configs[i].getOrchestra(stack) != null) {
                configs[i].getOrchestra(stack).accept(stack, ctx[i]);
            }
            setAnimTimer(stack, i, getAnimTimer(stack, i) + 1);

            int timer = getTimer(stack, i);
            if (timer > 0) setTimer(stack, i, timer - 1);
            // The decider runs on the tick the timer reaches its end, and every tick while idle at 0.
            if (timer <= 1) {
                var decider = configs[i].getDecider(stack);
                if (decider == null) {
                    throw new IllegalStateException("Gun config " + i + " on "
                            + getClass().getName() + " has no state decider");
                }
                decider.accept(stack, ctx[i]);
            }
        }
    }

    /**
     * A gun rewrites its own animation clock every tick, and that is not a change of weapon.
     *
     * <p>Without this the hand renderer treats each tick's state as a fresh draw and never finishes
     * raising the gun. Guns posed in camera space never notice; the fire extinguisher, which keeps the
     * pose vanilla hands it, would stay below the bottom of the screen.
     */
    @Override
    public boolean compatIsSameHeldItem(ItemStack shown, ItemStack held) {
        return shown.is(held.getItem());
    }

    /**
     * A held gun must never break a block: the left button is the trigger.
     *
     * <p>This is only half the story: it stops the block from breaking, but the client still plays the
     * swing and the cracking overlay. Suppressing the attack key itself is the client-side half, in the
     * keybind handler.
     */
    @Override
    public boolean compatCanDestroyBlock(ItemStack stack,
            BlockState state, Level level,
            BlockPos pos, LivingEntity user) {
        return false;
    }

    /* STATE ACCESSORS: all per config index. */

    public static int getTimer(ItemStack stack, int index) { return getValueInt(stack, KEY_TIMER + index); }
    public static void setTimer(ItemStack stack, int index, int value) { setValueInt(stack, KEY_TIMER + index, value); }
    // state and last animation are saved by enum name, never by ordinal; absent = first constant
    public static GunState getState(ItemStack stack, int index) { return savedEnum(GunState.class, getValueString(stack, KEY_STATE + index)); }
    public static void setState(ItemStack stack, int index, GunState value) { setValueString(stack, KEY_STATE + index, value.name()); }
    public static int getMode(ItemStack stack, int index) { return getValueInt(stack, KEY_MODE + index); }
    public static void setMode(ItemStack stack, int index, int value) { setValueInt(stack, KEY_MODE + index, value); }
    public static boolean getIsAiming(ItemStack stack) { return getValueBool(stack, KEY_AIMING); }
    public static void setIsAiming(ItemStack stack, boolean value) { setValueBool(stack, KEY_AIMING, value); }
    public static float getWear(ItemStack stack, int index) { return getValueFloat(stack, KEY_WEAR + index); }
    public static void setWear(ItemStack stack, int index, float value) { setValueFloat(stack, KEY_WEAR + index, value); }
    // 1.7 kept the target's network entity id here; the port keeps its UUID (numeric ids are never persisted)
    // and resolves it on the server where 1.7 called getEntityByID.
    public static String getLockonTarget(ItemStack stack) { return getValueString(stack, KEY_LOCKONTARGET); }
    public static void setLockonTarget(ItemStack stack, Entity target) {
        setValueString(stack, KEY_LOCKONTARGET, target.getUUID().toString());
    }
    /** The locked-on entity in this level, or null (1.7 {@code worldObj.getEntityByID(getLockonTarget(stack))}). */
    public static net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity resolveLockonTarget(
            Level level, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return null;
        String id = getLockonTarget(stack);
        if (id.isEmpty()) return null;
        try {
            return server.getEntity(UUID.fromString(id));
        } catch (IllegalArgumentException notAUuid) {
            return null; // an old numeric save: no target, as if none was locked
        }
    }
    public static boolean getIsLockedOn(ItemStack stack) { return getValueBool(stack, KEY_LOCKEDON); }
    public static void setIsLockedOn(ItemStack stack, boolean value) { setValueBool(stack, KEY_LOCKEDON, value); }
    public static GunAnimation getLastAnim(ItemStack stack, int index) { return savedEnum(GunAnimation.class, getValueString(stack, KEY_LASTANIM + index)); }
    public static void setLastAnim(ItemStack stack, int index, GunAnimation value) { setValueString(stack, KEY_LASTANIM + index, value.name()); }
    private static <E extends Enum<E>> E savedEnum(Class<E> type, String name) {
        E first = type.getEnumConstants()[0];
        return name.isEmpty() ? first : SavedIds.byName(type, name, first);
    }
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

    /** Identity, rather than item type, distinguishes two identical akimbo stacks. */
    private static InteractionHand handOf(Entity entity, ItemStack stack) {
        return entity instanceof Player player && player.getItemInHand(InteractionHand.OFF_HAND) == stack
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /* THE CHOKE POINT */

    /**
     * Which bucket a key belongs to. Everything the state machine scribbles on goes to the volatile
     * component; anything a player would notice losing goes to the durable one.
     */
    private static boolean isPersistent(String key) {
        return key.startsWith(KEY_WEAR) || key.startsWith(KEY_MODE) || key.startsWith("mag")
                || key.startsWith("KEY_MOD_LIST_") || key.startsWith("KEY_MOD_NAMES_") || key.equals("coincount") || key.equals("coincharge")
                || key.equals("colors") || key.equals("flashlight");
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
    private static void write(ItemStack stack, String key, Consumer<CompoundTag> mutator) {
        var component = isPersistent(key) ? ModDataComponents.GUN_DATA : ModDataComponents.GUN_STATE;
        CompoundTag current = component.getOr(stack, EMPTY);
        CompoundTag next = current.copy();
        mutator.accept(next);
        if (!next.equals(current)) component.set(stack, next);
    }

    private static final CompoundTag EMPTY = new CompoundTag();

    /** Snapshot helpers used by the modifier manager for its dynamic per-config integer lists. */
    public static CompoundTag getGunData(ItemStack stack) {
        return ModDataComponents.GUN_DATA.getOr(stack, EMPTY).copy();
    }

    public static void setGunData(ItemStack stack, CompoundTag data) {
        ModDataComponents.GUN_DATA.set(stack, data);
    }

    public static int getValueInt(ItemStack stack, String name) { return Tags.getIntOr(read(stack, name), name, 0); }
    public static void setValueInt(ItemStack stack, String name, int value) { write(stack, name, tag -> tag.putInt(name, value)); }

    public static float getValueFloat(ItemStack stack, String name) { return Tags.getFloatOr(read(stack, name), name, 0F); }
    public static void setValueFloat(ItemStack stack, String name, float value) { write(stack, name, tag -> tag.putFloat(name, value)); }

    public static boolean getValueBool(ItemStack stack, String name) { return Tags.getBooleanOr(read(stack, name), name, false); }
    public static void setValueBool(ItemStack stack, String name, boolean value) { write(stack, name, tag -> tag.putBoolean(name, value)); }

    public static String getValueString(ItemStack stack, String name) { return Tags.getStringOr(read(stack, name), name, ""); }
    public static void setValueString(ItemStack stack, String name, String value) { write(stack, name, tag -> tag.putString(name, value)); }

    public static int[] getValueIntArray(ItemStack stack, String name) {
        return Tags.getIntArrayOr(read(stack, name), name, new int[0]);
    }

    public static void setValueIntArray(ItemStack stack, String name, int[] value) {
        write(stack, name, tag -> tag.putIntArray(name, value));
    }

    public static void removeValue(ItemStack stack, String name) {
        write(stack, name, tag -> tag.remove(name));
    }

    /** Extra context handed to every gun lambda. */
    public static class LambdaContext {
        public final GunConfig config;
        public final LivingEntity entity;
        public final Container inventory;
        public final InteractionHand hand;
        public final int configIndex;

        public LambdaContext(GunConfig config, LivingEntity entity, Container inventory,
                InteractionHand hand, int configIndex) {
            this.config = config;
            this.entity = entity;
            this.inventory = inventory;
            this.hand = hand;
            this.configIndex = configIndex;
        }

        public Player getPlayer() {
            return entity instanceof Player player ? player : null;
        }
    }

    /** One mutable segment in a gun's client-side smoke ribbon. */
    public static class SmokeNode {
        public double forward;
        public double side;
        public double lift;
        public double alpha;
        public double width = 1D;

        public SmokeNode(double alpha) {
            this.alpha = alpha;
        }
    }

    /** The magazine of the given config's primary receiver, for HUD and tooltip use. */
    public IMagazine<BulletConfig> getPrimaryMagazine(ItemStack stack, int index) {
        Receiver[] receivers = getConfig(stack, index).getReceivers(stack);
        return receivers.length == 0 ? null : receivers[0].getMagazine(stack);
    }
}
