package com.hbm.items;

/**
 * Dejson declarations of the G-C rendering/input gap batch. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}.
 */
public final class RenderInputDeclarations {

    private static boolean declared;

    private RenderInputDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // 1.7 ItemRenderBoltgun handled every slot (inventory included), an IItemRenderer of its own: the weapon
        // frame. Replaces the authored machine_obj definition, whose scale/yaw were invented framing.
        ItemRendererDeclarations.model("boltgun", "boltgun", "weapon");
    }
}
