package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMixer;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixer screen — 1.7.10 {@code GUIMixer} ({@code processing/gui_mixer.png}, 176x204): power bar (12,18) 16x52,
 * progress (71,31) up to 52x44 from (192,0), tanks at x=52/61 (7 wide) and 126 (16 wide), bottom y=70, the
 * recipe-cycling button (71,17) 12x12 and the upgrade info panel (152,55).
 */
public class GUIMixer extends dev.golem.core.gui.CompatContainerScreen<ContainerMixer>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_mixer.png");

    public GUIMixer(ContainerMixer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    /** 1.7: title centred 20 px right of the panel centre ({@code xSize / 2 + 40 / 2}), inventory line. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 + 40 / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineMixer mixer = menu.getMixer();
        if (mixer == null) return;

        int i = (int) (mixer.getPower() * 52 / mixer.getMaxPower());
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 12, topPos + 70 - i, 176F, 52F - i, 16, i, 256, 256);

        if (mixer.processTime > 0 && mixer.progress > 0) {
            int j = mixer.progress * 52 / mixer.processTime;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 71, topPos + 31, 192F, 0F, j, 44, 256, 256);
        }

        GuiUtil.tank(graphics, mixer.tanks[0], leftPos + 52, topPos + 70, 7, 52);
        GuiUtil.tank(graphics, mixer.tanks[1], leftPos + 61, topPos + 70, 7, 52);
        GuiUtil.tank(graphics, mixer.tanks[2], leftPos + 126, topPos + 70, 16, 52);

        GuiUtil.drawInfoPanel(graphics, leftPos + 152, topPos + 55, 8);
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineMixer mixer = menu.getMixer();
        // 1.7: x [71,83), y (17,29].
        if (mixer != null && GuiUtil.hover(event.x(), event.y(), leftPos + 71, topPos + 17, 12, 12)) {
            GuiUtil.playClickSound();
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putBoolean("toggle", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(mixer.getBlockPos(), data));
            return true;
        }
        return false;
    }

    /** 1.7 {@code drawScreen}: energy, upgrade text, recipe cycler (only with more than one recipe), tanks. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineMixer mixer = menu.getMixer();
        if (mixer == null) return;
        int gl = leftPos, gt = topPos;
        if (GuiUtil.hover(mouseX, mouseY, gl + 12, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(mixer.getPower(), mixer.getMaxPower()), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 152, gt + 55, 8, 8)) {
            GuiUtil.tooltip(graphics, font, List.of(
                    Component.translatable("desc.gui.upgrade"),
                    Component.translatable("desc.gui.upgrade.speed"),
                    Component.translatable("desc.gui.upgrade.power"),
                    Component.translatable("desc.gui.upgrade.overdrive")), mouseX, mouseY);
            return;
        }
        MixerRecipe[] recipes = MixerRecipes.getOutput(mixer.tanks[2].getTankType());
        if (recipes != null && recipes.length > 1 && GuiUtil.hover(mouseX, mouseY, gl + 71, gt + 17, 12, 12)) {
            List<Component> label = new ArrayList<>();
            label.add(Component.literal(ChatFormatting.YELLOW + "Current recipe (" + (mixer.recipeIndex + 1) + "/" + recipes.length + "):"));
            MixerRecipe recipe = recipes[Math.floorMod(mixer.recipeIndex, recipes.length)];
            if (recipe.input1 != null) label.add(Component.literal("-").append(recipe.input1.type.getDisplayName()));
            if (recipe.input2 != null) label.add(Component.literal("-").append(recipe.input2.type.getDisplayName()));
            if (recipe.solidInput != null) label.add(Component.literal("-").append(recipe.solidInput.getCyclingDisplayStack().getHoverName()));
            label.add(Component.literal(ChatFormatting.RED + "Click to change!"));
            GuiUtil.tooltip(graphics, font, label, mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 52, gt + 18, 7, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(mixer.tanks[0]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 61, gt + 18, 7, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(mixer.tanks[1]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 126, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(mixer.tanks[2]), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getMixer();
    }
}
