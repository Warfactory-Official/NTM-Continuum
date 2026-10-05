package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFusionKlystron;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.hbm.util.BobMathUtil;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Fusion klystron control panel. The player types an output target in KyU and the machine emits that
 * much per tick, if its power and air can sustain it. Three lights along the bottom say which of those
 * two supplies is the limiting one, and whether the target is actually being met.
 */
public class GUIFusionKlystron extends dev.golem.core.gui.CompatContainerScreen<ContainerFusionKlystron> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_fusion_klystron.png");

    private EditBox field;
    /** Guards the responder against the normalisation below re-entering it. */
    private boolean suppressResponder;

    public GUIFusionKlystron(ContainerFusionKlystron menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 194, 200);
    }

    @Override
    protected void init() {
        super.init();
        String previous = this.field == null ? null : this.field.getValue();
        // Upstream's geometry: 102x12 at (84, 22), green on no background. The label below it at y=40
        // is the committed value, so the field must stay up here or the two overlap.
        this.field = new EditBox(this.font, leftPos + 84, topPos + 22, 102, 12, Component.empty());
        this.field.setTextColor(0xFF00FF00);
        this.field.setBordered(false);
        this.field.setMaxLength(12);
        if (previous != null) {
            this.field.setValue(previous);
        } else {
            TileEntityFusionKlystron klystron = this.menu.getKlystron();
            this.field.setValue(klystron == null ? "0" : String.valueOf(klystron.outputTarget));
        }
        this.field.setResponder(this::commit);
        addRenderableWidget(this.field);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityFusionKlystron klystron = this.menu.getKlystron();
        if (klystron == null) return;

        int p = (int) (klystron.power * 52 / Math.max(1L, klystron.getMaxPower()));
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, x + 8, y + 70 - p, 194F, (float) (52 - p), 16, p, 256, 256);

        // 1.7 GUIFusionKlystron gauges: output vs target, compressed air fill, power via the torus speed curve.
        double outputGauge = klystron.outputTarget <= 0 ? 0 : ((double) klystron.output / (double) klystron.outputTarget);
        double airGauge = klystron.compair.getMaxFill() == 0 ? 0
                : (double) klystron.compair.getFill() / klystron.compair.getMaxFill();
        double powerGauge = TileEntityFusionTorus.getSpeedScaled(klystron.maxPower, klystron.power);

        // power LED, cooling LED: green when comfortable and emitting, amber when merely present
        light(graphics, x + 160, y + 71, powerGauge, klystron.output > 0);
        light(graphics, x + 170, y + 71, airGauge, klystron.output > 0);
        // action LED
        if (klystron.output >= klystron.outputTarget && klystron.output > 0) {
            GuiDraw.blit(graphics, TEXTURE, x + 180, y + 71, 210F, 8F, 8, 8, 256, 256);
        } else if (klystron.output > 0) {
            GuiDraw.blit(graphics, TEXTURE, x + 180, y + 71, 210F, 0F, 8, 8, 256, 256);
        }

        GuiUtil.drawSmoothGauge(graphics, x + 52, y + 80, outputGauge, 5, 2, 1, 0xA00000);
        GuiUtil.drawSmoothGauge(graphics, x + 88, y + 80, airGauge, 5, 2, 1, 0xA00000);
        GuiUtil.drawSmoothGauge(graphics, x + 124, y + 80, powerGauge, 5, 2, 1, 0xA00000);
    }

    private void light(GuiGraphicsExtractor graphics, int x, int y, double gauge, boolean emitting) {
        if (gauge >= 0.5 && emitting) GuiDraw.blit(graphics, TEXTURE, x, y, 210F, 8F, 8, 8, 256, 256);
        else if (gauge > 0) GuiDraw.blit(graphics, TEXTURE, x, y, 210F, 0F, 8, 8, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, 115 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 35, this.imageHeight - 93, 4210752, false);

        TileEntityFusionKlystron klystron = this.menu.getKlystron();
        if (klystron == null) return;
        String result = "= " + BobMathUtil.getShortNumber(klystron.outputTarget) + "KyU";
        if (klystron.outputTarget == TileEntityFusionKlystron.MAX_OUTPUT) result += " (max)";
        GuiDraw.text(graphics, this.font, result, 183 - this.font.width(result), 40, 0x00FF00, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFusionKlystron klystron = this.menu.getKlystron();
        if (klystron == null) return;

        if (this.isHovering(8, 18, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(klystron.power, klystron.getMaxPower()), mouseX, mouseY);
        } else if (this.isHovering(43, 71, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(BobMathUtil.getShortNumber(klystron.output) + "KyU / "
                            + BobMathUtil.getShortNumber(klystron.outputTarget) + "KyU").withStyle(ChatFormatting.RESET))),
                    mouseX, mouseY);
        } else if (this.isHovering(76, 71, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(klystron.compair), mouseX, mouseY);
        } else if (this.isHovering(115, 71, 18, 18, mouseX, mouseY)) {
            // 1.7 prints the KyU output and target again here, labelled HE.
            tooltip(graphics, List.of(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(BobMathUtil.getShortNumber(klystron.output) + "HE / "
                            + BobMathUtil.getShortNumber(klystron.outputTarget) + "HE").withStyle(ChatFormatting.RESET))),
                    mouseX, mouseY);
        }
    }

    @Override
    protected boolean keyPressed(dev.golem.core.gui.KeyInput event) {
        // While the field has focus let it consume typing - including the inventory key ('e'), which
        // AbstractContainerScreen would otherwise take as "close the menu" mid-number. Escape still closes.
        if (this.field != null && this.field.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(this.field);
            return true;
        }
        return false;
    }

    /**
     * Sends the typed target. Upstream commits on every keystroke rather than on Enter, so the beam
     * responds as you type; the normalisation (strip a leading zero, empty becomes "0") is upstream's too.
     */
    private void commit(String text) {
        if (suppressResponder) return;
        TileEntityFusionKlystron klystron = this.menu.getKlystron();
        if (klystron == null) return;

        // Digits only, one leading zero stripped, empty reads as zero - upstream's normalisation, done
        // here rather than through EditBox.setFilter, which NeoForge's mappings expose and Fabric's do not.
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
                this.field.setValue(cleaned);
            } finally {
                suppressResponder = false;
            }
        }

        long amount;
        try {
            amount = Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return; // only reachable if the entry is longer than a long, which maxLength prevents
        }
        CompoundTag data = new CompoundTag();
        data.putLong("amount", amount);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(klystron.getBlockPos(), data));
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, text, Optional.empty(), mouseX, mouseY);
    }
}
