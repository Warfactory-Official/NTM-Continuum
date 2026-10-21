package com.hbm.client;

import com.hbm.Constants;
import com.hbm.client.anim.ToolAnimations;
import com.hbm.client.crucible.CrucibleClient;
import com.hbm.client.particle.ParticleHadron;
import com.hbm.client.particle.ParticleSmokePlume;
import com.hbm.client.particle.SparkParticles;
import com.hbm.client.particle.TextParticles;
import com.hbm.client.render.RenderOverhead;
import com.hbm.client.render.entity.EntityItemRenderers;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.packet.KeybindPayload;
import com.hbm.packet.ToolAnimationPayload;
import com.hbm.particle.helper.HbmEffectNT;
import com.mojang.blaze3d.platform.InputConstants;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.client.VanillaHud;
import dev.golem.core.data.Tags;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.render.Particles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** G-C (rendering and input gaps): the client halves - packet handlers, key mappings, overlays and world renderers. */
public final class RenderInputClientRegistrations {

    private static final Identifier MISC = Ids.of("hbm", "textures/misc/overlay_misc.png");

    /** 1.7 {@code HbmKeybinds.magnetKey}: "hbm.key.toggleMagnet" on Z. */
    public static KeyMappings.Mapping magnetKey;
    private static boolean lastMagnet;

    private RenderInputClientRegistrations() {}

    /** Called from {@code HbmPackets.registerClientHandlers}. */
    public static void registerPacketHandlers() {
        GolemNetwork.registerPlayToClient(ToolAnimationPayload.CODEC,
                (payload, context) -> ToolAnimations.handle(payload));
        CrucibleClient.registerPacketHandlers(); // 1.12.2 crucible anims
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
     * armour row moved up above it. No loader here lets an overlay join that stacking on every node, so the bar goes
     * to the row above vanilla's armour row when armour shows (at the armour row otherwise): same bar, rows swapped.
     */
    public static void renderShieldBar(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        float max = ArmorModEvents.effectiveMaxShield(player);
        if (max <= 0) return;
        float shield = HbmPlayerProps.getData(player).shield;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int left = width / 2 - 91;

        // vanilla's armour row: the hearts' rows, spaced as Gui.renderPlayerHealth spaces them
        float maxHealth = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH), player.getHealth());
        int absorb = Mth.ceil(player.getAbsorptionAmount());
        int rows = Mth.ceil((maxHealth + absorb) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (rows - 2), 3);
        int armorRow = height - 39 - (rows - 1) * rowHeight - 10;
        boolean armorShows = player.getArmorValue() > 0 && !VanillaHud.hideArmor();
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
        ClientResourceReloads.register(Constants.MOD_ID, "framed_item_routing",
                EntityItemRenderers::reload);
        // 1.7 ClientProxy "tau": count line sparks (random gaussian * 0.05 sideways, 0.05 up) and one hadron flash
        SparkParticles.register();
        HbmEffectNT.Tau.setHandler((level, x, y, z, data) -> {
            if (!(level instanceof ClientLevel client)) return;
            boolean small = Tags.getBooleanOr(data, "small", false);
            int count = Tags.getByteOr(data, "count", (byte) 0);
            RandomSource rand = client.getRandom();
            for (int i = 0; i < count; i++) {
                SparkParticles.spawn(client, x, y, z, rand.nextGaussian() * 0.05, 0.05,
                        rand.nextGaussian() * 0.05, small);
            }
            Particles.add(new ParticleHadron(client, x, y, z).makeSmall(small));
        });
        // 1.7 IBlockCT connected textures: hadron coils, coated wire, ICF/PWR shells, deco metals, CT glass, platemetal, pile
        ConnectedTextureRegistrations.register();
        // 1.7 RenderOverhead markers ("marker" aux effect) and ParticleText ("debug")
        RenderOverhead.register();
        TextParticles.register();
        HbmEffectNT.Marker.setHandler((level, x, y, z, data) -> {
            int expires = Tags.getIntOr(data, "expires", 0);
            String label = Tags.getStringOr(data, "label", "");
            RenderOverhead.queue(BlockPos.containing(x, y, z),
                    new RenderOverhead.Marker(Tags.getIntOr(data, "color", 0))
                            .setDist(Tags.getDoubleOr(data, "dist", 0D))
                            .setExpire(expires > 0 ? System.currentTimeMillis() + expires : 0)
                            .withLabel(label.isEmpty() ? null : label));
        });
        HbmEffectNT.LaunchSmoke.setHandler((level, x, y, z, data) -> {
            if (!(level instanceof ClientLevel client)) return;
            Particles.add(new ParticleSmokePlume(client, x, y, z,
                    Tags.getDoubleOr(data, "moX", 0D), Tags.getDoubleOr(data, "moY", 0D),
                    Tags.getDoubleOr(data, "moZ", 0D)));
        });
        HbmEffectNT.DebugText.setHandler((level, x, y, z, data) -> {
            if (!(level instanceof ClientLevel client)) return;
            TextParticles.spawn(client, x, y, z, Tags.getIntOr(data, "color", 0),
                    Tags.getStringOr(data, "text", ""), Tags.getFloatOr(data, "scale", 1F));
        });
    }
}
