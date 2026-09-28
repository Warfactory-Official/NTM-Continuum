package com.hbm.handler.ability;

import net.minecraft.network.chat.Component;

/**
 * 1.7 {@code IBaseAbility}: one tool or weapon ability, possibly with levels.
 *
 * <p>{@link #getFullName} is a component rather than a client-translated string, so the same call serves
 * the tooltip, the configuration screen and the server's chat/HUD message.
 */
public interface IBaseAbility extends Comparable<IBaseAbility> {

    String getName();

    default String getExtension(int level) {
        return "";
    }

    default Component getFullName(int level) {
        return Component.translatable(getName()).append(getExtension(level));
    }

    default boolean isAllowed() {
        return true;
    }

    // 1 means no support for levels (i.e. the level is always 0).
    // The UI only supports levels() between 1 and 10 (inclusive).
    default int levels() {
        return 1;
    }

    default int sortOrder() {
        return hashCode();
    }

    @Override
    default int compareTo(IBaseAbility o) {
        return sortOrder() - o.sortOrder();
    }
}
