package com.hbm.inventory.gui;

import com.hbm.api.entity.RadarEntry;
import com.hbm.lib.Library;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import dev.golem.core.gui.CharInput;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code GUIMachineRadarNT}, the radar map: a 216x234 screen with no menu. The 200x200 scope shows the
 * sweep, the blips ({@code gui_radar_nt.png} u=216, v=8*blip level), and, when enabled, the height map; the
 * eight toggles hang off the left edge; the power bar runs along the bottom. Typing a digit 1-8 over the scope
 * sends a launch command through the link in that slot, at the blip under the cursor or at the map position.
 *
 * <p>The sweep was one smooth-shaded quad whose only visible half is the triangle centre / leading edge /
 * trailing edge, its alpha the leading edge's barycentric weight - linear across the triangle. That is drawn
 * here as one-pixel strips of vertical gradients in the triangle's own frame, which is the same function
 * sampled per pixel column. Jammed toggles flicker by the client's random, as they did.
 */
public class GUIMachineRadarNT extends CompatScreen {

    public static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_radar_nt.png");

    protected final TileEntityMachineRadarNT radar;
    protected int xSize = 216;
    protected int ySize = 234;
    protected int guiLeft;
    protected int guiTop;

    public int lastMouseX;
    public int lastMouseY;

    private final RandomSource rand = RandomSource.create();

    public GUIMachineRadarNT(TileEntityMachineRadarNT tile) {
        super(Component.translatable("container.radar"));
        this.radar = tile;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive() || radar.isRemoved()) this.onClose();
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        int x = (int) event.x();
        int y = (int) event.y();
        String cmd = null;

        if (checkClick(x, y, -10, 88, 8, 8)) cmd = "missiles";
        if (checkClick(x, y, -10, 98, 8, 8)) cmd = "shells";
        if (checkClick(x, y, -10, 108, 8, 8)) cmd = "players";
        if (checkClick(x, y, -10, 118, 8, 8)) cmd = "smart";
        if (checkClick(x, y, -10, 128, 8, 8)) cmd = "red";
        if (checkClick(x, y, -10, 138, 8, 8)) cmd = "map";
        if (checkClick(x, y, -10, 158, 8, 8)) cmd = "gui1";
        if (checkClick(x, y, -10, 178, 8, 8)) cmd = "clear";

        if (cmd != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean(cmd, true);
            send(data);
            return true;
        }
        return false;
    }

    private void send(CompoundTag data) {
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(radar.getBlockPos(), data));
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float f) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, guiLeft - 14, guiTop + 84, 224, 0, 14, 66, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, guiLeft - 14, guiTop + 154, 224, 66, 14, 36, 256, 256);

        if (radar.power > 0) {
            int i = (int) (radar.power * 200 / TileEntityMachineRadarNT.maxPower);
            GuiDraw.blit(graphics, TEXTURE, guiLeft + 8, guiTop + 221, 0, 234, i, 16, 256, 256);
        }

        boolean[] toggles = {radar.scanMissiles, radar.scanShells, radar.scanPlayers, radar.smartMode, radar.redMode, radar.showMap};
        for (int i = 0; i < toggles.length; i++) {
            if (toggles[i] ^ (radar.jammed && rand.nextBoolean())) {
                GuiDraw.blit(graphics, TEXTURE, guiLeft - 10, guiTop + 88 + i * 10, 238, 4 + i * 10, 8, 8, 256, 256);
            }
        }

        if (radar.power < TileEntityMachineRadarNT.consumption) return;

        if (radar.jammed) {
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    GuiDraw.blit(graphics, TEXTURE, guiLeft + 8 + i * 40, guiTop + 17 + j * 40, 216, 118 + rand.nextInt(81), 40, 40, 256, 256);
                }
            }
            return;
        }

        if (radar.showMap) drawMap(graphics);

        float rot = (float) -Math.toRadians(radar.prevRotation + (radar.rotation - radar.prevRotation) * f + 180F);
        drawSweep(graphics, rot);

        if (!radar.entries.isEmpty()) {
            for (RadarEntry m : radar.entries) {
                double x = (m.posX - radar.origin.getX()) / ((double) radar.getRange() * 2 + 1) * (200D - 8D) - 4D;
                double z = (m.posZ - radar.origin.getZ()) / ((double) radar.getRange() * 2 + 1) * (200D - 8D) - 4D;
                int t = m.blipLevel;
                GuiDraw.pushMatrix(graphics);
                GuiDraw.translate(graphics, (float) (guiLeft + 108 + x), (float) (guiTop + 117 + z));
                GuiDraw.blit(graphics, TEXTURE, 0, 0, 216, 8 * t, 8, 8, 256, 256);
                GuiDraw.popMatrix(graphics);
            }
        }
    }

    /** The height map: one opaque pixel per byte, green by height, 1.7's single quad per cell run together. */
    private void drawMap(GuiGraphicsExtractor graphics) {
        byte[] map = radar.map;
        for (int iZ = 0; iZ < 200; iZ++) {
            int iX = 0;
            while (iX < 200) {
                byte b = map[iZ * 200 + iX];
                if (b <= 0) { iX++; continue; }
                int start = iX;
                while (iX < 200 && map[iZ * 200 + iX] == b) iX++;
                int color = 0xFF000000 | (((b - 50) * 255 / 78) << 8);
                GuiDraw.fill(graphics, guiLeft + 8 + start, guiTop + 17 + iZ, guiLeft + 8 + iX, guiTop + 18 + iZ, color);
            }
        }
    }

    /**
     * 1.7's {@code Vec3.rotateAroundZ} maps {@code (x, y)} to {@code (x cos + y sin, y cos - x sin)}; the leading
     * edge is {@code (100, 0)} turned by {@code rot}, the trailing edge the same turned by {@code rot + 0.25}.
     */
    private void drawSweep(GuiGraphicsExtractor graphics, float rot) {
        float trX = (float) (100 * Math.cos(rot));
        float trY = (float) (-100 * Math.sin(rot));
        float tlX = (float) (100 * Math.cos(rot + 0.25F));
        float tlY = (float) (-100 * Math.sin(rot + 0.25F));

        // frame: u along centre -> trailing edge, v toward the leading edge
        float angle = (float) Math.atan2(tlY, tlX);
        float nX = (float) -Math.sin(angle), nY = (float) Math.cos(angle);
        float side = Math.signum(trX * nX + trY * nY);
        if (side == 0F) side = 1F;

        double uLead = 100 * Math.cos(0.25);
        double h = 100 * Math.sin(0.25);

        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, guiLeft + 108, guiTop + 117);
        GuiDraw.rotate(graphics, angle);
        GuiDraw.scale(graphics, 1F, side);
        for (int u = 0; u < 100; u++) {
            double mid = u + 0.5;
            double edge = mid <= uLead ? mid * h / uLead : h * (100 - mid) / (100 - uLead);
            int v = (int) Math.round(edge);
            if (v <= 0) continue;
            int alpha = (int) Math.round(Math.min(1D, v / h) * 255D);
            GuiDraw.fillGradient(graphics, u, 0, u + 1, v, 0x0000FF00, (alpha << 24) | 0x00FF00);
        }
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {

        if (checkClick(mouseX, mouseY, 8, 221, 200, 7)) {
            tip(graphics, List.of(Component.literal(Library.getShortNumber(radar.power) + "/"
                    + Library.getShortNumber(TileEntityMachineRadarNT.maxPower) + "HE")), mouseX, mouseY);
        }

        String[] keys = {"radar.detectMissiles", "radar.detectShells", "radar.detectPlayers", "radar.smartMode",
                "radar.redMode", "radar.showMap"};
        for (int i = 0; i < keys.length; i++) {
            if (checkClick(mouseX, mouseY, -10, 88 + i * 10, 8, 8)) tip(graphics, lines(keys[i]), mouseX, mouseY);
        }
        if (checkClick(mouseX, mouseY, -10, 158, 8, 8)) tip(graphics, lines("radar.toggleGui"), mouseX, mouseY);
        if (checkClick(mouseX, mouseY, -10, 178, 8, 8)) tip(graphics, lines("radar.clearMap"), mouseX, mouseY);

        if (!radar.entries.isEmpty()) {
            for (RadarEntry m : radar.entries) {
                int x = guiLeft + (int) ((m.posX - radar.origin.getX()) / ((double) radar.getRange() * 2 + 1) * (200D - 8D)) + 108;
                int z = guiTop + (int) ((m.posZ - radar.origin.getZ()) / ((double) radar.getRange() * 2 + 1) * (200D - 8D)) + 117;

                if (mouseX + 5 > x && mouseX - 4 <= x && mouseY + 5 > z && mouseY - 4 <= z) {
                    tip(graphics, List.of(Component.literal(I18n.get(m.unlocalizedName)),
                            Component.literal(m.posX + " / " + m.posZ), Component.literal("Alt.: " + m.posY)), x, z);
                    return;
                }
            }
        }

        if (checkClick(mouseX, mouseY, 8, 17, 200, 200)) {
            int tX = (int) ((lastMouseX - guiLeft - 108) * ((double) radar.getRange() * 2 + 1) / 192D + radar.origin.getX());
            int tZ = (int) ((lastMouseY - guiTop - 117) * ((double) radar.getRange() * 2 + 1) / 192D + radar.origin.getZ());
            tip(graphics, List.of(Component.literal(tX + " / " + tZ)), lastMouseX, lastMouseY);
        }
    }

    private void tip(GuiGraphicsExtractor graphics, List<Component> lines, int x, int y) {
        GuiDraw.tooltip(graphics, this.font, lines, Optional.empty(), x, y);
    }

    /** 1.7 {@code I18nUtil.resolveKeyArray}: one translation, split on {@code $}. */
    private static List<Component> lines(String key) {
        List<Component> lines = new ArrayList<>();
        for (String line : I18n.get(key).split("\\$")) lines.add(Component.literal(line));
        return lines;
    }

    protected boolean checkClick(int x, int y, int left, int top, int sizeX, int sizeY) {
        return guiLeft + left <= x && guiLeft + left + sizeX > x && guiTop + top < y && guiTop + top + sizeY >= y;
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.key() == 256 || event.matches(mc.options.keyInventory)) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    protected boolean charTypedCompat(CharInput event) {
        char c = event.codePoint();
        if (!checkClick(lastMouseX, lastMouseY, 8, 17, 200, 200) || c < '1' || c > '8') return false;

        int id = c - '1';

        if (!radar.entries.isEmpty()) {
            for (RadarEntry m : radar.entries) {
                int x = guiLeft + (int) ((m.posX - radar.origin.getX()) / ((double) radar.getRange() * 2 + 1) * (200D - 8D)) + 108;
                int z = guiTop + (int) ((m.posZ - radar.origin.getZ()) / ((double) radar.getRange() * 2 + 1) * (200D - 8D)) + 117;

                if (lastMouseX + 5 > x && lastMouseX - 4 <= x && lastMouseY + 5 > z && lastMouseY - 4 <= z) {
                    CompoundTag data = new CompoundTag();
                    data.putInt("launchEntity", m.entityID);
                    data.putInt("link", id);
                    send(data);
                    return true;
                }
            }
        }

        int tX = (int) ((lastMouseX - guiLeft - 108) * ((double) radar.getRange() * 2 + 1) / 192D + radar.origin.getX());
        int tZ = (int) ((lastMouseY - guiTop - 117) * ((double) radar.getRange() * 2 + 1) / 192D + radar.origin.getZ());
        CompoundTag data = new CompoundTag();
        data.putInt("launchPosX", tX);
        data.putInt("launchPosZ", tZ);
        data.putInt("link", id);
        send(data);
        return true;
    }
}
