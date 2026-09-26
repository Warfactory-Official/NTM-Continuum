package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;
import com.hbm.inventory.container.ContainerCraneRouter;
import com.hbm.tileentity.network.TileEntityCraneRouter;
import com.hbm.modules.ModulePatternMatcher;

/** 1.7 {@code GUICraneRouter} ("Conveyor Sorter"): 256x201, drawn as the 256x93 top and a 176x108 player panel offset by 39. */
public class GUICraneRouter extends CompatContainerScreen<ContainerCraneRouter> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_crane_router.png");

    public GUICraneRouter(ContainerCraneRouter menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 201);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, 256, 93, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 39, topPos + 93, 39F, 93F, 176, 108, 256, 256);

        TileEntityCraneRouter router = menu.getTile();
        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                int index = j * 3 + k;
                int mode = router == null ? 0 : router.modes[index];
                GuiDraw.blit(graphics, TEXTURE, leftPos + 7 + j * 222, topPos + 16 + k * 26, 238F, 93F + mode * 18F, 18, 18, 256, 256);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 5, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8 + 39, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCraneRouter router = menu.getTile();
        if (router == null) return;

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                if (legacyHover(7 + j * 222, 16 + k * 26, 18, 18, mouseX, mouseY)) {
                    int index = j * 3 + k;
                    List<Component> text = switch (router.modes[index]) {
                        case 1 -> List.of(Component.literal("WHITELIST"), Component.literal("Route if filter matches"));
                        case 2 -> List.of(Component.literal("BLACKLIST"), Component.literal("Route if filter doesn't match"));
                        case 3 -> List.of(Component.literal("WILDCARD"), Component.literal("Route if no other route is valid"));
                        default -> List.of(Component.literal("OFF"));
                    };
                    tooltip(graphics, text, mouseX, mouseY);
                    return;
                }
            }
        }

        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < TileEntityCraneRouter.SLOT_COUNT; i++) {
                Slot slot = menu.slots.get(i);
                ModulePatternMatcher matcher = router.patterns[i / 5];
                int index = i % 5;
                if (overSlot(slot, mouseX, mouseY) && matcher.modes[index] != null) {
                    tooltip(graphics, List.of(Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                            Component.literal(ModulePatternMatcher.getLabel(matcher.modes[index]))), mouseX, mouseY - 30);
                    return;
                }
            }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityCraneRouter router = menu.getTile();
        if (router == null) return false;

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                if (legacyHover(7 + j * 222, 16 + k * 26, 18, 18, event.x(), event.y())) {
                    CompoundTag data = new CompoundTag();
                    data.putInt("toggle", j * 3 + k);
                    click(router.getBlockPos(), data);
                    return true;
                }
            }
        }
        return false;
    }

    /** The old GUI used a closed lower bound and an open upper bound on x, the reverse on y. */
    private boolean legacyHover(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    private void click(BlockPos pos, CompoundTag data) {
        Gauges.click();
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(pos, data));
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }

    private static Component onOff(boolean on) {
        return on ? Component.literal("ON").withStyle(ChatFormatting.GREEN) : Component.literal("OFF").withStyle(ChatFormatting.RED);
    }

    /** 1.7 {@code isMouseOverSlot}. */
    private boolean overSlot(Slot slot, double mouseX, double mouseY) {
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }
}
