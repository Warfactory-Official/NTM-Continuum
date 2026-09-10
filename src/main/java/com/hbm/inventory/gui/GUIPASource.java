package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerPASource;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.albion.TileEntityPASource;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Particle source panel. The coloured bar across the middle is the run's {@link
 * TileEntityPASource.PAState}: blue while idle, yellow while a beam is in flight, green on a match, and
 * red for whichever crash ended it. Hovering the readout explains that state; the button beside it
 * abandons the run.
 */
public class GUIPASource extends GUIPABase<ContainerPASource> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/particleaccelerator/gui_source.png");

    public GUIPASource(ContainerPASource menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected @Nullable TileEntityCooledBase machine() {
        return menu.getSource();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityPASource source = menu.getSource();
        if (source == null) return;

        drawPowerBar(graphics, x + 8, y + 18, source.power, source.getMaxPower());

        if (Math.ceil(source.temperature) <= COOL_ENOUGH) drawLamp(graphics, x + 44, y + 18);
        if (source.power >= TileEntityPASource.usage) drawLamp(graphics, x + 44, y + 43);

        GuiDraw.blit(graphics, TEXTURE, x + 45, y + 73, 176F, 52F, 68, 14, 256, 256,
                0xFF000000 | source.state.color);

        drawTank(graphics, source.coolantTanks[0], x + 134, y + 36);
        drawTank(graphics, source.coolantTanks[1], x + 152, y + 36);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics, 4);
        drawTemperature(graphics, 136, 22, 166, 12);

        TileEntityPASource source = menu.getSource();
        if (source == null) return;

        Component state = Component.translatable(stateKey(source));
        GuiDraw.text(graphics, font, state, 79 - font.width(state) / 2, 76,
                0xFF000000 | source.state.color, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (commonTooltip(graphics, mouseX, mouseY, 8, 18, 134, 36)) return;

        TileEntityPASource source = menu.getSource();
        if (source == null) return;

        if (isHovering(105, 18, 10, 10, mouseX, mouseY)) {
            List<Component> info = new ArrayList<>();
            info.add(Component.literal("Last momentum: ").withStyle(ChatFormatting.BLUE)
                    .append(Component.literal(comma(source.lastSpeed)).withStyle(ChatFormatting.RESET)));
            String base = stateKey(source) + ".desc.";
            Language language = Language.getInstance();
            for (int i = 0; language.has(base + i); i++) {
                info.add(Component.translatable(base + i).withStyle(ChatFormatting.YELLOW));
            }
            tooltip(graphics, info, mouseX, mouseY);
        } else if (isHovering(105, 30, 10, 10, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Cancel operation").withStyle(ChatFormatting.RED)),
                    mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityPASource source = menu.getSource();
        if (source == null) return false;

        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        if (x >= 105 && x < 115 && y >= 30 && y < 40) {
            GuiUtil.playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("cancel", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(source.getBlockPos(), data));
            return true;
        }
        return false;
    }

    private static String stateKey(TileEntityPASource source) {
        return "pa." + source.state.name().toLowerCase(Locale.US);
    }
}
