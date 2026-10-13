package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerPADipole;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.albion.TileEntityPADipole;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Dipole panel. Three compass dials pick the beam's exit for the below-threshold, above-threshold and
 * redstone cases; each is drawn as a blue needle for the player's own facing and a red one for the
 * configured direction, so the setting reads the same way round as the world does. The text field
 * underneath is the momentum threshold that chooses between the first two.
 */
public class GUIPADipole extends GUIPABase<ContainerPADipole> {

    private static final Identifier TEXTURE =
            Ids.of("hbm", "textures/gui/particleaccelerator/gui_dipole.png");

    private EditBox threshold;
    private boolean suppressResponder;

    public GUIPADipole(ContainerPADipole menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected @Nullable TileEntityCooledBase machine() {
        return menu.getMachine();
    }

    @Override
    protected void init() {
        super.init();
        String previous = this.threshold == null ? null : this.threshold.getValue();
        this.threshold = new EditBox(this.font, leftPos + 47, topPos + 77, 66, 8, Component.empty());
        this.threshold.setTextColor(0xFF00FF00);
        this.threshold.setBordered(false);
        this.threshold.setMaxLength(9);
        if (previous != null) {
            this.threshold.setValue(previous);
        } else {
            TileEntityPADipole dipole = menu.getMachine();
            this.threshold.setValue(dipole == null ? "0" : String.valueOf(dipole.threshold));
        }
        this.threshold.setResponder(this::commit);
        addRenderableWidget(this.threshold);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityPADipole dipole = menu.getMachine();
        if (dipole == null) return;

        drawPowerBar(graphics, x + 8, y + 18, dipole.power, dipole.getMaxPower());

        if (dipole.power >= TileEntityPADipole.usage) drawLamp(graphics, x + 83, y + 54);
        if (Math.ceil(dipole.temperature) <= COOL_ENOUGH) drawLamp(graphics, x + 93, y + 54);
        if (ItemPACoil.typeOf(dipole.inventory.getStackInSlot(1)) != null) drawLamp(graphics, x + 103, y + 54);

        float playerYaw = Minecraft.getInstance().player == null ? 0F : Minecraft.getInstance().player.getYRot();
        dial(graphics, x + 68, y + 35, playerYaw, dipole.dirLower);
        dial(graphics, x + 68, y + 49, playerYaw, dipole.dirUpper);
        dial(graphics, x + 68, y + 63, playerYaw, dipole.dirRedstone);

        drawTank(graphics, dipole.coolantTanks[0], x + 134, y + 36);
        drawTank(graphics, dipole.coolantTanks[1], x + 152, y + 36);
    }

    /** The player-facing reference needle and the configured-direction needle, both six pixels long. */
    private void dial(GuiGraphicsExtractor graphics, int x, int y, float playerYaw, int dir) {
        needle(graphics, x, y, 180F, 0xFF8080FF);
        needle(graphics, x, y, playerYaw - dir * 90F, 0xFFFF0000);
    }

    private void needle(GuiGraphicsExtractor graphics, int x, int y, float yawDegrees, int color) {
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, x, y);
        GuiDraw.rotate(graphics, (float) Math.toRadians(yawDegrees));
        GuiDraw.scale(graphics, 1F / 8F, 1F / 8F);
        graphics.fill(-4, 0, 4, 48, color);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics, -9, 6, 4210752);
        drawTemperature(graphics, 136, 22, 166, 12);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (commonTooltip(graphics, mouseX, mouseY, 8, 18, 134, 36)) return;

        TileEntityPADipole dipole = menu.getMachine();
        if (dipole == null) return;

        if (isHovering(62, 29, 12, 12, mouseX, mouseY)) {
            tooltip(graphics, dialTooltip(dipole.dirLower), mouseX, mouseY);
        } else if (isHovering(62, 43, 12, 12, mouseX, mouseY)) {
            tooltip(graphics, dialTooltip(dipole.dirUpper), mouseX, mouseY);
        } else if (isHovering(62, 57, 12, 12, mouseX, mouseY)) {
            tooltip(graphics, dialTooltip(dipole.dirRedstone), mouseX, mouseY);
        }
    }

    private static List<Component> dialTooltip(int dir) {
        return List.of(
                Component.literal("Player orientation").withStyle(ChatFormatting.BLUE),
                Component.literal("Output orientation:").withStyle(ChatFormatting.RED),
                Component.literal(TileEntityPADipole.ditToForgeDir(dir).name()));
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityPADipole dipole = menu.getMachine();
        if (dipole == null) return false;

        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        if (x < 62 || x >= 74) return false;

        // 1.7 hit boxes: the y test is exclusive at the top and inclusive at the bottom.
        if (y > 29 && y <= 41) return cycle(dipole, "lower");
        if (y > 43 && y <= 55) return cycle(dipole, "upper");
        if (y > 57 && y <= 69) return cycle(dipole, "redstone");
        return false;
    }

    private boolean cycle(TileEntityPADipole dipole, String key) {
        GuiUtil.playClickSound();
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                new ControlEventPayload(dipole.getBlockPos(), data));
        return true;
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        if (this.threshold != null && this.threshold.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(this.threshold);
            return true;
        }
        return false;
    }

    /** Commits on every keystroke, as upstream does, after the same leading-zero normalisation. */
    private void commit(String text) {
        if (suppressResponder) return;
        TileEntityPADipole dipole = menu.getMachine();
        if (dipole == null) return;

        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') digits.append(c);
        }
        String cleaned = digits.toString();
        if (cleaned.length() > 1 && cleaned.charAt(0) == '0') cleaned = cleaned.substring(1);
        if (cleaned.isEmpty()) cleaned = "0";

        if (!cleaned.equals(text)) {
            suppressResponder = true;
            try {
                this.threshold.setValue(cleaned);
            } finally {
                suppressResponder = false;
            }
        }

        int amount;
        try {
            amount = Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putInt("threshold", amount);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                new ControlEventPayload(dipole.getBlockPos(), data));
    }
}
