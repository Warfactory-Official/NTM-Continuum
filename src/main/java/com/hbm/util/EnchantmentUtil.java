package com.hbm.util;

import net.minecraft.world.entity.player.Player;

/**
 * 1.7 {@code EnchantmentUtil}, the experience half the IV bags use.
 *
 * <p>1.7 counted experience with 1.7's own per-level cost ({@code 17}, {@code 17 + 3n}, {@code 62 + 7n}). The
 * port's levels are modern levels, so the cost is the modern {@code Player.getXpNeededForNextLevel} curve; a bag
 * still holds exactly 100 points of experience, which is what the 1.7 numbers meant.
 */
public final class EnchantmentUtil {

    private EnchantmentUtil() {}

    /** The points needed to go from {@code level} to {@code level + 1}. */
    public static int xpBarCap(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : (level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2);
    }

    /** "Fun fact: experienceTotal lies" - counts completed levels plus the fraction of the current bar. */
    public static int getTotalExperience(Player player) {
        int xp = 0;
        for (int i = 0; i < player.experienceLevel; i++) xp += xpBarCap(i);
        xp += (int) (xpBarCap(player.experienceLevel) * player.experienceProgress);
        return xp;
    }

    /** As {@code Player.giveExperiencePoints} but without raising the score; {@code silent} skips the level sound. */
    public static void addExperience(Player player, int xp, boolean silent) {
        int j = Integer.MAX_VALUE - player.totalExperience;
        if (xp > j) xp = j;

        player.experienceProgress += (float) xp / (float) xpBarCap(player.experienceLevel);

        for (player.totalExperience += xp; player.experienceProgress >= 1.0F;
             player.experienceProgress /= (float) xpBarCap(player.experienceLevel)) {
            player.experienceProgress = (player.experienceProgress - 1.0F) * (float) xpBarCap(player.experienceLevel);
            if (silent) addExperienceLevelSilent(player, 1);
            else player.giveExperienceLevels(1);
        }
    }

    public static void setExperience(Player player, int xp) {
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
        addExperience(player, xp, true);
    }

    public static void addExperienceLevelSilent(Player player, int level) {
        player.experienceLevel += level;
        if (player.experienceLevel < 0) {
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
            player.totalExperience = 0;
        }
    }
}
