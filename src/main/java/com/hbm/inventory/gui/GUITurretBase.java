package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerTurretBase;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import dev.golem.core.Ids;
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
 * The screen every turret shares: 1.7 {@code GUITurretBase} with its per-turret subclasses folded in. The texture
 * comes from {@link TileEntityTurretBaseNT#guiTexture()} (1.7's {@code getTexture()} overrides: Greg = arty,
 * Chekhov = base, Jeremy = cannon, Tauon = tau, ...), the mode button from {@code GUITurretArty} /
 * {@code GUITurretHIMARS}, and the fuel tank from {@code GUITurretFritz}. Hit tests use 1.7's
 * {@code left <= x < left + w, top < y <= top + h}.
 */
public final class GUITurretBase extends CompatContainerScreen<ContainerTurretBase> {

    private static final Identifier FALLBACK_TEXTURE =
            Ids.of("hbm", "textures/gui/weapon/gui_turret_base.png");

    private static final int SWITCH_X = 115, SWITCH_Y = 26, SWITCH_SIZE = 18;
    private static final int MODE_X = 151, MODE_Y = 16, MODE_SIZE = 18;
    private static final int TOGGLE_Y = 30, TOGGLE_SIZE = 10;
    private static final int[] TOGGLE_X = {8, 22, 36, 50};
    private static final int PREV_X = 7, NEXT_X = 43, LIST_Y = 80, NAME_Y = 98, LIST_SIZE = 18;
    private static final int POWER_X = 152, POWER_Y = 45, POWER_W = 16, POWER_H = 52;
    private static final int AMMO_X = 79, AMMO_Y = 62, AMMO_SIZE = 54;
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

    /**
     * The name box is registered for input only, never as a renderable: 1.7 draws its contents at
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
            GuiDraw.blit(graphics, getTexture(), this.leftPos + SWITCH_X, this.topPos + SWITCH_Y, 176.0F, 40.0F,
                    SWITCH_SIZE, SWITCH_SIZE, 256, 256);
        }

        boolean[] targets = {turret.targetPlayers, turret.targetAnimals, turret.targetMobs, turret.targetMachines};
        for (int i = 0; i < targets.length; i++) {
            if (!targets[i]) continue;
            GuiDraw.blit(graphics, getTexture(), this.leftPos + TOGGLE_X[i], this.topPos + TOGGLE_Y,
                    176.0F, (float) (i * 10), TOGGLE_SIZE, TOGGLE_SIZE, 256, 256);
        }

        drawTally(graphics, turret.stattrak);

        // Subclass layers drawn after the base (1.7 GUITurretFritz / GUITurretArty / GUITurretHIMARS).
        drawTank(graphics, turret);
        int modeV = modeOverlayV(turret);
        if (modeV >= 0) {
            GuiDraw.blit(graphics, getTexture(), this.leftPos + MODE_X, this.topPos + MODE_Y,
                    210.0F, modeV, MODE_SIZE, MODE_SIZE, 256, 256);
        }
    }

    /** 1.7 {@code GUITurretFritz}: {@code tank.renderTank(guiLeft + 134, guiTop + 115, z, 7, 52)}; only the Fritz has one. */
    private void drawTank(GuiGraphicsExtractor graphics, TileEntityTurretBaseNT turret) {
        FluidTankNTM tank = turret.turretTank();
        if (tank == null || tank.getMaxFill() <= 0) return;
        GuiUtil.tank(graphics, tank, this.leftPos + TANK_X, this.topPos + TANK_Y + TANK_H, TANK_W, TANK_H);
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
        if (over(x, y, LIST_SIZE, LIST_SIZE, mouseX, mouseY)) {
            GuiDraw.blit(graphics, getTexture(), this.leftPos + x, this.topPos + y, (float) u, (float) v,
                    LIST_SIZE, LIST_SIZE, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7: both labels in 4210752 on every turret (the per-turret label colours were 1.12.2's).
        Component name = this.title;
        GuiDraw.text(graphics, this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 8,
                this.imageHeight - 96 + 2, 4210752, false);

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

        if (over(POWER_X, POWER_Y, POWER_W, POWER_H, mouseX, mouseY)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(turret.getPower(), turret.getMaxPower()), mouseX, mouseY);
            return;
        }

        String[] keys = {"turret.players", "turret.animals", "turret.mobs", "turret.machines"};
        boolean[] targets = {turret.targetPlayers, turret.targetAnimals, turret.targetMobs, turret.targetMachines};
        for (int i = 0; i < keys.length; i++) {
            if (!over(TOGGLE_X[i], TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE, mouseX, mouseY)) continue;
            Component state = Component.translatable(targets[i] ? "turret.on" : "turret.off")
                                       .withStyle(targets[i] ? ChatFormatting.GREEN : ChatFormatting.RED);
            GuiDraw.tooltip(graphics, this.font, List.of(Component.translatable(keys[i], state)),
                    Optional.empty(), mouseX, mouseY);
            return;
        }

        // 1.7: only while nothing is carried on the cursor.
        if (this.menu.getCarried().isEmpty() && over(AMMO_X, AMMO_Y, AMMO_SIZE, AMMO_SIZE, mouseX, mouseY)) {
            if (drawAmmo(graphics, turret, mouseX, mouseY)) return;
        }

        String modeKey = modeTooltipKey(turret);
        if (modeKey != null && over(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, modeTooltip(modeKey), Optional.empty(), mouseX, mouseY);
            return;
        }

        FluidTankNTM tank = turret.turretTank();
        if (tank != null && over(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(tank), mouseX, mouseY);
        }
    }

    /** Suppressed while a hovered ammo slot has its own tooltip, exactly as 1.7 does. Returns whether it drew. */
    private boolean drawAmmo(GuiGraphicsExtractor graphics, TileEntityTurretBaseNT turret, int mouseX, int mouseY) {
        for (int i = TileEntityTurretBaseNT.SLOT_AMMO_FIRST; i <= TileEntityTurretBaseNT.SLOT_AMMO_LAST; i++) {
            if (this.menu.getSlot(i).hasItem() && over(this.menu.getSlot(i).x, this.menu.getSlot(i).y,
                    16, 16, mouseX, mouseY)) {
                return false;
            }
        }

        List<ItemStack> ammo = turret.getAmmoTypesForDisplay();
        if (ammo.isEmpty()) return false;

        GuiUtil.cycledStackText(graphics, this.font, this.width, this.height, null, ammo, mouseX, mouseY);
        return true;
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityTurretBaseNT turret = this.menu.turret();
        if (turret == null) return false;

        double x = event.x();
        double y = event.y();
        this.field.setFocused(this.field.isMouseOver(event.x(), event.y()));
        if (this.field.isFocused()) setFocused(this.field);

        if (over(SWITCH_X, SWITCH_Y, SWITCH_SIZE, SWITCH_SIZE, x, y)) return toggle(turret, 0);
        for (int i = 0; i < TOGGLE_X.length; i++) {
            if (over(TOGGLE_X[i], TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE, x, y)) return toggle(turret, i + 1);
        }

        List<String> names = whitelist();
        int count = names == null ? 0 : names.size();
        if (count > 0) {
            if (over(PREV_X, LIST_Y, LIST_SIZE, LIST_SIZE, x, y)) {
                index = (index - 1 + count) % count;
                GuiUtil.playClickSound();
                return true;
            }
            if (over(NEXT_X, LIST_Y, LIST_SIZE, LIST_SIZE, x, y)) {
                index = (index + 1) % count;
                GuiUtil.playClickSound();
                return true;
            }
        }

        if (over(PREV_X, NAME_Y, LIST_SIZE, LIST_SIZE, x, y)) {
            // 1.7 plays the click before checking for an empty name.
            GuiUtil.playClickSound();
            if (this.field.getValue().isEmpty()) return true;
            CompoundTag data = new CompoundTag();
            data.putString("name", this.field.getValue());
            send(turret, data);
            this.field.setValue("");
            return true;
        }

        if (over(NEXT_X, NAME_Y, LIST_SIZE, LIST_SIZE, x, y)) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putInt("del", this.index);
            send(turret, data);
            return true;
        }

        // 1.7 GUITurretArty / GUITurretHIMARS: the mode button, checked after the base class's buttons.
        if (modeTooltipKey(turret) != null && over(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE, x, y)) {
            return toggle(turret, 5);
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

    private static int modeOverlayV(TileEntityTurretBaseNT turret) {
        if (turret instanceof TileEntityTurretArty arty) {
            return switch (arty.mode) {
                case TileEntityTurretArty.MODE_CANNON -> 0;
                case TileEntityTurretArty.MODE_MANUAL -> 18;
                default -> -1;
            };
        }
        if (turret instanceof TileEntityTurretHIMARS himars
                && himars.mode == TileEntityTurretHIMARS.MODE_MANUAL) return 0;
        return -1;
    }

    private static @org.jspecify.annotations.Nullable String modeTooltipKey(TileEntityTurretBaseNT turret) {
        if (turret instanceof TileEntityTurretArty arty) {
            return arty.mode == TileEntityTurretArty.MODE_ARTILLERY ? "turret.arty.artillery"
                    : arty.mode == TileEntityTurretArty.MODE_CANNON ? "turret.arty.cannon" : "turret.arty.manual";
        }
        if (turret instanceof TileEntityTurretHIMARS himars) {
            return himars.mode == TileEntityTurretHIMARS.MODE_AUTO ? "turret.arty.artillery_rocket" : "turret.arty.manual_rocket";
        }
        return null;
    }

    private static List<Component> modeTooltip(String key) {
        String[] lines = Component.translatable(key).getString().split("\\$", -1);
        List<Component> tooltip = new ArrayList<>(lines.length);
        for (String line : lines) tooltip.add(Component.literal(line));
        return tooltip;
    }

    /** 1.7's hit test, GUI-relative. */
    private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
        return GuiUtil.hover(mouseX, mouseY, this.leftPos + x, this.topPos + y, w, h);
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
