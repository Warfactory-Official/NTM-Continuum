package com.hbm.inventory.gui;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.handler.MissileStruct;
import com.hbm.inventory.container.ContainerMachineMissileAssembly;
import com.hbm.tileentity.machine.TileEntityMachineMissileAssembly;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.GuiEntities;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code GUIMachineMissileAssembly}: {@code gui_missile_assembly.png}, 176x222. Lamps over the five part slots
 * at y=23 (green fits; the fin lamp shows red for fins that do not fit and nothing for no fins), the build button
 * at (115,35) lit when the missile can be built, and the parts so far lying across the bay, turning.
 *
 * <p>The bay is 1.7's chain {@code T(88,98) Ry(-t/10) T(h/2*s) S(s) Rx(90) Rz(-90) S(-1)} with
 * {@code s = 144 / max(h, 6)}, rewritten into {@link GuiEntities}' z-flipped frame as
 * {@code T(Ry(t/10)(h/2, 0, 0)) Ry(t/10) Rz(180) Rx(90) Rz(-90)} at scale {@code s}; see
 * {@link GUICustomMissileLauncher} for why an entity carries the draw.
 */
public class GUIMachineMissileAssembly extends CompatContainerScreen<ContainerMachineMissileAssembly> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_missile_assembly.png");

    private @Nullable EntityMissileCustom preview;
    private @Nullable MissileStruct previewFor;

    public GUIMachineMissileAssembly(ContainerMachineMissileAssembly menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft minecraft = Minecraft.getInstance();
        double x = event.x() - leftPos, y = event.y() - topPos;
        // 1.7: x in [115, 133), y in (35, 53]
        if (minecraft.gameMode != null && x >= 115 && x < 133 && y > 35 && y <= 53) {
            GuiUtil.playClickSound();
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineMissileAssembly assembler = menu.getAssembly();
        if (assembler == null) return;

        if (assembler.fuselageState() == 1) lamp(graphics, 49, 194F);
        if (assembler.warheadState() == 1) lamp(graphics, 31, 194F);
        if (assembler.chipState() == 1) lamp(graphics, 13, 194F);
        if (assembler.stabilityState() == 1) lamp(graphics, 67, 194F);
        if (assembler.stabilityState() == 0) lamp(graphics, 67, 200F);
        if (assembler.thrusterState() == 1) lamp(graphics, 85, 194F);

        if (assembler.canBuild()) GuiDraw.blit(graphics, TEXTURE, leftPos + 115, topPos + 35, 176F, 0F, 18, 18, 256, 256);

        drawMissile(graphics, assembler.currentStruct());
    }

    private void lamp(GuiGraphicsExtractor graphics, int x, float u) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + 23, u, 0F, 6, 8, 256, 256);
    }

    private void drawMissile(GuiGraphicsExtractor graphics, MissileStruct struct) {
        if (this.minecraft == null || this.minecraft.level == null) return;
        if (struct.warhead() == null && struct.fuselage() == null && struct.fins() == null && struct.thruster() == null) return;
        if (preview == null) {
            preview = new EntityMissileCustom(ModNtmEntities.MISSILE_CUSTOM.get(), this.minecraft.level);
            preview.setYRot(90F);
            preview.yRotO = 90F;
            if (this.minecraft.player != null) preview.setPos(this.minecraft.player.position());
        }
        if (!struct.equals(previewFor)) {
            previewFor = struct;
            preview.setParts(stackOf(struct.warhead()), stackOf(struct.fuselage()), stackOf(struct.fins()),
                    stackOf(struct.thruster()));
        }

        float h = struct.height();
        float scale = 144F / Math.max(h, 6F);
        float spin = (float) Math.toRadians(System.currentTimeMillis() / 10 % 360);
        Vector3f translation = new Vector3f(h / 2F, 0F, 0F).rotateY(spin);
        Quaternionf rotation = new Quaternionf().rotateY(spin).rotateZ((float) Math.PI)
                .rotateX((float) Math.toRadians(90)).rotateZ((float) Math.toRadians(-90));
        GuiEntities.draw(graphics, preview, leftPos + 8, topPos + 50, leftPos + 168, topPos + 146, scale,
                translation, rotation);
    }

    private static ItemStack stackOf(@Nullable Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
