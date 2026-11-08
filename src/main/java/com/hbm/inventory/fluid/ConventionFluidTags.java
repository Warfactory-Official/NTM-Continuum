package com.hbm.inventory.fluid;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.dejson.Tags;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * port: the {@code c:} fluid tags other mods put the same substance in (Mekanism, Immersive Engineering and its
 * petroleum addon, Create addons). Each HBM fluid joins its tags and shares them in its {@code fluid_property} file,
 * so another mod's fluid in one of them is that HBM fluid in HBM's tanks and recipes. Forge reads the {@code forge:}
 * copy the build makes of every {@code c:} tag.
 */
public final class ConventionFluidTags {

    private ConventionFluidTags() {}

    private static final Map<String, List<String>> TAGS = new LinkedHashMap<>();

    static {
        put("oil", "crude_oil", "oil");
        put("ethanol", "ethanol");
        put("diesel", "diesel");
        put("gasoline", "gasoline");
        put("kerosene", "kerosene");
        put("naphtha", "naphtha");
        put("lubricant", "lubricant");
        put("petroleum", "petroleum_gas");
        put("xylene", "benzol");
        put("biofuel", "biodiesel");
        put("sunfloweroil", "plantoil");
        put("coalcreosote", "creosote");
        put("hydrogen", "hydrogen");
        put("oxygen", "oxygen");
        put("steam", "steam");
        put("sulfuric_acid", "sulfuric_acid");
        put("chlorine", "chlorine");
        put("heavywater", "heavy_water");
        put("uf6", "uranium_hexafluoride");
        put("sodium", "sodium");
        put("mercury", "mercury");
    }

    private static void put(String fluid, String... tags) {
        TAGS.put(Constants.MOD_ID + ":" + fluid, List.of(tags));
    }

    /** Every HBM fluid with conventional tags, by registry name, and the tag paths under {@code c}. */
    public static Map<String, List<String>> all() {
        return TAGS;
    }

    /** The {@code c:} tags {@code fluid} shares; empty for a fluid no other mod names. */
    public static List<TagKey<Fluid>> of(FluidType fluid) {
        return TAGS.getOrDefault(fluid.getRegistryName(), List.of()).stream()
                .map(path -> TagKey.create(Registries.FLUID, Ids.of("c", path))).toList();
    }

    public static void declare() {
        TAGS.forEach((fluid, tags) -> {
            for (String tag : tags) Tags.member("fluid", "c", tag, fluid);
        });
    }
}
