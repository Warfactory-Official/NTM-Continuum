package com.hbm.inventory.gui;

import com.hbm.Constants;
import com.hbm.client.render.RenderInfoSystem;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.IBaseAbility;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.packet.PlayerInformPayload;
import com.hbm.packet.ToolAbilityControlPayload;
import com.hbm.sound.ModSounds;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code GUIScreenToolAbility}: the preset editor opened with the ability-alt key. One row of area
 * switches, one row of harvest switches (with level LEDs), the preset counter, and seven small buttons:
 * reset, delete, add, first, next, previous, close. The texture, coordinates and behaviour are the
 * original's; scrolling steps through presets, clicking outside closes, and closing sends the edited
 * configuration to the server.
 */
public class GUIScreenToolAbility extends CompatScreen {

    public static final Identifier texture = Ids.of(Constants.MOD_ID, "textures/gui/tool/gui_tool_ability.png");

    protected int guiLeft;
    protected int guiTop;
    protected int xSize;
    protected int ySize;
    protected int insetWidth;

    /**
     * One switch of the ability GUI: the ability and its 16x16 icon at (u, v) (the lit icon 16 px right of it). HBM's
     * icons are on {@link #texture}; an addon ability brings its own sheet (256x256) and adds an {@code AbilityInfo}
     * to {@link #abilitiesArea} or {@link #abilitiesHarvest} from its client start-up.
     */
    public static class AbilityInfo {
        public IBaseAbility ability;
        public int textureU, textureV;
        public Identifier sheet;

        public AbilityInfo(IBaseAbility ability, int textureU, int textureV) {
            this(ability, texture, textureU, textureV);
        }

        public AbilityInfo(IBaseAbility ability, Identifier sheet, int textureU, int textureV) {
            this.ability = ability;
            this.sheet = sheet;
            this.textureU = textureU;
            this.textureV = textureV;
        }
    }

    public static final List<AbilityInfo> abilitiesArea = new ArrayList<>();
    public static final List<AbilityInfo> abilitiesHarvest = new ArrayList<>();

    static {
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.NONE, 0, 91));
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.RECURSION, 32, 91));
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.HAMMER, 64, 91));
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.HAMMER_FLAT, 96, 91));
        abilitiesArea.add(new AbilityInfo(IToolAreaAbility.EXPLOSION, 128, 91));

        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.NONE, 0, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.SILK, 32, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.LUCK, 64, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.SMELTER, 96, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.SHREDDER, 128, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.CENTRIFUGE, 160, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.CRYSTALLIZER, 192, 107));
        abilitiesHarvest.add(new AbilityInfo(IToolHarvestAbility.MERCURY, 224, 107));
    }

    protected ItemStack toolStack;
    protected AvailableAbilities availableAbilities;
    protected ItemToolAbility.Configuration config;

    protected int hoverIdxHarvest = -1;
    protected int hoverIdxArea = -1;
    protected int hoverIdxExtraBtn = -1;

    public GUIScreenToolAbility(AvailableAbilities availableAbilities) {
        super(Component.literal("Tool Abilities"));

        this.availableAbilities = availableAbilities;

        this.xSize = 186; // Note: increased dynamically
        this.ySize = 76;

        this.insetWidth = 20 * Math.max(abilitiesArea.size() - 4, abilitiesHarvest.size() - 8);
        this.xSize += insetWidth;
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        this.toolStack = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();

        if (!(this.toolStack.getItem() instanceof ItemToolAbility tool)) {
            mc.setScreen(null);
            return;
        }

        this.config = tool.getConfiguration(this.toolStack);

        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (config == null) return;

        // Draw window background
        drawStretchedRect(graphics, guiLeft, guiTop, 0, 0, xSize, xSize - insetWidth, ySize, 74, 87);

        // Draw the switches
        ToolPreset activePreset = config.getActivePreset();
        hoverIdxArea = drawSwitches(graphics, abilitiesArea, activePreset.areaAbility, activePreset.areaAbilityLevel,
                guiLeft + 15, guiTop + 25, mouseX, mouseY);
        hoverIdxHarvest = drawSwitches(graphics, abilitiesHarvest, activePreset.harvestAbility,
                activePreset.harvestAbilityLevel, guiLeft + 15, guiTop + 45, mouseX, mouseY);

        // Draw preset indicator
        drawNumber(graphics, config.currentPreset + 1, guiLeft + insetWidth + 115, guiTop + 25);
        drawNumber(graphics, config.presets.size(), guiLeft + insetWidth + 149, guiTop + 25);

        // Draw extra buttons hover highlights
        int extraBtnsX = guiLeft + xSize - 86;

        hoverIdxExtraBtn = -1;
        for (int i = 0; i < 7; ++i) {
            if (isInAABB(mouseX, mouseY, extraBtnsX + i * 11, guiTop + 11, 9, 9)) {
                hoverIdxExtraBtn = i;
                GuiDraw.blit(graphics, texture, extraBtnsX + i * 11, guiTop + 11, 193 + i * 9, 0, 9, 9, 256, 256);
            }
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (config == null) return;
        ToolPreset activePreset = config.getActivePreset();

        // Draw tooltip
        Component tooltipValue = null;

        if (hoverIdxArea != -1) {
            int level = 0;
            if (abilitiesArea.get(hoverIdxArea).ability == activePreset.areaAbility) level = activePreset.areaAbilityLevel;
            tooltipValue = abilitiesArea.get(hoverIdxArea).ability.getFullName(level);
        } else if (hoverIdxHarvest != -1) {
            int level = 0;
            if (abilitiesHarvest.get(hoverIdxHarvest).ability == activePreset.harvestAbility) level = activePreset.harvestAbilityLevel;
            tooltipValue = abilitiesHarvest.get(hoverIdxHarvest).ability.getFullName(level);
        } else if (hoverIdxExtraBtn != -1) {
            tooltipValue = Component.literal(switch (hoverIdxExtraBtn) {
                case 0 -> "Reset all presets";
                case 1 -> "Delete current preset";
                case 2 -> "Add new preset";
                case 3 -> "Select first preset";
                case 4 -> "Next preset";
                case 5 -> "Previous preset";
                default -> "Close window";
            });
        }

        if (tooltipValue != null && !tooltipValue.getString().isEmpty()) {
            int tooltipWidth = Math.max(6, font.width(tooltipValue));
            int tooltipX = guiLeft + xSize / 2 - tooltipWidth / 2;
            int tooltipY = guiTop + ySize + 1 + 4;
            drawStretchedRect(graphics, tooltipX - 5, tooltipY - 4, 0, 76, tooltipWidth + 10, 186, 15, 3, 3);
            GuiDraw.text(graphics, font, tooltipValue, tooltipX, tooltipY, 0xffffffff, false);
        }
    }

    protected void drawStretchedRect(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int realWidth, int width,
                                     int height, int keepLeft, int keepRight) {
        int midWidth = width - keepLeft - keepRight;
        int realMidWidth = realWidth - keepLeft - keepRight;
        GuiDraw.blit(graphics, texture, x, y, u, v, keepLeft, height, 256, 256);
        for (int i = 0; i < realMidWidth; i += midWidth) {
            GuiDraw.blit(graphics, texture, x + keepLeft + i, y, u + keepLeft, v, Math.min(midWidth, realMidWidth - i),
                    height, 256, 256);
        }
        GuiDraw.blit(graphics, texture, x + keepLeft + realMidWidth, y, u + keepLeft + midWidth, v, keepRight, height, 256, 256);
    }

    protected int drawSwitches(GuiGraphicsExtractor graphics, List<AbilityInfo> abilities, IBaseAbility selectedAbility,
                               int selectedLevel, int x, int y, int mouseX, int mouseY) {
        int hoverIdx = -1;

        for (int i = 0; i < abilities.size(); ++i) {
            AbilityInfo abilityInfo = abilities.get(i);
            boolean available = abilityAvailable(abilityInfo.ability);
            boolean selected = abilityInfo.ability == selectedAbility;

            // Draw switch
            GuiDraw.blit(graphics, abilityInfo.sheet, x + 20 * i, y, abilityInfo.textureU + (available ? 16 : 0),
                    abilityInfo.textureV, 16, 16, 256, 256);

            // Draw level LEDs
            if (abilityInfo.ability.levels() > 1) {
                int level = 0;
                if (selected) level = selectedLevel + 1;

                // Note: only visual effect for the LEDs
                int maxLevel = 5;

                if (level > 10 || level < 0) {
                    // All-red LEDs for invalid levels
                    level = -1;
                }

                GuiDraw.blit(graphics, texture, x + 20 * i + 17, y + 1, 188 + level * 2, maxLevel * 14, 2, 14, 256, 256);
            }

            boolean isHovered = isInAABB(mouseX, mouseY, x + 20 * i, y, 16, 16);
            if (isHovered) hoverIdx = i;

            if (selected) {
                // Draw selection highlight
                GuiDraw.blit(graphics, texture, x + 20 * i - 1, y - 1, 220, 9, 18, 18, 256, 256);
            } else if (available && isHovered) {
                // Draw hover highlight
                GuiDraw.blit(graphics, texture, x + 20 * i - 1, y - 1, 238, 9, 18, 18, 256, 256);
            }
        }

        return hoverIdx;
    }

    protected void drawNumber(GuiGraphicsExtractor graphics, int number, int x, int y) {
        number += 100; // Against accidental negatives
        drawDigit(graphics, (number / 10) % 10, x, y);
        drawDigit(graphics, number % 10, x + 12, y);
    }

    protected void drawDigit(GuiGraphicsExtractor graphics, int digit, int x, int y) {
        GuiDraw.blit(graphics, texture, x, y, digit * 10, 123, 10, 15, 256, 256);
    }

    private boolean isInAABB(double mouseX, double mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }

    private boolean abilityAvailable(IBaseAbility ability) {
        if (!availableAbilities.supportsAbility(ability)) return false;

        ToolPreset activePreset = config.getActivePreset();
        return !(ability instanceof IToolHarvestAbility) || ability == IToolHarvestAbility.NONE
                || activePreset.areaAbility.allowsHarvest(activePreset.areaAbilityLevel);
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (config == null) return false;
        if (scrollY < 0) doPrevPreset(true);
        if (scrollY > 0) doNextPreset(true);
        return true;
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        if (config == null) return false;
        double mouseX = event.x();
        double mouseY = event.y();
        ToolPreset activePreset = config.getActivePreset();

        // Process switches
        Object[] clickResult;

        clickResult = handleSwitchesClicked(abilitiesArea, activePreset.areaAbility, activePreset.areaAbilityLevel, hoverIdxArea);
        activePreset.areaAbility = (IToolAreaAbility) clickResult[0];
        activePreset.areaAbilityLevel = (Integer) clickResult[1];

        clickResult = handleSwitchesClicked(abilitiesHarvest, activePreset.harvestAbility, activePreset.harvestAbilityLevel, hoverIdxHarvest);
        activePreset.harvestAbility = (IToolHarvestAbility) clickResult[0];
        activePreset.harvestAbilityLevel = (Integer) clickResult[1];

        if (!activePreset.areaAbility.allowsHarvest(activePreset.areaAbilityLevel)) {
            activePreset.harvestAbility = IToolHarvestAbility.NONE;
            activePreset.harvestAbilityLevel = 0;
        }

        // Process extra buttons
        if (hoverIdxExtraBtn != -1) {
            switch (hoverIdxExtraBtn) {
                case 0 -> doResetPresets();
                case 1 -> doDelPreset();
                case 2 -> doAddPreset();
                case 3 -> doZeroPreset();
                case 4 -> doNextPreset(false);
                case 5 -> doPrevPreset(false);
                case 6 -> doClose();
                default -> { }
            }

            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
        }

        // Allow quick-closing
        if (!isInAABB(mouseX, mouseY, guiLeft, guiTop, xSize, ySize)) {
            doClose();
        }
        return true;
    }

    protected Object[] handleSwitchesClicked(List<AbilityInfo> abilities, IBaseAbility selectedAbility, int selectedLevel,
                                             int hoverIdx) {
        if (hoverIdx != -1) {
            IBaseAbility hoveredAbility = abilities.get(hoverIdx).ability;
            boolean available = abilityAvailable(hoveredAbility);

            if (available) {
                int availableLevels = availableAbilities.maxLevel(hoveredAbility) + 1;

                if (hoveredAbility != selectedAbility || availableLevels > 1) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.TECH_BOOP.get(), 2F));
                }

                if (hoveredAbility == selectedAbility) {
                    selectedLevel = (selectedLevel + 1) % availableLevels;
                } else {
                    selectedLevel = 0;
                }

                selectedAbility = hoveredAbility;
            }
        }

        return new Object[]{selectedAbility, selectedLevel};
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.key() == 256 || event.matches(mc.options.keyInventory)) {
            doClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void doResetPresets() {
        config.reset(availableAbilities);
    }

    protected void doDelPreset() {
        if (config.presets.size() <= 1) return;
        config.presets.remove(config.currentPreset);
        config.currentPreset = Math.min(config.currentPreset, config.presets.size() - 1);
    }

    protected void doAddPreset() {
        if (config.presets.size() >= 99) return;
        config.presets.add(config.currentPreset + 1, new ToolPreset());
        config.currentPreset += 1;
    }

    protected void doZeroPreset() {
        config.currentPreset = 0;
    }

    protected void doNextPreset(boolean bound) {
        if (bound) {
            if (config.currentPreset < config.presets.size() - 1) config.currentPreset += 1;
        } else {
            config.currentPreset = (config.currentPreset + 1) % config.presets.size();
        }
    }

    protected void doPrevPreset(boolean bound) {
        if (bound) {
            if (config.currentPreset > 0) config.currentPreset -= 1;
        } else {
            config.currentPreset = (config.currentPreset + config.presets.size() - 1) % config.presets.size();
        }
    }

    protected void doClose() {
        Minecraft mc = Minecraft.getInstance();
        // A bit messy, but I suppose it works
        if (toolStack.getItem() instanceof ItemToolAbility tool) {
            tool.setConfiguration(toolStack, config);
            CompoundTag data = new CompoundTag();
            config.writeToNBT(data);
            GolemNetwork.sendToServer(ToolAbilityControlPayload.CODEC, new ToolAbilityControlPayload(data));
        }

        mc.setScreen(null);

        RenderInfoSystem.displayTooltip(config.getActivePreset().getMessage(), 0, PlayerInformPayload.ID_TOOLABILITY);

        if (mc.player != null) {
            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25F, config.getActivePreset().isNone() ? 0.75F : 1.25F);
        }
    }
}
