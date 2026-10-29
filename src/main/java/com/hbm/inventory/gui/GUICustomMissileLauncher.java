package com.hbm.inventory.gui;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.handler.MissileStruct;
import com.hbm.inventory.container.ContainerCustomMissileLauncher;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.tileentity.bomb.TileEntityCustomMissileLauncher;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.GuiEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code GUIMachineCompactLauncher} / {@code GUIMachineLaunchTable}, which differ only in texture, the table's
 * three pad-size buttons and the hover text. 176x222: tanks at (116,36) and (134,36) 16x34, solid fuel bar at
 * (152,36) 16x52, power bar (134,113) 34x6, lamps at y=23 (green = enough, red = not enough, none = unused), a
 * tick over the missile and designator slots when they are valid, and the loaded missile standing in the bay.
 *
 * <p>The bay missile is 1.7's {@code MissilePronter} call chain {@code T(88,115) Ry(90) T(h/2*s) S(s) S(-1)} with
 * {@code s = 90 / max(h, 6)}, drawn through a throwaway {@link EntityMissileCustom} because a 26.1 screen only
 * draws 3D through an entity render state. The chain is rewritten into {@link GuiEntities}' frame (pixels per
 * block, z flipped): the same pose becomes {@code T(0, 0, h/2) Ry(-90) Rz(180)} at scale {@code s}.
 */
public abstract class GUICustomMissileLauncher<M extends ContainerCustomMissileLauncher> extends CompatContainerScreen<M> {

    private final Identifier texture;
    private @Nullable EntityMissileCustom preview;
    private @Nullable MissileStruct previewFor;

    protected GUICustomMissileLauncher(M menu, Inventory playerInventory, Component title, Identifier texture) {
        super(menu, playerInventory, title, 176, 222);
        this.texture = texture;
    }

    protected Identifier texture() {
        return texture;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, texture, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityCustomMissileLauncher launcher = menu.getLauncher();
        if (launcher == null) return;

        int i = (int) launcher.getPowerScaled(34);
        if (i > 0) GuiDraw.blit(graphics, texture, leftPos + 134, topPos + 113, 176F, 96F, i, 6, 256, 256);

        int j = launcher.getSolidScaled(52);
        if (j > 0) GuiDraw.blit(graphics, texture, leftPos + 152, topPos + 88 - j, 176F, 96F - j, 16, j, 256, 256);

        if (launcher.isMissileValid())
            GuiDraw.blit(graphics, texture, leftPos + 25, topPos + 35, 176F, 26F, 18, 18, 256, 256);
        if (launcher.hasDesignator())
            GuiDraw.blit(graphics, texture, leftPos + 25, topPos + 71, 176F, 26F, 18, 18, 256, 256);

        lamp(graphics, 121, launcher.liquidState());
        lamp(graphics, 139, launcher.oxidizerState());
        lamp(graphics, 157, launcher.solidState());

        extractExtras(graphics, launcher);

        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 36, 2);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 36 + 16, 11);

        drawTank(graphics, launcher.tanks[0], 116, 36, 16, 34);
        drawTank(graphics, launcher.tanks[1], 134, 36, 16, 34);

        if (launcher.isMissileValid()) drawMissile(graphics, launcher.getItem(0));
    }

    /**
     * The table's pad-size highlight.
     */
    protected void extractExtras(GuiGraphicsExtractor graphics, TileEntityCustomMissileLauncher launcher) {
    }

    private void lamp(GuiGraphicsExtractor graphics, int x, int state) {
        if (state == 1) GuiDraw.blit(graphics, texture, leftPos + x, topPos + 23, 176F, 0F, 6, 8, 256, 256);
        if (state == 0) GuiDraw.blit(graphics, texture, leftPos + x, topPos + 23, 182F, 0F, 6, 8, 256, 256);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        TankDisplay.Fill visible = TankDisplay.visibleFill(tank, x, y, width, height, true);
        if (visible == null) return;
        int x0 = leftPos + visible.x(), top = topPos + visible.top(), bottom = topPos + visible.bottom();
        Identifier fluid = tank.getTankType().clientMetadata().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluid).isPresent()) {
            graphics.enableScissor(x0, top, x0 + visible.width(), bottom);
            for (int ty = bottom - 16; ty > top - 16; ty -= 16)
                GuiDraw.blit(graphics, fluid, x0, ty, 0F, 0F, visible.width(), 16, 16, 16, visible.tint());
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x0, top, x0 + width, bottom, visible.tint());
        }
    }

    private void drawMissile(GuiGraphicsExtractor graphics, ItemStack stack) {
        MissileStruct struct = ItemCustomMissile.getStruct(stack);
        if (struct == null || this.minecraft == null || this.minecraft.level == null) return;
        EntityMissileCustom entity = previewEntity(struct);
        float h = struct.height();
        float scale = 90F / Math.max(h, 6F);
        GuiEntities.draw(graphics, entity, leftPos + 43, topPos + 25, leftPos + 133, topPos + 205, scale,
                new Vector3f(0F, 0F, h / 2F),
                new Quaternionf().rotateY((float) Math.toRadians(-90)).rotateZ((float) Math.PI));
    }

    /**
     * One throwaway entity per screen, re-parted when the struct changes. Never added to a level.
     */
    protected EntityMissileCustom previewEntity(MissileStruct struct) {
        if (preview == null) {
            preview = new EntityMissileCustom(ModNtmEntities.MISSILE_CUSTOM.get(), this.minecraft.level);
            // RenderMissileCustom turns by yaw - 90 and back: 90 leaves the body in its model frame
            preview.setYRot(90F);
            preview.yRotO = 90F;
            if (this.minecraft.player != null) preview.setPos(this.minecraft.player.position());
        }
        if (!struct.equals(previewFor)) {
            previewFor = struct;
            preview.setParts(stackOf(struct.warhead()), stackOf(struct.fuselage()), stackOf(struct.fins()),
                    stackOf(struct.thruster()));
        }
        return preview;
    }

    private static ItemStack stackOf(net.minecraft.world.item.@Nullable Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    protected abstract List<Component> sizeHint();

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCustomMissileLauncher launcher = menu.getLauncher();
        if (launcher == null) return;
        if (isHovering(116, 36, 16, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(launcher.tanks[0]), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(134, 36, 16, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(launcher.tanks[1]), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(152, 88 - 52, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("Solid Fuel: " + launcher.solid + "l")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(134, 113, 34, 6, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    GuiUtil.energyTooltip(launcher.power, TileEntityCustomMissileLauncher.maxPower),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(-16, 36, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, sizeHint(), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(-16, 36 + 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("Detonator can only trigger center block.")),
                    Optional.empty(), mouseX, mouseY);
        } else {
            extraTooltip(graphics, mouseX, mouseY);
        }
    }

    protected void extraTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }
}
