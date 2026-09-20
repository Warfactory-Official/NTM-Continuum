package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeCustom;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;
import com.hbm.explosion.ExplosionNukeCustom;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code GUINukeCustom}: the 9x3 grid over the "gun bomb" schematic. The lit stage icon is the highest
 * stage present (anti-mass > schrabidium > antimatter > thermonuclear > nuclear > TNT); the salted icon lights
 * only for a plain nuclear/thermonuclear device. Hovering a stage icon shows its level, adjusted level and caps.
 */
public class GUINukeCustom extends CompatContainerScreen<ContainerNukeCustom> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/weapon/gunbombschematic.png");

    public GUINukeCustom(ContainerNukeCustom menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityNukeCustom bomb = menu.getCustom();
        if (bomb == null) return;

        if (bomb.euph > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 89, 176F, 108F, 18, 18, 256, 256);
        else if (bomb.schrab > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 106, topPos + 89, 176F, 90F, 18, 18, 256, 256);
        else if (bomb.amat > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 70, topPos + 89, 176F, 54F, 18, 18, 256, 256);
        else if (bomb.hydro > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 52, topPos + 89, 176F, 36F, 18, 18, 256, 256);
        else if (bomb.nuke > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 34, topPos + 89, 176F, 18F, 18, 18, 256, 256);
        else if (bomb.tnt > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 16, topPos + 89, 176F, 0F, 18, 18, 256, 256);

        if (bomb.dirty > 0 && bomb.nuke > 0 && bomb.amat == 0 && bomb.schrab == 0 && bomb.euph == 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 88, topPos + 89, 176F, 72F, 18, 18, 256, 256);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityNukeCustom bomb = menu.getCustom();
        if (bomb == null) return;

        if (legacyHover(16, 89, mouseX, mouseY)) {
            tooltip(graphics, mouseX, mouseY,
                    ChatFormatting.YELLOW + "Conventional Explosives (Level " + bomb.tnt + "/" + Math.min(bomb.tnt, ExplosionNukeCustom.MAX_TNT) + ")",
                    "Caps at " + ExplosionNukeCustom.MAX_TNT,
                    "N²-like above level 75",
                    ChatFormatting.ITALIC + "\"Goes boom\"");
        } else if (legacyHover(34, 89, mouseX, mouseY)) {
            tooltip(graphics, mouseX, mouseY,
                    ChatFormatting.YELLOW + "Nuclear (Level " + bomb.nuke + "/" + bomb.getNukeAdj() + ")",
                    "Requires TNT level 16",
                    "Caps at " + ExplosionNukeCustom.MAX_NUKE,
                    "Has fallout",
                    ChatFormatting.ITALIC + "\"Now I am become death, destroyer of worlds.\"");
        } else if (legacyHover(52, 89, mouseX, mouseY)) {
            tooltip(graphics, mouseX, mouseY,
                    ChatFormatting.YELLOW + "Thermonuclear (Level " + bomb.hydro + "/" + bomb.getHydroAdj() + ")",
                    "Requires nuclear level 100",
                    "Caps at " + ExplosionNukeCustom.MAX_HYDRO,
                    "Reduces added fallout by salted stage by 75%",
                    ChatFormatting.ITALIC + "\"And for my next trick, I'll make",
                    ChatFormatting.ITALIC + "the island of Elugelab disappear!\"");
        } else if (legacyHover(70, 89, mouseX, mouseY)) {
            tooltip(graphics, mouseX, mouseY,
                    ChatFormatting.YELLOW + "Antimatter (Level " + bomb.amat + "/" + bomb.getAmatAdj() + ")",
                    "Requires nuclear level 50",
                    "Caps at " + ExplosionNukeCustom.MAX_AMAT,
                    ChatFormatting.ITALIC + "\"Antimatter, Balefire, whatever.\"");
        } else if (legacyHover(88, 89, mouseX, mouseY)) {
            tooltip(graphics, mouseX, mouseY,
                    ChatFormatting.YELLOW + "Salted (Level " + bomb.dirty + "/" + Math.min(bomb.dirty, ExplosionNukeCustom.MAX_DIRTY) + ")",
                    "Extends fallout of nuclear and",
                    "thermonuclear stages",
                    "Caps at 100",
                    ChatFormatting.ITALIC + "\"Not to be confused with tablesalt.\"");
        } else if (legacyHover(106, 89, mouseX, mouseY)) {
            tooltip(graphics, mouseX, mouseY,
                    ChatFormatting.YELLOW + "Schrabidium (Level " + bomb.schrab + "/" + bomb.getSchrabAdj() + ")",
                    "Requires nuclear level 50",
                    "Caps at " + ExplosionNukeCustom.MAX_SCHRAB,
                    ChatFormatting.ITALIC + "\"For the hundredth time,",
                    ChatFormatting.ITALIC + "you can't bypass these caps!\"");
        } else if (legacyHover(142, 89, mouseX, mouseY)) {
            tooltip(graphics, mouseX, mouseY,
                    ChatFormatting.YELLOW + "Ice cream (Level unknown)",
                    ChatFormatting.ITALIC + "\"Probably not ice cream but the label came off.\"");
        }
    }

    /** 1.7 {@code drawCustomInfoStat}: closed lower / open upper bound on x, the reverse on y, 18x18. */
    private boolean legacyHover(int x, int y, int mouseX, int mouseY) {
        return leftPos + x <= mouseX && leftPos + x + 18 > mouseX && topPos + y < mouseY && topPos + y + 18 >= mouseY;
    }

    private void tooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, String... lines) {
        List<Component> components = new ArrayList<>(lines.length);
        for (String line : lines) components.add(Component.literal(line));
        GuiDraw.tooltip(graphics, font, components, Optional.empty(), mouseX, mouseY);
    }
}
