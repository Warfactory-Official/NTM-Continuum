package com.hbm.api.script;

import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The payload of the startup script event {@code HBMEvents.registry}: what a startup script may add to HBM with data
 * alone. Each call only records the request; HBM applies it once at a safe point of its own start-up (after every mod's
 * registers are in the registries, before any datapack loads), on the client and the server alike. A request that
 * cannot be applied (a name taken, a bad value) is logged and skipped, never thrown into the game.
 *
 * <pre>{@code
 * // startup script
 * HBMEvents.registry(event => {
 *   event.material('zinc').colors(0xA8B3B5, 0x4E5A5C, 0xC6CFD0).smelting('SMELTABLE')
 *   event.material('brass_scrap').smelting('SMELTABLE').smeltsInto('copper', 2, 1)
 * })
 * }</pre>
 *
 * <p><b>Materials</b> are crucible/foundry materials ({@code Mats.register}): a name, colours and how they melt. HBM
 * recognises an item as the material through the {@code c:} item tags ({@code c:ingots/zinc}, {@code c:nuggets/zinc},
 * {@code c:plates/zinc}, ...), so tag existing items (another mod's ingots, KubeJS items) with them. Script materials get
 * no auto-generated items ({@code setAutogen}): those items need models that HBM generates at build time.
 *
 * <p><b>Fluids</b> are not made here: a fluid registered by KubeJS's startup fluid builder is an HBM fluid already,
 * and its HBM properties (temperature, colour, traits, hazard) come from the datapack file
 * {@code data/<namespace>/fluid_property/<name>.json}. New trait <i>types</i> are Java (an addon's
 * {@code FluidProperties.registerTrait}).
 */
public final class HbmScriptRegistry {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]+");

    private final List<Material> materials = new ArrayList<>();
    private boolean closed;

    HbmScriptRegistry() {}

    /** Requests a new material {@code name} (lower case, digits and {@code _}; unique among all materials). */
    public synchronized Material material(String name) {
        if (closed) throw new IllegalStateException("HBMEvents.registry is over; materials can only be added while it runs");
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("A material name is lower case letters, digits and _: " + name);
        }
        Material material = new Material(name);
        materials.add(material);
        return material;
    }

    synchronized List<Material> close() {
        closed = true;
        return Collections.unmodifiableList(new ArrayList<>(materials));
    }

    /** A requested material: a builder whose values HBM copies onto the real material when it applies the request. */
    public static final class Material {

        final String name;
        @Nullable String displayName;
        int solidColorLight = 0xFF4A00;
        int solidColorDark = 0x802000;
        int moltenColor = 0xFF4A00;
        SmeltingBehavior smelting = SmeltingBehavior.NOT_SMELTABLE;
        @Nullable String smeltsInto;
        int convIn = 1;
        int convOut = 1;

        Material(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /** The name shown to players (default: the name in title case). */
        public Material displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /** The solid colours (light, dark; the item and slag shading) and the molten colour, as 0xRRGGBB. */
        public Material colors(int solidLight, int solidDark, int molten) {
            this.solidColorLight = solidLight;
            this.solidColorDark = solidDark;
            this.moltenColor = molten;
            return this;
        }

        /** {@code NOT_SMELTABLE} (default), {@code VAPORIZES}, {@code BREAKS}, {@code SMELTABLE} or {@code ADDITIVE}. */
        public Material smelting(String behavior) {
            this.smelting = SmeltingBehavior.valueOf(behavior.trim().toUpperCase(Locale.ROOT));
            return this;
        }

        /** Melts into another material ({@code in} of this make {@code out} of that), as coal melts into carbon. */
        public Material smeltsInto(String material, int in, int out) {
            if (in <= 0 || out <= 0) throw new IllegalArgumentException("conversion amounts must be positive");
            this.smeltsInto = material;
            this.convIn = in;
            this.convOut = out;
            return this;
        }
    }
}
