package com.hbm.inventory.gui;

import com.hbm.blocks.generic.BlockWandTandem.TileEntityWandTandem;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * 1.7 {@code BlockWandTandem.GuiWandTandem}: the jigsaw screen without name or priorities (target pool, target
 * name, joint type), at 1.7's coordinates; it does not pause the game. Closing it sends the fields.
 */
public final class GUIWandTandem extends CompatScreen {

    private final TileEntityWandTandem jigsaw;

    private EditBox textPool;
    private EditBox textTarget;

    private GUIStructureWandButton jointToggle;

    public GUIWandTandem(TileEntityWandTandem jigsaw) {
        super(Component.translatable("block.hbm.wand_tandem"));
        this.jigsaw = jigsaw;
    }

    @Override
    protected void init() {
        textPool = new EditBox(font, this.width / 2 - 150, 50, 300, 20, Component.empty());
        textPool.setValue(jigsaw.pool);
        addRenderableWidget(textPool);

        textTarget = new EditBox(font, this.width / 2 + 10, 100, 140, 20, Component.empty());
        textTarget.setValue(jigsaw.target);
        addRenderableWidget(textTarget);

        jointToggle = new GUIStructureWandButton(this.width / 2 + 60, 150, 90, 20, jigsaw.isRollable ? "Rollable" : "Aligned");
    }

    /** 1.7 {@code drawDefaultBackground} in a world: the dark gradient over the scene. */
    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.fillGradient(g, 0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.text(g, font, "Target pool:", this.width / 2 - 150, 37, 0xFFA0A0A0, true);
        GuiDraw.text(g, font, "Target name:", this.width / 2 + 10, 87, 0xFFA0A0A0, true);
        GuiDraw.text(g, font, "Joint type:", this.width / 2 + 60, 137, 0xFFA0A0A0, true);
        jointToggle.draw(g, font, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedAfterWidgets(MouseInput event, boolean doubleClick) {
        if (jointToggle.isOver(event.x(), event.y())) {
            GUIStructureWandButton.click();
            jointToggle.label = jointToggle.label.equals("Rollable") ? "Aligned" : "Rollable";
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();
        data.putString("pool", textPool.getValue());
        data.putString("target", textTarget.getValue());
        data.putBoolean("roll", jointToggle.label.equals("Rollable"));

        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(jigsaw.getBlockPos(), data));
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
