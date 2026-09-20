package com.hbm.inventory.gui;

import com.hbm.packet.ControlEventPayload;
import com.hbm.inventory.container.ContainerMachineExcavator;
import com.hbm.sound.ModProduction2Sounds;
import com.hbm.tileentity.machine.TileEntityMachineExcavator;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** 1.7 {@code GUIMachineExcavator}: {@code gui_mining_drill.png}, 242x204, five lever toggles. */
public final class GUIMachineExcavator extends CompatContainerScreen<ContainerMachineExcavator> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_mining_drill.png");
    private static final String[] TOGGLES = {"drill", "crusher", "walling", "veinminer", "silktouch"};

    public GUIMachineExcavator(ContainerMachineExcavator menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 242, 204);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, 242, 96, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 33, topPos + 104, 33F, 104F, 176, 100, 256, 256);

        TileEntityMachineExcavator drill = menu.getExcavator();
        if (drill == null) return;

        int i = (int) (drill.getPower() * 52 / drill.getMaxPower());
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 220, topPos + 70 - i, 229F, 156F - i, 16, i, 256, 256);

        if (drill.getPower() > drill.getPowerConsumption()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 224, topPos + 4, 239F, 156F, 9, 12, 256, 256);
        }

        if (drill.getInstalledDrill() == null && Production2Gui.blink()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 171, topPos + 74, 209F, 154F, 18, 18, 256, 256);
        }

        if (drill.enableDrill) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 6, topPos + 42, 209F, 114F, 20, 40, 256, 256);
            if (drill.getInstalledDrill() != null && drill.getPower() >= drill.getPowerConsumption()) lamp(graphics, 11, true);
            else if (Production2Gui.blink()) lamp(graphics, 11, false);
        }

        if (drill.enableCrusher) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 30, topPos + 42, 209F, 114F, 20, 40, 256, 256);
            lamp(graphics, 35, true);
        }

        if (drill.enableWalling) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 54, topPos + 42, 209F, 114F, 20, 40, 256, 256);
            lamp(graphics, 59, true);
        }

        if (drill.enableVeinMiner) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 78, topPos + 42, 209F, 114F, 20, 40, 256, 256);
            if (drill.canVeinMine()) lamp(graphics, 83, true);
            else if (Production2Gui.blink()) lamp(graphics, 83, false);
        }

        if (drill.enableSilkTouch) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 102, topPos + 42, 209F, 114F, 20, 40, 256, 256);
            if (drill.canSilkTouch()) lamp(graphics, 107, true);
            else if (Production2Gui.blink()) lamp(graphics, 107, false);
        }

        Production2Gui.drawTank(graphics, drill.tank, leftPos + 202, topPos + 70, 16, 52);
    }

    private void lamp(GuiGraphicsExtractor graphics, int x, boolean green) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + 5, green ? 209F : 219F, 104F, 10, 10, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, playerInventoryTitle, 8 + 33, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineExcavator drill = menu.getExcavator();
        if (drill == null) return;

        for (int i = 0; i < TOGGLES.length; i++) {
            if (isHovering(6 + i * 24, 42, 20, 40, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, List.of(Component.translatable("excavator." + TOGGLES[i])), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        if (isHovering(220, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(drill.getPower(), TileEntityMachineExcavator.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(202, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(drill.tank), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityMachineExcavator drill = menu.getExcavator();
        if (drill == null) return false;

        String toggle = null;
        for (int i = 0; i < TOGGLES.length; i++) {
            if (isHovering(6 + i * 24, 42, 20, 40, event.x(), event.y())) toggle = TOGGLES[i];
        }

        if (toggle != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModProduction2Sounds.LEVER_LARGE.get(), 1.0F));
            CompoundTag data = new CompoundTag();
            data.putBoolean(toggle, true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(drill.getBlockPos(), data));
            return true;
        }
        return false;
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getExcavator();
    }
}
