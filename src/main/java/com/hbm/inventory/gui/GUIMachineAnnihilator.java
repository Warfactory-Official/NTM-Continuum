package com.hbm.inventory.gui;

import com.hbm.items.machine.IItemFluidId;
import com.hbm.packet.ControlEventPayload;
import com.hbm.inventory.container.ContainerMachineAnnihilator;
import com.hbm.tileentity.machine.TileEntityMachineAnnihilator;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** 1.7 {@code GUIMachineAnnihilator}: {@code gui_annihilator.png}, 176x208, with the green pool-name field. */
public final class GUIMachineAnnihilator extends CompatContainerScreen<ContainerMachineAnnihilator> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_annihilator.png");

    private EditBox pool;

    public GUIMachineAnnihilator(ContainerMachineAnnihilator menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 208);
    }

    @Override
    protected void init() {
        super.init();
        TileEntityMachineAnnihilator annihilator = menu.getAnnihilator();

        // 1.7: GuiTextField(31, 85, 80 wide, 8 high), green text, no background, max 20.
        pool = new EditBox(font, leftPos + 31, topPos + 85, 80, 12, Component.empty());
        pool.setBordered(false);
        pool.setTextColor(0xFF00FF00);
        pool.setTextColorUneditable(0xFF00FF00);
        pool.setMaxLength(20);
        pool.setValue(annihilator == null ? "" : String.valueOf(annihilator.pool));
        // 1.7 sent the pool name on every accepted key.
        pool.setResponder(text -> {
            if (annihilator == null) return;
            CompoundTag data = new CompoundTag();
            data.putString("pool", text);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(annihilator.getBlockPos(), data));
        });
        addRenderableWidget(pool);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 70 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineAnnihilator annihilator = menu.getAnnihilator();
        if (annihilator == null) return;

        ItemStack monitored = annihilator.inventory.getStackInSlot(8);
        if (!monitored.isEmpty() && isHovering(151, 35, 18, 18, mouseX, mouseY)) {
            Component name = monitored.getHoverName();
            if (monitored.getItem() instanceof IItemFluidId id) {
                name = id.getType(null, 0, 0, 0, monitored).getDisplayName();
            }
            GuiDraw.tooltip(graphics, font, List.of(name.copy().append(":"),
                    Component.literal(String.format(Locale.US, "%,d", annihilator.monitorBigInt))), Optional.empty(), mouseX, mouseY);
        }
    }
}
