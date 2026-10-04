package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidIDMulti;
import com.hbm.packet.ItemControlPayload;
import com.hbm.tileentity.IFluidCopiable;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code GUIScreenFluid}, the multi identifier's picker: a search line and nine result slots (each a
 * colour swatch); left click sets the primary fluid, right click the secondary, both sent as an item control
 * tag. {@code gui_fluid.png}, 176x54.
 */
public class GUIScreenFluid extends CompatScreen {

    protected static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_fluid.png");
    protected int xSize = 176;
    protected int ySize = 54;
    protected int guiLeft;
    protected int guiTop;
    private EditBox search;

    private FluidType primary = Fluids.NONE;
    private FluidType secondary = Fluids.NONE;
    private FluidType[] searchArray = new FluidType[9];

    public GUIScreenFluid() {
        super(Component.empty());
    }

    private static boolean holding() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getMainHandItem().getItem() instanceof ItemFluidIDMulti;
    }

    @Override
    public void tick() {
        super.tick();
        if (!holding()) Minecraft.getInstance().setScreen(null);
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.search = new EditBox(font, guiLeft + 46, guiTop + 11, 86, 12, Component.empty());
        this.search.setTextColor(0xFFFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFFFF);
        this.search.setBordered(false);
        this.search.setResponder(text -> updateSearch());
        addRenderableWidget(search);
        setFocused(search);
        focusedEditBoxForInput = search;

        if (holding()) {
            ItemStack held = Minecraft.getInstance().player.getMainHandItem();
            this.primary = ItemFluidIDMulti.getType(held, true);
            this.secondary = ItemFluidIDMulti.getType(held, false);
        }
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        double i = event.x(), j = event.y();
        for (int k = 0; k < this.searchArray.length; k++) {
            if (this.searchArray[k] == null) return false;

            if (guiLeft + 7 + k * 18 <= i && guiLeft + 7 + k * 18 + 18 > i && guiTop + 29 < j && guiTop + 29 + 18 >= j) {
                CompoundTag data = new CompoundTag();
                if (event.button() == 0) {
                    click();
                    this.primary = this.searchArray[k];
                    Fluids.write(data, "primary", this.primary);
                } else if (event.button() == 1) {
                    click();
                    this.secondary = this.searchArray[k];
                    Fluids.write(data, "secondary", this.secondary);
                } else {
                    return false;
                }
                GolemNetwork.sendToServer(ItemControlPayload.CODEC, new ItemControlPayload(data));
                return true;
            }
        }
        return false;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int i, int j, float partialTick) {
        for (int k = 0; k < this.searchArray.length; k++) {
            if (this.searchArray[k] == null) return;

            if (guiLeft + 7 + k * 18 <= i && guiLeft + 7 + k * 18 + 18 > i && guiTop + 29 < j && guiTop + 29 + 18 >= j) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(this.searchArray[k].getDisplayName());
                this.searchArray[k].addInfo(tooltip);
                GuiDraw.tooltip(graphics, font, tooltip, Optional.empty(), i, j);
            }
        }
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int i, int j, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0F, 0F, xSize, ySize, 256, 256);

        if (this.search.isFocused())
            GuiDraw.blit(graphics, TEXTURE, guiLeft + 43, guiTop + 7, 166F, 54F, 90, 18, 256, 256);

        for (int k = 0; k < this.searchArray.length; k++) {
            FluidType type = this.searchArray[k];
            if (type == null) return;

            GuiDraw.blit(graphics, TEXTURE, guiLeft + 12 + k * 18, guiTop + 31, 12F + k * 18, 56F, 8, 14, 256, 256,
                    0xFF000000 | (type.getColor() & 0xFFFFFF));

            if (type == this.primary && type == this.secondary) {
                GuiDraw.blit(graphics, TEXTURE, guiLeft + 7 + k * 18, guiTop + 29, 176F, 36F, 18, 18, 256, 256);
            } else if (type == this.primary) {
                GuiDraw.blit(graphics, TEXTURE, guiLeft + 7 + k * 18, guiTop + 29, 176F, 0F, 18, 18, 256, 256);
            } else if (type == this.secondary) {
                GuiDraw.blit(graphics, TEXTURE, guiLeft + 7 + k * 18, guiTop + 29, 176F, 18F, 18, 18, 256, 256);
            }
        }
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        if (event.key() == 256) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateSearch() {
        this.searchArray = new FluidType[9];
        int next = 0;
        String subs = this.search.getValue().toLowerCase(Locale.US);

        for (FluidType type : Fluids.getAll()) {
            String name = type.getDisplayNameString().toLowerCase(Locale.US);
            if (name.contains(subs) && !IFluidCopiable.ItemFluidIds.hasNoID(type)) {
                this.searchArray[next] = type;
                next++;
                if (next >= 9) return;
            }
        }
    }
}
