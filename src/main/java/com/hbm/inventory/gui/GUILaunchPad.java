package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerLaunchPad;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import com.hbm.client.render.MissileRenderer;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.GuiEntities;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Screen for the launch pad — 1.12.2's {@code GUILaunchPadLarge} ({@code gui_launch_pad_large.png},
 * 176×236).
 *
 * <p>The three vertical bars are power (107,36) and the two fuel tanks (125,36) and (143,36), each
 * 16×52 and filling upward to y=88. Above each sits a small green/red lamp at y=23 — lit green when
 * that resource is sufficient for the loaded missile, red when it is not, and dark when the missile
 * does not need it (a solid-fuel missile needs neither tank). The status line under the missile slot
 * reads "Not ready" / "Loading..." / "Ready" at three different scales, which is upstream's way of
 * making readiness legible at a glance.
 *
 * <p>The empty designator slot cycles through the designators that fit it, once a second, with the
 * name of whichever is showing. That hint is upstream's, and it is the only thing in the game that
 * tells a player a pad needs a designator before it will fire — a pad can sit at "Ready" (which only
 * means missile + fuel + power) and still refuse to launch without one.
 *
 * <p>The missile itself stands in the bay — the black window in the panel texture — in 3D. Upstream draws it from its item
 * renderer table with a hand-tuned scale per airframe; here it is the missile's own flying entity,
 * drawn by the same renderer that draws it in the sky, and the scale is derived from the model's
 * bounds instead — upstream's constants were calibrated against 1.12's item-render transform, which
 * has no equivalent here, and the bounds give the same result without the calibration.
 */
public class GUILaunchPad extends dev.golem.core.gui.CompatContainerScreen<ContainerLaunchPad> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/weapon/gui_launch_pad_large.png");

    /**
     * The missile bay: the black window in the panel texture, measured from it — x 52–86, y 17–123.
     *
     * <p>Not derived from upstream's {@code translate(guiLeft + 70, guiTop + 120)}, which is the
     * missile's <b>tail</b>, not the centre of anything: upstream then scales by −8, so the body grows
     * <em>upward</em> from that point to fill the bay. Reading 120 as a centre drops the missile half
     * its own length out of the panel.
     */
    private static final int PREVIEW_X0 = 52;
    private static final int PREVIEW_X1 = 87;
    private static final int PREVIEW_Y0 = 17;
    private static final int PREVIEW_Y1 = 124;

    /**
     * Drawn height of the body, in GUI pixels, whatever the airframe.
     *
     * <p>This is upstream's number, recovered rather than copied. Its per-airframe constants
     * ({@code V2 1.75}, {@code STRONG 1.375}, {@code HUGE 0.925}, {@code ATLAS 0.875}, ABM
     * {@code 1.45}, stealth {@code 1.125}, each times a base 8) look arbitrary until they are
     * multiplied out against the bodies they scale: they come to 98, 99, 99.9, 101.1, 98.6 and 99.2
     * pixels. They are a normalisation to this bay, so the bay is what this normalises to — which
     * needs no table and cannot go stale when a model changes.
     */
    private static final float PREVIEW_HEIGHT = 100F;

    /** The preview's entity, rebuilt only when the loaded missile changes. */
    private ItemStack previewFor = ItemStack.EMPTY;
    private net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity previewEntity;

    public GUILaunchPad(ContainerLaunchPad menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 236);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityLaunchPadBase pad = this.menu.getPad();
        if (pad == null) return;

        // The three lamps: u=192 is the green one, u=198 the red one.
        drawLamp(graphics, 130, pad.getFuelState());
        drawLamp(graphics, 148, pad.getOxidizerState());
        if (pad.isMissileValid()) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 112, this.topPos + 23,
                    pad.power >= TileEntityLaunchPadBase.launchCost ? 192.0F : 198.0F, 0.0F, 6, 8, 256, 256);
        }

        int power = (int) (pad.power * 52 / TileEntityLaunchPadBase.maxPower);
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 107, this.topPos + 88 - power,
                    176.0F, (float) (52 - power), 16, power, 256, 256);
        }

        drawTank(graphics, pad.tanks[0], 125, 36);
        drawTank(graphics, pad.tanks[1], 143, 36);

        drawMissile(graphics, this.menu.getSlot(0).getItem());
        drawStatus(graphics, pad);
        drawDesignatorHint(graphics);
    }

    private void drawLamp(GuiGraphicsExtractor graphics, int x, int gaugeState) {
        if (gaugeState == 0) return;
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + x, this.topPos + 23,
                gaugeState == 1 ? 192.0F : 198.0F, 0.0F, 6, 8, 256, 256);
    }

    private void drawStatus(GuiGraphicsExtractor graphics, TileEntityLaunchPadBase pad) {
        String text;
        int colour;
        switch (pad.state) {
            // Alpha is explicit: a modern ARGB colour of 0x00ff00 has alpha 0 and draws nothing.
            case TileEntityLaunchPadBase.STATE_LOADING -> { text = "Loading..."; colour = 0xFFFF8000; }
            case TileEntityLaunchPadBase.STATE_READY -> { text = "Ready"; colour = 0xFF00FF00; }
            default -> { text = "Not ready"; colour = 0xFFFF0000; }
        }
        // The readier the pad, the bigger the word: upstream makes the scale part of the signal.
        float scale = switch (pad.state) {
            case TileEntityLaunchPadBase.STATE_LOADING -> 0.6F;
            case TileEntityLaunchPadBase.STATE_READY -> 0.8F;
            default -> 0.5F;
        };

        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, this.leftPos + 34, this.topPos + 107);
        GuiDraw.scale(graphics, scale, scale);
        GuiDraw.text(graphics, this.font, text,
                -this.font.width(text) / 2, -this.font.lineHeight / 2, colour, false);
        GuiDraw.popMatrix(graphics);
    }

    /**
     * The missile standing in the panel. The entity is a throwaway from the launch table, never added
     * to a level and cached until the slot changes — 26.1 draws 3D in a screen only through an entity
     * render state, see {@code GuiEntities}.
     */
    private void drawMissile(GuiGraphicsExtractor graphics, ItemStack stack) {
        if (stack.isEmpty()) {
            this.previewFor = ItemStack.EMPTY;
            this.previewEntity = null;
            return;
        }

        if (!ItemStack.isSameItem(stack, this.previewFor)) {
            this.previewFor = stack.copy();
            // Target one block away, not zero: the launch constructor divides by the distance.
            this.previewEntity = TileEntityLaunchPadBase.createMissileEntity(
                    this.minecraft.level, stack.getItem(), 0F, 0F, 0F, 1, 0);
            if (this.previewEntity != null) {
                // The renderer turns the body by yaw − 90°, so 180° is upstream's three-quarter view.
                this.previewEntity.setYRot(180F);
                this.previewEntity.yRotO = 180F;
                // Stand it where the player is. It is never added to the level, but the render state
                // is extracted from it, and that reads the world around wherever it thinks it is.
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.previewEntity.setPos(this.minecraft.player.position());
                }
            }
        }

        if (this.previewEntity == null) return;

        // Ask the renderer that is about to draw it, rather than a parallel table: MissileModels is the
        // item/pad table and its scales differ from the entity ones, which is how the first version of
        // this put an Atlas warhead through the top of the panel at 150% size.
        Object renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(this.previewEntity);
        if (!(renderer instanceof MissileRenderer<?> body)) return;

        // Fit the body to the box from its own bounds. The model's Y is its long axis and its origin
        // is the tail: the renderer only turns it about Y (yaw) and rolls it about Z by the pitch,
        // which is zero here, so Y stays vertical.
        float modelScale = body.modelScale();
        float low = body.model().boundsMin()[1] * modelScale;
        float high = body.model().boundsMax()[1] * modelScale;
        float height = Math.max(high - low, 0.001F);
        float size = PREVIEW_HEIGHT / height;

        // rotateZ(PI) is vanilla's GUI convention: the screen's Y runs down, so the model is turned
        // over to stand up. The translation is applied before it and so is in screen space, which is
        // why centring the body means shifting by +its own midpoint.
        GuiEntities.draw(graphics, this.previewEntity,
                this.leftPos + PREVIEW_X0, this.topPos + PREVIEW_Y0,
                this.leftPos + PREVIEW_X1, this.topPos + PREVIEW_Y1,
                size,
                new org.joml.Vector3f(0F, (low + high) / 2F, 0F),
                new org.joml.Quaternionf().rotateZ((float) Math.PI));
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * 52 / maxF);
        if (h <= 0) return;

        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + 52;
        int yTop = yBottom - h;
        Identifier fluidTex = tank.getTankType().getTexture();

        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + 16, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, 16, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + 16, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    /** The designators that fit slot 1, cycled one per second while the slot is empty. */
    private static final java.util.List<net.minecraft.world.item.Item> DESIGNATOR_HINTS = java.util.List.of(
            com.hbm.items.ModMissileItems.designator.get(),
            com.hbm.items.ModMissileItems.designator_range.get());

    /** Draws the cycling designator hint over the empty designator slot. */
    private void drawDesignatorHint(GuiGraphicsExtractor graphics) {
        if (this.menu.getSlot(1).hasItem()) return;

        // System time, not a tick counter: the slot must keep cycling while the game is paused, which is
        // exactly when a player is reading it. One entry per second, as upstream.
        int index = (int) ((System.currentTimeMillis() / 1000L) % DESIGNATOR_HINTS.size());
        ItemStack hint = new ItemStack(DESIGNATOR_HINTS.get(index));
        GuiDraw.item(graphics, hint, this.leftPos + 26, this.topPos + 72);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityLaunchPadBase pad = this.menu.getPad();
        if (pad == null) return;

        // Name the designator being hinted at, so the cycling ghost is readable rather than a mystery.
        if (!this.menu.getSlot(1).hasItem() && this.isHovering(26, 72, 16, 16, mouseX, mouseY)) {
            int index = (int) ((System.currentTimeMillis() / 1000L) % DESIGNATOR_HINTS.size());
            GuiDraw.tooltip(graphics, this.font, List.of(
                            new ItemStack(DESIGNATOR_HINTS.get(index)).getHoverName(),
                            Component.translatable("desc.padneedsdesignator")),
                    Optional.empty(), mouseX, mouseY);
            return;
        }

        if (this.isHovering(107, 36, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(pad.power, pad.getMaxPower()),
                    Optional.empty(), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < 2; i++) {
            if (this.isHovering(125 + i * 18, 36, 16, 52, mouseX, mouseY)) {
                FluidTankNTM tank = pad.tanks[i];
                GuiDraw.tooltip(graphics, this.font, List.of(
                        tank.getTankType().getDisplayName(),
                        Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }
}
