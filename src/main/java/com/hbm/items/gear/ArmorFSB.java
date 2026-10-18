package com.hbm.items.gear;

import com.hbm.handler.ArmorUtil;
import com.hbm.handler.ArmorModHandler;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.handler.HazmatRegistry;
import com.hbm.items.ModGearItems;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.ShadyUtil;
import dev.golem.core.render.Worn;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Effects;
import dev.golem.core.world.GolemArmorMaterial;
import dev.golem.core.world.Attributes;
import com.hbm.items.armor.ItemModInsert;
import com.hbm.util.TickPhase;
import dev.golem.core.world.StepHeight;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Full-set-bonus armour: the base every modelled HBM suit is built on.
 *
 * <p>Ported 1:1 from 1.12.2 {@code com.hbm.items.gear.ArmorFSB}. The idea is that a suit does nothing
 * special piece by piece: wear <em>all four</em> pieces of the same material and the chestplate's
 * configuration switches on for the whole set. That covers potion effects re-applied every tick, radiation
 * resistance, hazard-class protection, footstep sounds and the hard-landing shockwave.
 *
 * <p>Set identity is <b>material equality</b>, not an item tag. Upstream compared {@code ArmorMaterial}
 * enum constants; here {@link GolemArmorMaterial} instances are compared by reference, so each suit must
 * declare exactly one material object and share it across its four pieces.
 *
 * <p>Two departures from the 1.12.2 class, both because the target API differs:
 * <ul>
 * <li>There is no armour-layer <b>texture path</b> parameter: from 26.1 that is the material's equipment
 *     asset. Upstream passed a dummy path ({@code starmetal_1.png}) for every modelled suit anyway; the real
 *     body came from the OBJ model.
 * <li>{@code onArmorTick} is not an override ({@code Item.inventoryTick} is server-only from 26.1 and the
 *     1.12.2 hook has no cross-loader equivalent). The per-tick work is driven from
 *     {@code EntityEffectHandler}, which the {@code MixinLivingEntity} tick injection runs on both sides; see
 *     {@link #armorTick}.
 * </ul>
 */
public class ArmorFSB extends CompatArmorItem implements IArmorDisableModel, IFillableItem {

    /** The material this piece belongs to. Set membership is reference equality on this object. */
    public final GolemArmorMaterial material;
    /** Which piece this is. Needed because 26.1 does not expose a slot from the item. */
    public final ArmorSlot slot;

    public List<MobEffectInstance> effects = new ArrayList<>();
    public boolean noHelmet = false;
    public boolean vats = false;
    public boolean thermal = false;
    public boolean geigerSound = false;
    public boolean customGeiger = false;
    public boolean hardLanding = false;
    public int dashCount = 0;
    public int stepSize = 0;
    public Supplier<SoundEvent> step;
    public Supplier<SoundEvent> jump;
    public Supplier<SoundEvent> fall;
    public double radResist = 0;
    private Identifier overlay = null;
    private String armorModel = null;

    private final Set<EnumPlayerPart> hidden = EnumSet.noneOf(EnumPlayerPart.class);
    private boolean needsFullSet = false;

    public ArmorFSB(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties);
        this.material = material;
        this.slot = slot;
    }

    // Legacy FluidLoaderFillableItem exposed attached reservoirs to every machine container slot.
    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
            if (mod.getItem() instanceof IFillableItem fillable && fillable.acceptsFluid(type, mod)) return true;
        }
        return false;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (stack.getCount() != 1 || amount <= 0) return amount;
        for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
            if (mod.getItem() instanceof IFillableItem fillable && fillable.acceptsFluid(type, mod)) {
                int remaining = fillable.tryFill(type, amount, mod);
                if (remaining < amount) ArmorModHandler.applyMod(stack, mod);
                amount = remaining;
                if (amount == 0) break;
            }
        }
        return amount;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
            if (mod.getItem() instanceof IFillableItem fillable && fillable.providesFluid(type, mod)) return true;
        }
        return false;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (stack.getCount() != 1 || amount <= 0) return 0;
        int drained = 0;
        for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
            if (mod.getItem() instanceof IFillableItem fillable && fillable.providesFluid(type, mod)) {
                int moved = fillable.tryEmpty(type, amount - drained, mod);
                if (moved > 0) ArmorModHandler.applyMod(stack, mod);
                drained += moved;
                if (drained == amount) break;
            }
        }
        return drained;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
            if (mod.getItem() instanceof IFillableItem fillable) {
                FluidType type = fillable.getFirstFluidType(mod);
                if (type != null && type != Fluids.NONE) return type;
            }
        }
        return null;
    }

    @Override
    public int getFill(ItemStack stack) {
        int fill = 0;
        for (ItemStack mod : ArmorModHandler.pryMods(stack)) {
            if (mod.getItem() instanceof IFillableItem fillable) fill += fillable.getFill(mod);
        }
        return fill;
    }

    // Set detection

    /**
     * Whether {@code entity} is wearing a complete, <em>working</em> set.
     *
     * <p>Every armour slot must hold an {@link ArmorFSB} of the chestplate's material and must answer
     * {@link #isArmorEnabled}, which is how "out of power" disables a powered suit's bonuses without
     * unequipping anything. The head slot is skipped for suits declaring {@link #setNoHelmet}.
     */
    public static boolean hasFSBArmor(Player entity) {
        return hasSet(entity, true);
    }

    /** As {@link #hasFSBArmor}, but requires the set to actually use a helmet. */
    public static boolean hasFSBArmorHelmet(Player entity) {
        if (entity == null) return false;
        ItemStack plate = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (plate.getItem() instanceof ArmorFSB chestplate) {
            return !chestplate.noHelmet && hasFSBArmor(entity);
        }
        return false;
    }

    /**
     * As {@link #hasFSBArmor}, but ignores {@link #isArmorEnabled}.
     *
     * <p>This is what the HUD and the model both ask: a suit at zero charge is still <em>worn</em>, so it
     * still renders and still shows its (empty) power bars. Only its bonuses stop.
     */
    public static boolean hasFSBArmorIgnoreCharge(Player entity) {
        return hasSet(entity, false);
    }

    /**
     * As {@link #hasFSBArmorIgnoreCharge(Player)}, from the worn stacks alone.
     *
     * <p>The renderer has no entity to ask from 26.1 on. It is handed a render state, so the set check
     * has to be expressible over just the four stacks. This is that form; the {@link Player} overload
     * reads the slots and delegates.
     */
    public static boolean hasFSBArmorIgnoreCharge(Worn worn) {
        if (!(worn.chest().getItem() instanceof ArmorFSB chestplate)) return false;

        for (ArmorSlot slot : ArmorSlot.values()) {
            if (chestplate.noHelmet && slot == ArmorSlot.HELMET) continue;
            if (!(worn.get(slot).getItem() instanceof ArmorFSB piece)) return false;
            if (piece.material != chestplate.material) return false;
        }
        return true;
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static boolean hasSet(Player entity, boolean requireEnabled) {
        if (entity == null) return false;

        ItemStack plate = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!(plate.getItem() instanceof ArmorFSB chestplate)) return false;

        boolean noHelmet = chestplate.noHelmet;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (noHelmet && slot == EquipmentSlot.HEAD) continue;

            ItemStack armor = entity.getItemBySlot(slot);
            if (!(armor.getItem() instanceof ArmorFSB piece)) return false;
            if (piece.material != chestplate.material) return false;
            if (requireEnabled && !piece.isArmorEnabled(armor)) return false;
        }
        return true;
    }

    /** The worn chestplate if it is an {@link ArmorFSB}, else null. */
    public static ArmorFSB chestplate(Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorFSB fsb
                ? fsb : null;
    }

    /** Whether this piece's bonuses are available. Always true here; powered suits gate on charge. */
    public boolean isArmorEnabled(ItemStack stack) {
        return true;
    }

    /**
     * A suit's own HUD, drawn after the standard charge bars. Upstream's {@code handleOverlay}.
     *
     * <p>Empty here and overridden by suits that replace the vanilla readouts wholesale, such as HEV's
     * Half-Life-style HP/armour/rad panel. Upstream calls its version <b>twice</b> per frame (once
     * either side of the screen shake), which anything implementing it has to tolerate.
     *
     * <p>Called on the client only.
     *
     * @param graphics the HUD draw target, typed loosely so this common class stays off client APIs
     */
    public void handleOverlay(Object graphics, Player player) {
    }

    // Per-tick behaviour

    /**
     * The set's per-tick work, driven from {@code EntityEffectHandler} for the chest slot.
     *
     * <p>Upstream re-applies the chestplate's effects <em>every tick</em> with a 20-tick duration, which
     * is what makes them vanish a second after the suit comes off. Particles are suppressed, per the
     * comment in the old file: an effect refreshed 20 times a second is otherwise a wall of them.
     */
    public static void handleTick(Player player) {
        updateInsertSpeed(player);
        updateStepSize(player);

        // 1.7: the_NCR and Pheo always walk with the powered step while wearing any FSB chestplate (1.7 only called
        // handleTick for one), in place of the suit's own. 1.7 compared a UUID with a String here, so it never fired;
        // fixed as 1.12.2 did.
        boolean step = true;
        if (ShadyUtil.is(player.getUUID(), ShadyUtil.the_NCR)
                || ShadyUtil.is(player.getUUID(), ShadyUtil.Barnaby99_x)) {
            step = false;
            if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorFSB
                    && player.level().isClientSide() && player.onGround()) {
                steppy(player, ModSounds.STEP_POWERED.get());
            }
        }

        if (!hasFSBArmor(player)) return;

        ArmorFSB chestplate = chestplate(player);
        if (chestplate == null) return;

        for (MobEffectInstance effect : chestplate.effects) {
            Object kind = effect.getEffect();
            // visible = false hides the particles (upstream's intent); showIcon = true keeps the status
            // icon, which 1.12's five-argument PotionEffect also did. The modern five-argument constructor
            // lets showIcon follow visible, which would make a working set bonus indistinguishable from a
            // broken one.
            player.addEffect(Effects.instance(kind, effect.getDuration(), effect.getAmplifier(),
                    effect.isAmbient(), false, true));
        }

        if (step && chestplate.step != null && player.level().isClientSide() && player.onGround()
                && !player.isShiftKeyDown()) {
            steppy(player, chestplate.step.get());
        }
    }

    /**
     * 1.7 {@code ModEventHandlerClient} (client tick START): a working full set with a step size sets the
     * player's step height to {@code stepSize + 0.003}; without one, a height it recognises as its own is put
     * back. The 0.003 "discriminator" was how upstream told its own value from anyone else's; the step height
     * facade tracks ownership by id instead, so only the value is kept. Run on both sides here because the
     * modern step height is a synced attribute rather than a client-only field.
     */
    private static void updateStepSize(Player player) {
        ArmorFSB plate = chestplate(player);
        int size = plate != null && hasFSBArmor(player) ? plate.stepSize : 0;
        if (size > 0) StepHeight.set(player, "hbm:armor_step_size", size + 0.003D);
        else StepHeight.clear(player, "hbm:armor_step_size");
    }

    private static void updateInsertSpeed(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack insert = ArmorModHandler.pryMod(chest, ArmorModHandler.kevlar);
        if (insert.getItem() instanceof ItemModInsert mod && mod.speed() != 1F) {
            Attributes.replaceTransient(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),
                    "hbm:armor_insert_speed", mod.speed() - 1F);
        } else {
            Attributes.removeTransient(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),
                    "hbm:armor_insert_speed");
        }
    }

    /**
     * The metallic footstep a heavy suit makes, fired on the same cadence as the vanilla step it plays
     * alongside.
     *
     * <p>Upstream keeps its own copy of the step threshold in entity NBT ({@code hfr_nextStepDistance}),
     * seeded from vanilla's {@code nextStepDistance} field, so the suit's step tracks vanilla's without
     * consuming it. That field is <b>private</b> from 1.20 on, so the port keeps the equivalent counter
     * itself and reproduces vanilla's rule directly: step when {@code moveDist} passes the threshold,
     * then set the threshold to {@code (int) moveDist + 1}, which matches {@code Entity.nextStep()}.
     * Same cadence, no access widener.
     *
     * <p>Client-side only, so the map never needs to persist and is keyed by player rather than stored
     * on the stack.
     */
    private static final Map<UUID, Float> NEXT_STEP = new HashMap<>();

    private static void steppy(Player player, SoundEvent sound) {
        float next = NEXT_STEP.getOrDefault(player.getUUID(), 1.0F);

        if (player.moveDist <= next) return;

        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2D, player.getZ());
        if (!player.level().getBlockState(below).isAir()) {
            player.playSound(sound, 1.0F, 1.0F);
        }

        NEXT_STEP.put(player.getUUID(), (float) ((int) player.moveDist + 1));
    }

    /** The jump sound, from the vanilla jump path. */
    public static void handleJump(Player entity) {
        if (!hasFSBArmor(entity)) return;
        ArmorFSB chestplate = chestplate(entity);
        if (chestplate != null && chestplate.jump != null) {
            entity.playSound(chestplate.jump.get(), 1.0F, 1.0F);
        }
    }

    /** Damage hook implemented by suits whose legacy class intercepted LivingAttackEvent. */
    public void handleAttack(DamageEvents.DamageEvent event) {
    }

    /** Damage hook implemented by suits whose legacy class mutated LivingHurtEvent. */
    public void handleHurt(DamageEvents.DamageEvent event) {
    }

    /**
     * The hard-landing shockwave: a heavy suit landing from height throws everything nearby away.
     *
     * <p>Constants are upstream's, unchanged. Trigger above {@code fallDistance > 10}, a box inflated by
     * 3 on X/Z only, and within a <em>horizontal</em> radius of 3 an impulse and damage that both scale
     * with {@code 3 - distance}. The sign matters: {@code vec} points from the target to the player, so
     * multiplying by {@code -2} pushes outward.
     */
    public static void handleFall(Player player, float fallDistance) {
        if (!hasFSBArmor(player)) return;

        ArmorFSB chestplate = chestplate(player);
        if (chestplate == null) return;

        if (chestplate.hardLanding && fallDistance > 10 && player.level() instanceof ServerLevel level) {
            DamageSource source = ModDamageSources.hardLanding(level, player);

            for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(3, 0, 3))) {
                if (e instanceof ItemEntity) continue;

                Vec3 vec = new Vec3(player.getX() - e.getX(), 0, player.getZ() - e.getZ());
                if (vec.length() >= 3) continue;

                double intensity = 3 - vec.length();
                e.setDeltaMovement(e.getDeltaMovement().add(
                        vec.x * intensity * -2,
                        0.1D * intensity,
                        vec.z * intensity * -2));
                e.hurtMarked = true;

                Compat.hurt(e, level, source, (float) (intensity * 10));
            }
        }

        if (chestplate.fall != null && fallDistance > 0.5) {
            player.playSound(chestplate.fall.get(), 0.5F, 1.0F);
        }
    }

    /**
     * The geiger click a suit with a built-in counter makes, every 5 ticks from the chest slot.
     *
     * <p>Suppressed while the player is carrying a hand-held counter, so the two do not stack. The
     * overlapping-threshold table is upstream's unchanged: several bands can match one dose and one of
     * them is picked at random, which is what makes the clicking irregular rather than metronomic.
     */
    public void armorTick(Level level, Player entity, ItemStack stack) {
        if (this.slot != ArmorSlot.CHESTPLATE) return;
        if (!hasFSBArmor(entity) || !this.geigerSound) return;
        if (carriesCounter(entity)) return;
        if (!TickPhase.every(entity, 5)) return;

        double x = ContaminationUtil.getActualPlayerRads(entity);
        if (x <= 1e-5) return;

        List<Integer> list = new ArrayList<>();
        if (x < 1) list.add(0);
        if (x < 5) list.add(0);
        if (x < 10) list.add(1);
        if (x > 5 && x < 15) list.add(2);
        if (x > 10 && x < 20) list.add(3);
        if (x > 15 && x < 25) list.add(4);
        if (x > 20 && x < 30) list.add(5);
        if (x > 25) list.add(6);

        int r = list.get(level.getRandom().nextInt(list.size()));
        if (r > 0) {
            level.playSound(null, entity.blockPosition(), ModSounds.GEIGER_SOUNDS[r - 1].get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** Whether the player is carrying a hand-held counter, which suppresses the suit's own clicking. */
    private static boolean carriesCounter(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            Item item = inventory.getItem(i).getItem();
            if (item == ModGearItems.geiger_counter.get() || item == ModGearItems.dosimeter.get()) return true;
        }
        return false;
    }

    // Model hiding

    @Override
    public boolean disablesPart(Worn worn, ItemStack stack, EnumPlayerPart part) {
        return hidden.contains(part) && (!needsFullSet || hasFSBArmorIgnoreCharge(worn));
    }

    // Builder

    public ArmorFSB setHides(EnumPlayerPart... parts) {
        Collections.addAll(hidden, parts);
        return this;
    }

    public ArmorFSB setFullSetForHide() {
        needsFullSet = true;
        return this;
    }

    public ArmorFSB enableThermalSight(boolean thermal) {
        this.thermal = thermal;
        return this;
    }

    public ArmorFSB setHasGeigerSound(boolean geiger) {
        this.geigerSound = geiger;
        return this;
    }

    public ArmorFSB setHasCustomGeiger(boolean geiger) {
        this.customGeiger = geiger;
        return this;
    }

    public ArmorFSB setHasHardLanding(boolean hardLanding) {
        this.hardLanding = hardLanding;
        return this;
    }

    public ArmorFSB setDashCount(int dashCount) {
        this.dashCount = dashCount;
        return this;
    }

    public ArmorFSB setStepSize(int stepSize) {
        this.stepSize = stepSize;
        return this;
    }

    public ArmorFSB setStep(Supplier<SoundEvent> step) {
        this.step = step;
        return this;
    }

    public ArmorFSB setJump(Supplier<SoundEvent> jump) {
        this.jump = jump;
        return this;
    }

    public ArmorFSB setFall(Supplier<SoundEvent> fall) {
        this.fall = fall;
        return this;
    }

    public ArmorFSB addEffect(MobEffectInstance effect) {
        effects.add(effect);
        return this;
    }

    public ArmorFSB setNoHelmet(boolean noHelmet) {
        this.noHelmet = noHelmet;
        return this;
    }

    public ArmorFSB enableVATS(boolean vats) {
        this.vats = vats;
        return this;
    }

    public ArmorFSB setOverlay(Identifier path) {
        this.overlay = path;
        return this;
    }

    /** The fullscreen helmet overlay texture, or null. Drawn by the client HUD. */
    public Identifier overlay() {
        return overlay;
    }

    /**
     * Names the OBJ part tree that draws this suit on a wearer, e.g. {@code "t51"}.
     *
     * <p>1.12.2 needed a subclass per suit purely to override {@code getArmorModel}; here the worn model
     * is looked up by name in {@code ArmorRenderers}, so a suit that adds no behaviour needs no class of
     * its own. Suits that <i>do</i> add behaviour (HEV's HUD, DNT's damage handling) still get one.
     */
    public ArmorFSB setArmorModel(String armorModel) {
        this.armorModel = armorModel;
        return this;
    }

    /** The name of this suit's worn model, or null if it uses the plain equipment-texture layer. */
    public String armorModel() {
        return armorModel;
    }

    /** Registers this piece as protecting against {@code classes}. Mirrors 1.12's {@code setHazardClass}. */
    public ArmorFSB setHazardClass(HazardClass... classes) {
        ArmorRegistry.registerHazard(this, classes);
        return this;
    }

    /**
     * Registers this piece's share of a full set's radiation resistance.
     *
     * <p>{@code fullSet} is the whole suit's coefficient; each piece takes the slot weight from
     * {@code HazmatRegistry} (helmet .2 / chest .4 / legs .3 / boots .1), so the four pieces sum back to
     * {@code fullSet} and a partial set is proportionally weaker. Reduction is {@code 10^-resistance}.
     */
    public ArmorFSB setRadResist(double fullSet) {
        this.radResist = fullSet;
        if (fullSet > 0) {
            double mult = switch (slot) {
                case HELMET -> HazmatRegistry.helmet;
                case CHESTPLATE -> HazmatRegistry.chest;
                case LEGGINGS -> HazmatRegistry.legs;
                case BOOTS -> HazmatRegistry.boots;
            };
            HazmatRegistry.registerHazmat(this, fullSet * mult);
        }
        return this;
    }

    /**
     * Copies the helmet's configuration onto the other three pieces.
     *
     * <p>Upstream declares the whole set's behaviour on the helmet and clones it; the port keeps that so
     * the numbers live in one place. The effects list is shared, not copied, because it is never mutated after
     * construction (upstream's own reasoning).
     */
    public ArmorFSB cloneStats(ArmorFSB original) {
        this.effects = original.effects;
        this.noHelmet = original.noHelmet;
        this.vats = original.vats;
        this.thermal = original.thermal;
        this.geigerSound = original.geigerSound;
        this.customGeiger = original.customGeiger;
        this.hardLanding = original.hardLanding;
        this.dashCount = original.dashCount;
        this.stepSize = original.stepSize;
        this.step = original.step;
        this.jump = original.jump;
        this.fall = original.fall;
        this.armorModel = original.armorModel;
        this.setRadResist(original.radResist);
        // The overlay is not copied: it is a helmet-only, fullscreen effect.
        return this;
    }

    // Tooltip

    // 1.7 adds the resistances in its tooltip event, after the item's lines; here before the attribute lines 1.7 lacks.
    @Override
    protected final void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        addInformation(stack, tooltip);
        DamageResistanceHandler.addInfo(stack, tooltip);
    }

    public void addInformation(ItemStack stack, List<Component> tooltip) {
        List<Component> toAdd = new ArrayList<>();

        if (!effects.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (MobEffectInstance effect : effects) {
                names.add(Component.translatable(effect.getDescriptionId()).getString());
            }
            toAdd.add(Component.literal(String.join(", ", names)).withStyle(ChatFormatting.AQUA));
        }

        if (geigerSound) toAdd.add(bonus("armor.geigerSound", ChatFormatting.GOLD));
        if (customGeiger) toAdd.add(bonus("armor.geigerHUD", ChatFormatting.GOLD));
        if (vats) toAdd.add(bonus("armor.vats", ChatFormatting.RED));
        if (thermal) toAdd.add(bonus("armor.thermal", ChatFormatting.RED));
        if (hardLanding) toAdd.add(bonus("armor.hardLanding", ChatFormatting.RED));
        if (stepSize != 0) toAdd.add(bonus("armor.stepSize", ChatFormatting.BLUE, stepSize));
        if (dashCount > 0) toAdd.add(bonus("armor.dash", ChatFormatting.AQUA, dashCount));

        if (!toAdd.isEmpty()) {
            tooltip.add(Component.translatable("armor.fullSetBonus").withStyle(ChatFormatting.GOLD));
            tooltip.addAll(toAdd);
        }
    }

    private static Component bonus(String key, ChatFormatting color, Object... args) {
        // The style is applied to the whole component, never as an embedded section code: appended
        // siblings inherit the parent's style, so a section code inside the text loses its colour after
        // the first sibling (see the radiation-port gotchas in IMPLEMENTATION.md).
        return Component.literal("  ").append(Component.translatable(key, args)).withStyle(color);
    }
}
