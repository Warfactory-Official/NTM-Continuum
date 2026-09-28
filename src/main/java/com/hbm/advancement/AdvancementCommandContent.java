package com.hbm.advancement;

/**
 * The advancement + command family's one bootstrap line: registers the criterion triggers and the achievement icon
 * items, hooks the boltgun's melee half (the "Go Fish" route), and installs the server command tree on every loader.
 * Client commands are registered from {@code HbmClientRegistrations}.
 */
public final class AdvancementCommandContent {

    private AdvancementCommandContent() {}

    public static void init() {
        HbmCriteria.init();
        com.hbm.items.ModAchievementItems.init();
        com.hbm.items.tool.ItemBoltgun.installAttack();
        com.hbm.commands.HbmServerCommands.init();
    }
}
