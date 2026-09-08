package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerTurretBase;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The screen every turret shares: the on switch, the four target-class toggles, the biometry
 * whitelist editor, an energy bar and the kill tally.
 *
 * <p>1.12.2 draws the ammo list through {@code GuiInfoContainer.drawStackText}; that helper has no
 * counterpart here, so the panel is rebuilt inline in the same shape (the same dark frame, the same
 * one-second cycle through the loaded rounds).
 */
public final class GUITurretBase extends CompatContainerScreen<ContainerTurretBase> {

    private static final Identifier FALLBACK_TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/weapon/gui_turret_base.png");

    private static final int SWITCH_X = 115, SWITCH_Y = 25, SWITCH_SIZE = 18;
    private static final int TOGGLE_Y = 29, TOGGLE_SIZE = 10;
    private static final int[] TOGGLE_X = {8, 22, 36, 50};
    private static final int PREV_X = 7, NEXT_X = 43, LIST_Y = 80, NAME_Y = 98, LIST_SIZE = 18;
    private static final int POWER_X = 152, POWER_Y = 45, POWER_W = 16, POWER_H = 52;
    private static final int AMMO_X = 79, AMMO_Y = 61, AMMO_SIZE = 54;
    private static final int TANK_X = 134, TANK_Y = 63, TANK_W = 7, TANK_H = 52;

    private EditBox field;
    private int index;

    public GUITurretBase(ContainerTurretBase menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    private Identifier getTexture() {
        TileEntityTurretBaseNT turret = this.menu.turret();
        return turret == null ? FALLBACK_TEXTURE : turret.guiTexture();
    }

    private int getTurretFontColor() {
        TileEntityTurretBaseNT turret = this.menu.turret();
        return turret == null ? 0x808080 : turret.guiFontColor();
    }

    /**
     * The name box is registered for input only, never as a renderable: 1.12.2 draws its contents at
     * half scale to fit the painted slot, which is done in {@link #extractLabels} instead.
     */
    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(this.font, this.leftPos + 10, this.topPos + 65, 50, 14, Component.empty());
        this.field.setBordered(false);
        this.field.setMaxLength(25);
        this.focusedEditBoxForInput = this.field;
        addWidget(this.field);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, getTexture(), this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        hover(graphics, mouseX, mouseY, PREV_X, LIST_Y, 176, 58);
        hover(graphics, mouseX, mouseY, NEXT_X, LIST_Y, 194, 58);
        hover(graphics, mouseX, mouseY, PREV_X, NAME_Y, 176, 76);
        hover(graphics, mouseX, mouseY, NEXT_X, NAME_Y, 194, 76);

        TileEntityTurretBaseNT turret = this.menu.turret();
        if (turret == null) return;

        int filled = turret.getPowerScaled(53);
        if (filled > 0) {
            GuiDraw.blit(graphics, getTexture(), this.leftPos + POWER_X, this.topPos + 97 - filled,
                    194.0F, (float) (52 - filled), POWER_W, filled, 256, 256);
        }

        if (turret.isOn) {
            GuiDraw.blit(graphics, getTexture(), this.leftPos + SWITCH_X, this.topPos + 26, 176.0F, 40.0F,
                    SWITCH_SIZE, SWITCH_SIZE, 256, 256);
        }

        boolean[] targets = {turret.targetPlayers, turret.targetAnimals, turret.targetMobs, turret.targetMachines};
        for (int i = 0; i < targets.length; i++) {
            if (!targets[i]) continue;
            GuiDraw.blit(graphics, getTexture(), this.leftPos + TOGGLE_X[i], this.topPos + 30,
                    176.0F, (float) (i * 10), TOGGLE_SIZE, TOGGLE_SIZE, 256, 256);
        }

        drawTally(graphics, turret.stattrak);
        drawTank(graphics, turret);
    }

    /** Only a fuel-burning turret has one; the rest paint nothing over that part of the background. */
    private void drawTank(GuiGraphicsExtractor graphics, TileEntityTurretBaseNT turret) {
        com.hbm.inventory.fluid.tank.FluidTankNTM tank = turret.turretTank();
        if (tank == null || tank.getMaxFill() <= 0) return;

        int filled = (int) ((long) tank.getFill() * TANK_H / tank.getMaxFill());
        if (filled <= 0) return;

        int x = this.leftPos + TANK_X;
        int bottom = this.topPos + TANK_Y + TANK_H;
        graphics.fill(x, bottom - filled, x + TANK_W, bottom,
                0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
    }

    private void drawTally(GuiGraphicsExtractor graphics, int tallies) {
        if (tallies >= 36) {
            GuiDraw.blit(graphics, getTexture(), this.leftPos + 77, this.topPos + 50, 176.0F, 120.0F, 63, 6, 256, 256);
            return;
        }

        int steps = (int) Math.ceil(tallies / 5D);
        for (int s = 0; s < steps; s++) {
            int remainder = tallies % 5;
            if (s < steps - 1 || remainder == 0) {
                GuiDraw.blit(graphics, getTexture(), this.leftPos + 77 + 9 * s, this.topPos + 50,
                        194.0F, 94.0F, 9, 6, 256, 256);
            } else {
                GuiDraw.blit(graphics, getTexture(), this.leftPos + 77 + 9 * s, this.topPos + 50,
                        176.0F, 94.0F, remainder * 2, 6, 256, 256);
            }
        }
    }

    private void hover(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int u, int v) {
        if (isHovering(x, y, LIST_SIZE, LIST_SIZE, mouseX, mouseY)) {
            GuiDraw.blit(graphics, getTexture(), this.leftPos + x, this.topPos + y, (float) u, (float) v,
                    LIST_SIZE, LIST_SIZE, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int color = getTurretFontColor();
        Component name = this.title;
        GuiDraw.text(graphics, this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, color, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 8,
                this.imageHeight - 96 + 2, color, false);

        List<String> names = whitelist();
        clampIndex(names);
        String selected = names == null || names.isEmpty()
                ? ChatFormatting.ITALIC + Component.translatable("turret.none").getString()
                : names.get(index);

        String typed = this.field.getValue();
        if (this.field.isFocused()) {
            String cursor = System.currentTimeMillis() % 1000 < 500 ? " " : "||";
            int caret = Math.min(this.field.getCursorPosition(), typed.length());
            typed = typed.substring(0, caret) + cursor + typed.substring(caret);
        }

        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 0.5F, 0.5F);
        GuiDraw.text(graphics, this.font, selected, 24, 102, 0x00ff00, false);
        GuiDraw.text(graphics, this.font, typed, 24, 138, 0x00ff00, false);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityTurretBaseNT turret = this.menu.turret();
        if (turret == null) return;

        if (isHovering(POWER_X, POWER_Y, POWER_W, POWER_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(turret.getPower(), turret.getMaxPower()), Optional.empty(), mouseX, mouseY);
            return;
        }

        String[] keys = {"turret.players", "turret.animals", "turret.mobs", "turret.machines"};
        boolean[] targets = {turret.targetPlayers, turret.targetAnimals, turret.targetMobs, turret.targetMachines};
        for (int i = 0; i < keys.length; i++) {
            if (!isHovering(TOGGLE_X[i], TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE, mouseX, mouseY)) continue;
            Component state = Component.translatable(targets[i] ? "turret.on" : "turret.off")
                                       .withStyle(targets[i] ? ChatFormatting.GREEN : ChatFormatting.RED);
            GuiDraw.tooltip(graphics, this.font, List.of(Component.translatable(keys[i], state)),
                    Optional.empty(), mouseX, mouseY);
            return;
        }

        com.hbm.inventory.fluid.tank.FluidTankNTM tank = turret.turretTank();
        if (tank != null && isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.tankTooltip(tank), Optional.empty(), mouseX, mouseY);
            return;
        }

        if (isHovering(AMMO_X, AMMO_Y, AMMO_SIZE, AMMO_SIZE, mouseX, mouseY)) drawAmmo(graphics, turret, mouseX, mouseY);
    }

    /** Suppressed while a hovered ammo slot has its own tooltip, exactly as 1.12.2 does. */
    private void drawAmmo(GuiGraphicsExtractor graphics, TileEntityTurretBaseNT turret, int mouseX, int mouseY) {
        for (int i = TileEntityTurretBaseNT.SLOT_AMMO_FIRST; i <= TileEntityTurretBaseNT.SLOT_AMMO_LAST; i++) {
            if (this.menu.getSlot(i).hasItem() && isHovering(this.menu.getSlot(i).x, this.menu.getSlot(i).y,
                    16, 16, mouseX, mouseY)) {
                return;
            }
        }

        List<ItemStack> ammo = new ArrayList<>(turret.getAmmoTypesForDisplay());
        if (ammo.isEmpty()) return;

        int highlight = ammo.size() > 1
                ? (int) ((System.currentTimeMillis() % (1000L * ammo.size())) / 1000L)
                : 0;
        ItemStack selected = ammo.get(highlight);

        List<List<ItemStack>> rows = new ArrayList<>();
        if (ammo.size() < 10) {
            rows.add(ammo);
        } else if (ammo.size() < 24) {
            rows.add(ammo.subList(0, ammo.size() / 2));
            rows.add(ammo.subList(ammo.size() / 2, ammo.size()));
        } else {
            int first = (int) Math.ceil(ammo.size() / 3D);
            int second = (int) Math.ceil(ammo.size() / 3D * 2D);
            rows.add(ammo.subList(0, first));
            rows.add(ammo.subList(first, second));
            rows.add(ammo.subList(second, ammo.size()));
        }

        String label = selected.getHoverName().getString();
        int panelWidth = this.font.width(label);
        for (List<ItemStack> row : rows) panelWidth = Math.max(panelWidth, row.size() * 18);
        int panelHeight = 12 + rows.size() * 18;
        int minX = mouseX + 12;
        int minY = mouseY - 12;
        if (minX + panelWidth > this.width) minX -= 28 + panelWidth;
        if (minY + panelHeight + 6 > this.height) minY = this.height - panelHeight - 6;

        GuiDraw.nextStratum(graphics);

        int background = 0xF0100010;
        graphics.fill(minX - 3, minY - 4, minX + panelWidth + 3, minY - 3, background);
        graphics.fill(minX - 3, minY + panelHeight + 3, minX + panelWidth + 3, minY + panelHeight + 4, background);
        graphics.fill(minX - 3, minY - 3, minX + panelWidth + 3, minY + panelHeight + 3, background);
        graphics.fill(minX - 4, minY - 3, minX - 3, minY + panelHeight + 3, background);
        graphics.fill(minX + panelWidth + 3, minY - 3, minX + panelWidth + 4, minY + panelHeight + 3, background);

        int border = 0x505000FF;
        int borderShadow = (border & 0xFEFEFE) >> 1 | border & 0xFF000000;
        graphics.fill(minX - 3, minY - 2, minX - 2, minY + panelHeight + 2, border);
        graphics.fill(minX + panelWidth + 2, minY - 2, minX + panelWidth + 3, minY + panelHeight + 2, border);
        graphics.fill(minX - 3, minY - 3, minX + panelWidth + 3, minY - 2, border);
        graphics.fill(minX - 3, minY + panelHeight + 2, minX + panelWidth + 3, minY + panelHeight + 3, borderShadow);

        int lineY = minY;
        int itemIndex = 0;
        for (List<ItemStack> row : rows) {
            for (int column = 0; column < row.size(); column++) {
                if (itemIndex == highlight) {
                    graphics.fill(minX + column * 18 - 1, lineY - 1, minX + column * 18 + 17, lineY + 17, 0xFFFF0000);
                    graphics.fill(minX + column * 18, lineY, minX + column * 18 + 16, lineY + 16, 0xFF808080);
                }
                GuiDraw.item(graphics, row.get(column), minX + column * 18, lineY);
                itemIndex++;
            }
            lineY += 18;
        }
        GuiDraw.text(graphics, this.font, label, minX, lineY, 0xFFFFFFFF, true);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityTurretBaseNT turret = this.menu.turret();
        if (turret == null) return false;

        int x = (int) event.x();
        int y = (int) event.y();
        this.field.setFocused(this.field.isMouseOver(event.x(), event.y()));
        if (this.field.isFocused()) setFocused(this.field);

        if (isHovering(SWITCH_X, SWITCH_Y, SWITCH_SIZE, SWITCH_SIZE, x, y)) return toggle(turret, 0);
        for (int i = 0; i < TOGGLE_X.length; i++) {
            if (isHovering(TOGGLE_X[i], TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE, x, y)) return toggle(turret, i + 1);
        }

        List<String> names = whitelist();
        int count = names == null ? 0 : names.size();
        if (count > 0) {
            if (isHovering(PREV_X, LIST_Y, LIST_SIZE, LIST_SIZE, x, y)) {
                index = (index - 1 + count) % count;
                GuiUtil.playClickSound();
                return true;
            }
            if (isHovering(NEXT_X, LIST_Y, LIST_SIZE, LIST_SIZE, x, y)) {
                index = (index + 1) % count;
                GuiUtil.playClickSound();
                return true;
            }
        }

        if (isHovering(PREV_X, NAME_Y, LIST_SIZE, LIST_SIZE, x, y)) {
            if (this.field.getValue().isEmpty()) return true;
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putString("name", this.field.getValue());
            send(turret, data);
            this.field.setValue("");
            return true;
        }

        if (isHovering(NEXT_X, NAME_Y, LIST_SIZE, LIST_SIZE, x, y)) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putInt("del", this.index);
            send(turret, data);
            return true;
        }

        return false;
    }

    private boolean toggle(TileEntityTurretBaseNT turret, int button) {
        GuiUtil.playClickSound();
        CompoundTag data = new CompoundTag();
        data.putInt("toggle", button);
        send(turret, data);
        return true;
    }

    private void send(TileEntityTurretBaseNT turret, CompoundTag data) {
        GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                new ControlEventPayload(turret.getBlockPos(), data));
    }

    private @org.jspecify.annotations.Nullable List<String> whitelist() {
        TileEntityTurretBaseNT turret = this.menu.turret();
        return turret == null ? null : turret.getWhitelist();
    }

    private void clampIndex(@org.jspecify.annotations.Nullable List<String> names) {
        int count = names == null ? 0 : names.size();
        if (index >= count) index = count - 1;
        if (index < 0) index = 0;
    }
}
