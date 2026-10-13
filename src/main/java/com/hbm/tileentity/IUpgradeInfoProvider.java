package com.hbm.tileentity;

import java.util.HashMap;
import java.util.List;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/**
 * Implemented by machine BlockEntities that accept {@link com.hbm.items.machine.ItemMachineUpgrade}s
 * (the 1.12.2 {@code IUpgradeInfoProvider}). Drives both the mechanics (via
 * {@link com.hbm.inventory.UpgradeManagerNT}, which clamps stacked levels to {@link #getValidUpgrades()})
 * and the GUI info:
 * <ul>
 *   <li>{@code level > 0}: hovering an upgrade item inside the machine's GUI:
 *       {@link #provideInfo} prints the exact effect numbers for that item's tier.</li>
 *   <li>{@code level == 0}: the sentinel the "Acceptable Upgrades" slot-hover panel uses to probe
 *       which types the machine reacts to (see {@code GuiUtil.upgradeInfo}).</li>
 * </ul>
 */
public interface IUpgradeInfoProvider {

    String KEY_ACID = "upgrade.acid";
    String KEY_BURN = "upgrade.burn";
    String KEY_CONSUMPTION = "upgrade.consumption";
    String KEY_COOLANT_CONSUMPTION = "upgrade.coolantConsumption";
    String KEY_DELAY = "upgrade.delay";
    String KEY_SPEED = "upgrade.speed";
    String KEY_EFFICIENCY = "upgrade.efficiency";
    String KEY_PRODUCTIVITY = "upgrade.productivity";
    String KEY_FORTUNE = "upgrade.fortune";
    String KEY_RANGE = "upgrade.range";

    boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo);

    void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo);

    HashMap<UpgradeType, Integer> getValidUpgrades();

    /** The yellow {@code >>> Machine Name <<<} header line (1.12.2 {@code getStandardLabel}). */
    static Component getStandardLabel(Block block) {
        return Component.literal(">>> ").append(block.getName()).append(" <<<").withStyle(ChatFormatting.YELLOW);
    }

    /** 1.12.2 {@code BobMathUtil.getBlink()}, which drives the blinking OVERDRIVE "YES" line. */
    static boolean getBlink() {
        return System.currentTimeMillis() % 1000 < 500;
    }
}
