package com.hbm.inventory.gui;

import com.hbm.client.render.MissileRenderer;
import com.hbm.entity.missile.EntityMissileTier4;
import com.hbm.inventory.container.ContainerLaunchPadRusted;
import com.hbm.items.ModItems;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRusted;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.GuiEntities;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 1.7 {@code GUILaunchPadRusted}: {@code gui_launch_pad_rusted.png}, 176x236. Lamps for the codes and key and,
 * once both are in and the missile is still loaded, the pad's eight-digit launch code, seeded from its x/z.
 * Clicking the missile slot's release button (26,36) asks the pad to drop the missile into it.
 *
 * <p>The locked-in missile stands in the same bay the launch pad screen uses, fitted from the model's bounds the
 * way {@link GUILaunchPad} does (upstream's 0.875 Atlas scale was that same normalisation).
 */
public class GUILaunchPadRusted extends CompatContainerScreen<ContainerLaunchPadRusted> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/weapon/gui_launch_pad_rusted.png");

    private EntityMissileTier4.@Nullable EntityMissileDoomsdayRusted preview;

    public GUILaunchPadRusted(ContainerLaunchPadRusted menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 236);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 4, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityLaunchPadRusted pad = menu.getPad();
        double x = event.x() - leftPos, y = event.y() - topPos;
        if (pad != null && x >= 26 && x < 42 && y > 36 && y <= 52) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("release", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(pad.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityLaunchPadRusted pad = menu.getPad();
        if (pad == null) return;

        ItemStack codes = pad.getItem(1);
        ItemStack key = pad.getItem(2);
        boolean hasCodes = !codes.isEmpty() && codes.getItem() == ModItems.launch_code.get();
        boolean hasKey = !key.isEmpty() && key.getItem() == ModItems.launch_key.get();

        if (hasCodes) GuiDraw.blit(graphics, TEXTURE, leftPos + 121, topPos + 32, 192F, 0F, 6, 8, 256, 256);
        if (hasKey) GuiDraw.blit(graphics, TEXTURE, leftPos + 139, topPos + 32, 192F, 0F, 6, 8, 256, 256);

        if (hasCodes && hasKey && pad.missileLoaded) {
            BlockPosSeed seed = new BlockPosSeed(pad.getBlockPos().getX(), pad.getBlockPos().getZ());
            int launchCodes = new Random(seed.value()).nextInt(100_000_000);
            for (int i = 0; i < 8; i++) {
                int magnitude = (int) Math.pow(10, i);
                int digit = (launchCodes % (magnitude * 10)) / magnitude;
                GuiDraw.blit(graphics, TEXTURE, leftPos + 109 + 6 * i, topPos + 85, 192F + 6 * digit, 8F, 6, 8, 256, 256);
            }
        }

        if (pad.missileLoaded) drawMissile(graphics);
    }

    /** 1.7 {@code new Random(xCoord * 131_071 + zCoord)}, int arithmetic and all. */
    private record BlockPosSeed(int x, int z) {
        long value() { return x * 131_071 + z; }
    }

    private void drawMissile(GuiGraphicsExtractor graphics) {
        if (this.minecraft == null || this.minecraft.level == null) return;
        if (preview == null) {
            preview = new EntityMissileTier4.EntityMissileDoomsdayRusted(this.minecraft.level, 0F, 0F, 0F, 1, 0);
            preview.setYRot(180F);
            preview.yRotO = 180F;
            if (this.minecraft.player != null) preview.setPos(this.minecraft.player.position());
        }
        Object renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(preview);
        if (!(renderer instanceof MissileRenderer<?> body)) return;
        float modelScale = body.modelScale();
        float low = body.model().boundsMin()[1] * modelScale;
        float high = body.model().boundsMax()[1] * modelScale;
        float size = 100F / Math.max(high - low, 0.001F);
        GuiEntities.draw(graphics, preview, leftPos + 52, topPos + 17, leftPos + 87, topPos + 124, size,
                new Vector3f(0F, (low + high) / 2F, 0F), new Quaternionf().rotateZ((float) Math.PI));
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(26, 36, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("Release Missile").withStyle(ChatFormatting.YELLOW),
                    Component.literal("Missile is locked in lauch position,"),
                    Component.literal("releasing may cause damage to the missile."),
                    Component.literal("Damaged missile can not be put back"),
                    Component.literal("into launching position.")), Optional.empty(), mouseX, mouseY);
        }
    }
}
