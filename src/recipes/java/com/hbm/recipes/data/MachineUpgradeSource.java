package com.hbm.recipes.data;

import com.hbm.api.data.MachinePartDataMaps;
import com.hbm.api.data.MachinePartDataMaps.Upgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import dev.golem.core.data.DataMapSource;

/**
 * 1.7 {@code ModItems} {@code new ItemMachineUpgrade(type, tier)}: the typeless upgrades ({@code new ItemMachineUpgrade()})
 * are {@code SPECIAL} tier 0. The order is the file's (ejector and stack upgrades first).
 */
public final class MachineUpgradeSource extends DataMapSource<Upgrade> {

    public MachineUpgradeSource() {
        super(MachinePartDataMaps.MACHINE_UPGRADE, false);
    }

    private void upgrade(String id, UpgradeType type, int tier) {
        put("hbm:" + id, new Upgrade(type, tier));
    }

    @Override
    protected void values() {
        upgrade("upgrade_ejector_1", UpgradeType.SPECIAL, 1);
        upgrade("upgrade_ejector_2", UpgradeType.SPECIAL, 2);
        upgrade("upgrade_ejector_3", UpgradeType.SPECIAL, 3);
        upgrade("upgrade_stack_1", UpgradeType.SPECIAL, 1);
        upgrade("upgrade_stack_2", UpgradeType.SPECIAL, 2);
        upgrade("upgrade_stack_3", UpgradeType.SPECIAL, 3);
        upgrade("upgrade_radius", UpgradeType.SPECIAL, 0);
        upgrade("upgrade_health", UpgradeType.SPECIAL, 0);
        upgrade("upgrade_5g", UpgradeType.SPECIAL, 0);
        upgrade("upgrade_speed_1", UpgradeType.SPEED, 1);
        upgrade("upgrade_speed_2", UpgradeType.SPEED, 2);
        upgrade("upgrade_speed_3", UpgradeType.SPEED, 3);
        upgrade("upgrade_effect_1", UpgradeType.EFFECT, 1);
        upgrade("upgrade_effect_2", UpgradeType.EFFECT, 2);
        upgrade("upgrade_effect_3", UpgradeType.EFFECT, 3);
        upgrade("upgrade_power_1", UpgradeType.POWER, 1);
        upgrade("upgrade_power_2", UpgradeType.POWER, 2);
        upgrade("upgrade_power_3", UpgradeType.POWER, 3);
        upgrade("upgrade_overdrive_1", UpgradeType.OVERDRIVE, 1);
        upgrade("upgrade_overdrive_2", UpgradeType.OVERDRIVE, 2);
        upgrade("upgrade_overdrive_3", UpgradeType.OVERDRIVE, 3);
        upgrade("upgrade_afterburn_1", UpgradeType.AFTERBURN, 1);
        upgrade("upgrade_afterburn_2", UpgradeType.AFTERBURN, 2);
        upgrade("upgrade_afterburn_3", UpgradeType.AFTERBURN, 3);
        upgrade("upgrade_fortune_1", UpgradeType.FORTUNE, 1);
        upgrade("upgrade_fortune_2", UpgradeType.FORTUNE, 2);
        upgrade("upgrade_fortune_3", UpgradeType.FORTUNE, 3);
        // 1.7 upgrade_nullifier = new ItemMachineUpgrade(): SPECIAL 0 (the mining laser finds it by item)
        upgrade("upgrade_nullifier", UpgradeType.SPECIAL, 0);
        upgrade("upgrade_smelter", UpgradeType.SPECIAL, 0);
        upgrade("upgrade_shredder", UpgradeType.SPECIAL, 0);
        upgrade("upgrade_centrifuge", UpgradeType.SPECIAL, 0);
        upgrade("upgrade_crystallizer", UpgradeType.SPECIAL, 0);
        upgrade("upgrade_gc_speed", UpgradeType.SPECIAL, 0);
        // 1.7 upgrade_screm = new ItemMachineUpgrade(): SPECIAL 0 (the mining laser and the Maxwell find it by item)
        upgrade("upgrade_screm", UpgradeType.SPECIAL, 0);
    }
}
