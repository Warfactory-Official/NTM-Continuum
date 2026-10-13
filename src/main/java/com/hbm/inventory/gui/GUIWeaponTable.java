package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerWeaponTable;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.client.render.weapon.WeaponRenderer;
import com.hbm.client.render.weapon.WeaponRenderers;
import com.hbm.client.render.weapon.WeaponTableFrames;
import com.mojang.math.Axis;
import dev.golem.core.gui.GuiScenes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The source 176x240 weapon-table screen with 1.7's rotatable, per-gun framed preview. */
public final class GUIWeaponTable extends CompatContainerScreen<ContainerWeaponTable> {
    private static final Identifier TEXTURE = Ids.of(
            "hbm", "textures/gui/machine/gui_weapon_modifier.png");
    private static final int PREVIEW_X = 8, PREVIEW_Y = 17, PREVIEW_W = 160, PREVIEW_H = 80;
    private static final int CONFIG_X = 26, CONFIG_Y = 111, CONFIG_W = 7, CONFIG_H = 10;
    /** 1.7 {@code glTranslated(guiLeft + 88, guiTop + 57, 100)}: where the gun's model origin sits. */
    private static final int PREVIEW_CENTRE_X = 88, PREVIEW_CENTRE_Y = 57;
    /** 1.7 drew the preview with the GUI's lightmap off, i.e. at full brightness. */
    private static final int FULL_BRIGHT = 0xF000F0;
    private double yaw = 20D;
    private double pitch = -10D;

    public GUIWeaponTable(ContainerWeaponTable menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 240);
        inventoryLabelY = imageHeight - 96 + 2;
        titleLabelY = 5;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        ItemStack gun = menu.previewStack();
        // 1.7 drawScreen: the turn follows the held left button only while the cursor is over the box.
        if (Minecraft.getInstance().mouseHandler.isLeftPressed()
                && isHovering(PREVIEW_X, PREVIEW_Y + 1, PREVIEW_W, PREVIEW_H - 1, mouseX, mouseY)) {
            updateRotation(mouseX, mouseY);
        }
        if (gun.getItem() instanceof ItemGunBaseNT weapon) {
            int config = weapon.isValidConfigIndex(menu.configIndex()) ? menu.configIndex() : 0;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 35, topPos + 112,
                    176F + 6F * config, 0F, 6, 8, 256, 256);
            submitPreview(graphics, gun, config);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 5, 0xFFFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    /**
     * 1.7 {@code drawGuiContainerBackgroundLayer}: {@code glTranslated(guiLeft + 88, guiTop + 57, 100)}, the mouse
     * turn ({@code glRotated(yaw, 0, 1, 0)}, {@code glRotated(pitch, 1, 0, 0)}), then the gun's own
     * {@code setupModTable} ({@link WeaponTableFrames}) and {@code renderModTable(gun, index)}. The scene is the
     * whole screen, not the 160x80 box, because 1.7 did not clip: a large gun (the Quadro is scaled by 30) ran
     * over the frame there too. {@link GuiScenes}' frame is the legacy GUI frame (y down, origin at the scene
     * centre), so the 1.7 calls apply unchanged after moving the origin to the preview centre. Only a gun with
     * an {@code ItemRenderWeaponBase} in 1.7 is drawn; {@code gun_fireext} shows no preview, as there.
     */
    private void submitPreview(GuiGraphicsExtractor graphics, ItemStack gun, int config) {
        WeaponRenderer renderer = WeaponRenderers.forStack(gun);
        WeaponTableFrames.Frame frame = WeaponTableFrames.forItem(
                BuiltInRegistries.ITEM.getKey(gun.getItem()).getPath());
        if (renderer == null || frame == null) return;
        ItemStack stack = gun.copy();
        float centreX = leftPos + PREVIEW_CENTRE_X - width / 2F;
        float centreY = topPos + PREVIEW_CENTRE_Y - height / 2F;
        float turnYaw = (float) yaw;
        float turnPitch = (float) pitch;
        GuiScenes.draw(graphics, 0, 0, width, height, (pose, sink, camera) -> {
            pose.pushPose();
            pose.translate(centreX, centreY, 0F);
            pose.mulPose(Axis.YP.rotationDegrees(turnYaw));
            pose.mulPose(Axis.XP.rotationDegrees(turnPitch));
            float scale = (float) -frame.scale();
            pose.scale(scale, scale, scale);
            pose.mulPose(Axis.YP.rotationDegrees((float) frame.yaw()));
            pose.translate((float) frame.x(), (float) frame.y(), (float) frame.z());
            renderer.renderModTable(stack, config, pose, sink, FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        });
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();
        if (event.button() == 0 && isHovering(PREVIEW_X, PREVIEW_Y + 1, PREVIEW_W, PREVIEW_H - 1, x, y)) {
            updateRotation(x, y);
            return true;
        }
        if (event.button() == 0 && isHovering(CONFIG_X, CONFIG_Y, CONFIG_W, CONFIG_H, x, y)) {
            ItemStack gun = menu.previewStack();
            if (gun.getItem() instanceof ItemGunBaseNT weapon && weapon.getConfigCount() > 1
                    && Minecraft.getInstance().gameMode != null) {
                int target = (menu.configIndex() + 1) % weapon.getConfigCount();
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, target);
                return true;
            }
        }
        return false;
    }

    private void updateRotation(int mouseX, int mouseY) {
        yaw = (leftPos + PREVIEW_X + PREVIEW_W / 2D - mouseX) / (PREVIEW_W / 2D) * -180D;
        pitch = (topPos + PREVIEW_Y + 1 + (PREVIEW_H - 1) / 2D - mouseY)
                / ((PREVIEW_H - 1) / 2D) * 90D;
    }
}
