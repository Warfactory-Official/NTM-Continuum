package com.hbm.advancement;

import com.hbm.commands.HbmServerCommands;
import com.hbm.items.ModAchievementItems;
import com.hbm.items.tool.ItemBoltgun;


/**
 * The advancement + command family's one bootstrap line: registers the criterion triggers and the achievement icon
 * items, hooks the boltgun's melee half (the "Go Fish" route), and installs the server command tree on every loader.
 * Client commands are registered from {@code HbmClientRegistrations}.
 */
public final class AdvancementCommandContent {

    private AdvancementCommandContent() {}

    public static void init() {
        HbmCriteria.init();
        ModAchievementItems.init();
        ItemBoltgun.installAttack();
        HbmServerCommands.init();
    }
}
