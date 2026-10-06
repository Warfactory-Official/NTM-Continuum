package com.hbm.client;

import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.packet.KeybindPayload;
import com.hbm.packet.ToolAnimationPayload;
import com.mojang.blaze3d.platform.InputConstants;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** G-C (rendering and input gaps): the client halves - packet handlers, key mappings, overlays and world renderers. */
public final class RenderInputClientRegistrations {

    private static final Identifier MISC = dev.golem.core.Ids.of("hbm", "textures/misc/overlay_misc.png");

    /** 1.7 {@code HbmKeybinds.magnetKey}: "hbm.key.toggleMagnet" on Z. */
    public static KeyMappings.Mapping magnetKey;
    private static boolean lastMagnet;

    private RenderInputClientRegistrations() {}

    /** Called from {@code HbmPackets.registerClientHandlers}. */
    public static void registerPacketHandlers() {
        GolemNetwork.registerPlayToClient(ToolAnimationPayload.CODEC,
                (payload, context) -> com.hbm.client.anim.ToolAnimations.handle(payload));
        com.hbm.client.crucible.CrucibleClient.registerPacketHandlers(); // old-hbm crucible anims and cut mobs
    }

    /** Called from {@code HbmClientRegistrations.installKeybinds}. */
    public static void installKeybinds() {
        magnetKey = KeyMappings.register("hbm", "key.hbm.toggleMagnet", InputConstants.KEY_Z, "hbm");
        ClientTicks.onEndTick(RenderInputClientRegistrations::onEndTick);
    }

    /** The magnet key's state, sent on every change as 1.7's {@code KeybindPacket} was; the server toggles on press. */
    private static void onEndTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            lastMagnet = false;
            return;
        }
        boolean magnet = mc.screen == null && magnetKey.isHeld();
        if (magnet != lastMagnet) {
            GolemNetwork.sendToServer(KeybindPayload.CODEC, new KeybindPayload(EnumKeybind.TOGGLE_MAGNET.ordinal(), magnet));
            lastMagnet = magnet;
        }
    }

    /** Called from {@code HbmClientRegistrations.registerHud}. */
    public static void registerHud(HbmClientRegistrations.HudSink sink) {
        sink.register("shield_bar", RenderInputClientRegistrations::renderShieldBar);
    }

    /**
     * 1.7 {@code RenderScreenOverlay.renderShieldBar}, drawn while the effective maximum shield (base plus a Life
     * Extender insert) is above zero: the empty bar ({@code overlay_misc} 146,0 81x9), its fill (147,9, ceil(shield *
     * 79 / max) wide) and the shield to one decimal in 0xFFFF80 over a four-way black outline.
     *
     * <p>1.7 drew it in the ARMOR overlay's slot (Forge's {@code left_height}) and raised the left stack by ten, so the
     * armour row moved up above it. No loader here lets an overlay take part in that stacking on every node, so the bar
     * goes to the row above vanilla's armour row when armour shows (at the armour row otherwise): same bar, rows
     * swapped.
     */
    public static void renderShieldBar(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        float max = ArmorModEvents.effectiveMaxShield(player);
        if (max <= 0) return;
        float shield = com.hbm.extprop.HbmPlayerProps.getData(player).shield;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int left = width / 2 - 91;

        // vanilla's armour row: the hearts' rows, spaced as Gui.renderPlayerHealth spaces them
        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), player.getHealth());
        int absorb = Mth.ceil(player.getAbsorptionAmount());
        int rows = Mth.ceil((maxHealth + absorb) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (rows - 2), 3);
        int armorRow = height - 39 - (rows - 1) * rowHeight - 10;
        boolean armorShows = player.getArmorValue() > 0 && !dev.golem.core.client.VanillaHud.hideArmor();
        int top = armorShows ? armorRow - 10 : armorRow;

        GuiDraw.blit(graphics, MISC, left, top, 146, 0, 81, 9, 256, 256);
        int i = (int) Math.ceil(shield * 79 / max);
        if (i > 0) GuiDraw.blit(graphics, MISC, left + 1, top, 147, 9, i, 9, 256, 256);

        Font font = mc.font;
        String label = "" + ((int) (shield * 10F)) / 10D;
        int half = font.width(label) / 2;
        GuiDraw.text(graphics, font, label, left + 41 - half, top + 1, 0xFF000000, false);
        GuiDraw.text(graphics, font, label, left + 39 - half, top + 1, 0xFF000000, false);
        GuiDraw.text(graphics, font, label, left + 40 - half, top, 0xFF000000, false);
        GuiDraw.text(graphics, font, label, left + 40 - half, top + 2, 0xFF000000, false);
        GuiDraw.text(graphics, font, label, left + 40 - half, top + 1, 0xFFFFFF80, false);
    }

    /** Called from {@code HbmClientRegistrations.installEntityEffects}. */
    public static void install() {
        // 1.7 Forge IItemRenderer items on pedestals, skeleton holders and belts (FramedItem's CUSTOM arm)
        dev.golem.core.client.ClientResourceReloads.register(com.hbm.Constants.MOD_ID, "framed_item_routing",
                com.hbm.client.render.entity.EntityItemRenderers::reload);
        // 1.7 ClientProxy "tau": count line sparks (random gaussian * 0.05 sideways, 0.05 up) and one hadron flash
        com.hbm.client.particle.SparkParticles.register();
        com.hbm.particle.helper.HbmEffectNT.Tau.setHandler((level, x, y, z, data) -> {
            if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel client)) return;
            boolean small = dev.golem.core.data.Tags.getBooleanOr(data, "small", false);
            int count = dev.golem.core.data.Tags.getByteOr(data, "count", (byte) 0);
            net.minecraft.util.RandomSource rand = client.getRandom();
            for (int i = 0; i < count; i++) {
                com.hbm.client.particle.SparkParticles.spawn(client, x, y, z, rand.nextGaussian() * 0.05, 0.05,
                        rand.nextGaussian() * 0.05, small);
            }
            dev.golem.core.render.Particles.add(new com.hbm.client.particle.ParticleHadron(client, x, y, z).makeSmall(small));
        });
        com.hbm.client.GapsCHarnessQueries.register(); // harness query hbm_gapsc (FinalGapsC)
        com.hbm.client.ConnectedTextureHarnessQueries.register(); // harness query hbm_ct (ConnectedTextures17)
        com.hbm.client.IconAuditHarnessQueries.register(); // harness query hbm_icon_audit (ItemIconAudit)
        // 1.7 IBlockCT connected textures: hadron coils, coated wire, ICF/PWR shells, deco metals, CT glass, platemetal, pile
        com.hbm.client.ConnectedTextureRegistrations.register();
        // 1.7 RenderOverhead markers ("marker" aux effect) and ParticleText ("debug")
        com.hbm.client.render.RenderOverhead.register();
        com.hbm.client.particle.TextParticles.register();
        com.hbm.particle.helper.HbmEffectNT.Marker.setHandler((level, x, y, z, data) -> {
            int expires = dev.golem.core.data.Tags.getIntOr(data, "expires", 0);
            String label = dev.golem.core.data.Tags.getStringOr(data, "label", "");
            com.hbm.client.render.RenderOverhead.queue(net.minecraft.core.BlockPos.containing(x, y, z),
                    new com.hbm.client.render.RenderOverhead.Marker(dev.golem.core.data.Tags.getIntOr(data, "color", 0))
                            .setDist(dev.golem.core.data.Tags.getDoubleOr(data, "dist", 0D))
                            .setExpire(expires > 0 ? System.currentTimeMillis() + expires : 0)
                            .withLabel(label.isEmpty() ? null : label));
        });
        com.hbm.particle.helper.HbmEffectNT.LaunchSmoke.setHandler((level, x, y, z, data) -> {
            if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel client)) return;
            dev.golem.core.render.Particles.add(new com.hbm.client.particle.ParticleSmokePlume(client, x, y, z,
                    dev.golem.core.data.Tags.getDoubleOr(data, "moX", 0D), dev.golem.core.data.Tags.getDoubleOr(data, "moY", 0D),
                    dev.golem.core.data.Tags.getDoubleOr(data, "moZ", 0D)));
        });
        com.hbm.particle.helper.HbmEffectNT.DebugText.setHandler((level, x, y, z, data) -> {
            if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel client)) return;
            com.hbm.client.particle.TextParticles.spawn(client, x, y, z, dev.golem.core.data.Tags.getIntOr(data, "color", 0),
                    dev.golem.core.data.Tags.getStringOr(data, "text", ""), dev.golem.core.data.Tags.getFloatOr(data, "scale", 1F));
        });
    }
}
