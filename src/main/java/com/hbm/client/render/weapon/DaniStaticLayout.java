package com.hbm.client.render.weapon;

import java.util.List;
import net.minecraft.world.item.ItemDisplayContext;

/** Data-only static draw plan, kept free of client renderer types so GameTests can verify it. */
public final class DaniStaticLayout {

    public enum Draw {
        CELESTIAL,
        LUNAR,
        GUI_CELESTIAL,
        GUI_LUNAR,
        THIRD_PERSON_RIGHT_LUNAR,
        THIRD_PERSON_LEFT_LUNAR
    }

    private static final List<Draw> INVENTORY = List.of(Draw.GUI_CELESTIAL, Draw.GUI_LUNAR);
    private static final List<Draw> THIRD_PERSON_RIGHT = List.of(
            Draw.THIRD_PERSON_RIGHT_LUNAR, Draw.THIRD_PERSON_LEFT_LUNAR);
    private static final List<Draw> THIRD_PERSON_LEFT = List.of(
            Draw.THIRD_PERSON_LEFT_LUNAR, Draw.THIRD_PERSON_RIGHT_LUNAR);
    private static final List<Draw> LUNAR = List.of(Draw.LUNAR);
    private static final List<Draw> CELESTIAL = List.of(Draw.CELESTIAL);

    private DaniStaticLayout() {}

    public static List<Draw> forContext(ItemDisplayContext context) {
        if (context == ItemDisplayContext.GUI) return INVENTORY;
        if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return THIRD_PERSON_RIGHT;
        if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return THIRD_PERSON_LEFT;
        return context == ItemDisplayContext.HEAD ? LUNAR : CELESTIAL;
    }
}
