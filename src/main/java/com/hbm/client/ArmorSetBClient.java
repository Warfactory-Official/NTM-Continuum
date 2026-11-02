package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.capability.HbmLivingProps;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.ModGearItems;
import com.hbm.items.armor.ArmorNo9;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ArmorFSBFueled;
import com.hbm.items.armor.ArmorFSBPowered;
import com.hbm.util.EnchantmentUtil;
import dev.golem.core.Ids;
import dev.golem.core.client.Hud;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.harness.HarnessQueryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Client half of armour set B: the helmet overlays (1.7 {@code renderHelmetOverlay}) and the
 * {@code hbm_armor_set_b} harness query.
 */
public final class ArmorSetBClient {

    private ArmorSetBClient() {}

    private static final Identifier[] BLURS = new Identifier[6];
    /** 1.7 {@code ArmorGasMask.maskBlur}: the plain gas mask's own six wear stages. */
    private static final Identifier[] MASK_BLURS = new Identifier[6];

    static {
        for (int i = 0; i < 6; i++) BLURS[i] = Ids.of("hbm", "textures/misc/overlay_goggles_" + i + ".png");
        for (int i = 0; i < 6; i++) MASK_BLURS[i] = Ids.of("hbm", "textures/misc/overlay_gasmask_" + i + ".png");
    }

    private static final Identifier HAZMAT = Ids.of("hbm", "textures/misc/overlay_hazmat.png");

    public static void registerHud(HbmClientRegistrations.HudSink sink) {
        HarnessQueryRegistry.register("hbm_armor_set_b", (mc, params) -> query(mc));
    }

    /** Forge drew helmet overlays first, so the crosshair and hotbar lie on top of them. */
    public static void installHelmetOverlay() {
        Hud.addCameraOverlay("hbm", "helmet_overlay", ArmorSetBClient::renderHelmetOverlay);
    }

    /**
     * The helmet's fullscreen overlay, first person only. The goggles, M65 mask, advanced and high-performance hazmat
     * hoods and the plain gas mask pick one of six textures by wear, {@code (int) (damage / maxDamage * 6)}, 5 when out
     * of range; the mono and olde masks draw none, as in 1.7.
     */
    public static void renderHelmetOverlay(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()) return;
        ItemStack helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        Identifier overlay = overlay(helmet);
        if (overlay != null) GuiDraw.blitUvFullscreen(graphics, overlay, 0F, 0F, 1F, 1F);
    }

    private static Identifier overlay(ItemStack helmet) {
        Item item = helmet.getItem();
        if (item instanceof ArmorFSB fsb && fsb.overlay() != null) return fsb.overlay();
        if (item == ModGearItems.hazmat_helmet.get()) return HAZMAT;
        Identifier[] blurs;
        if (item == ModArmorSetBItems.goggles.get() || item == ModGearItems.gas_mask_m65.get()
                || item == ModGearItems.hazmat_helmet_red.get() || item == ModGearItems.hazmat_helmet_grey.get()) {
            blurs = BLURS;
        } else if (item == ModGearItems.gas_mask.get()) {
            blurs = MASK_BLURS;
        } else {
            return null;
        }
        int max = helmet.getMaxDamage();
        int index = max <= 0 ? 0 : (int) ((double) helmet.getDamageValue() / (double) max * 6D);
        if (index < 0 || index > 5) index = 5;
        return blurs[index];
    }

    private static JsonObject query(Minecraft mc) {
        JsonObject json = new JsonObject();
        if (mc.player == null || mc.getSingleplayerServer() == null) return json;
        Player server = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (server == null) return json;
        json.addProperty("health", server.getHealth());
        json.addProperty("food", server.getFoodData().getFoodLevel());
        json.addProperty("air", server.getAirSupply());
        json.addProperty("xp_total", EnchantmentUtil.getTotalExperience(server));
        json.addProperty("xp_level", server.experienceLevel);
        json.addProperty("blacklung", HbmLivingProps.getBlackLung(server));
        json.addProperty("hud_enabled", HbmLivingProps.getEnableHUD(server));
        json.addProperty("fsb", ArmorFSB.hasFSBArmor(server));
        json.addProperty("speed_server",
                server.getAttributeValue(Attributes.MOVEMENT_SPEED));
        json.addProperty("speed_client",
                mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED));
        json.addProperty("knockback_server",
                server.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        json.addProperty("in_water", server.isInWater());
        json.addProperty("camera", mc.options.getCameraType().name());
        json.addProperty("steam_id", Fluids.STEAM.getRegistryName());
        json.addProperty("diesel_crack_id", Fluids.DIESEL_CRACK.getRegistryName());
        json.addProperty("night_vision", server.hasEffect(MobEffects.NIGHT_VISION));
        json.addProperty("sprinting", server.isSprinting());
        JsonArray effects = new JsonArray();
        for (MobEffectInstance effect : server.getActiveEffects()) {
            effects.add(effect.getDescriptionId() + "@" + effect.getAmplifier());
        }
        json.add("effects", effects);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            ItemStack stack = server.getItemBySlot(slot);
            JsonObject piece = new JsonObject();
            piece.addProperty("id", stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            piece.addProperty("damageable", stack.isDamageableItem());
            piece.addProperty("damage", stack.getDamageValue());
            if (stack.getItem() instanceof ArmorFSBFueled fueled) {
                piece.addProperty("fuel", fueled.getFill(stack));
                piece.addProperty("max_fuel", fueled.maxFuel);
            }
            if (stack.getItem() instanceof ArmorFSBPowered powered) {
                piece.addProperty("charge", powered.getCharge(stack));
                piece.addProperty("max_charge", powered.getMaxCharge(stack));
            }
            if (stack.getItem() instanceof ArmorNo9) piece.addProperty("lamp_on", ArmorNo9.isOn(stack));
            json.add(slot.getName(), piece);
        }
        return json;
    }
}
