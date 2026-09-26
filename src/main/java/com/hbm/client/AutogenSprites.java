package com.hbm.client;

import com.hbm.Constants;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import com.hbm.scripting.ScriptedMaterials;
import dev.golem.core.render.Sprites;
import java.util.HashSet;
import java.util.Set;

/**
 * The material-shape sprites, computed while the atlas is stitched instead of shipped as files.
 *
 * <p>Each autogen shape draws one greyscale plate — {@code hbm:items/plate_cast}, authored in the band
 * white..{@code #505050} — and every material that declares that shape gets it recoloured onto its own
 * {@link NTMMaterial#solidColorLight} and {@link NTMMaterial#solidColorDark}. That is what 1.7 did
 * through {@code RGBMutatorInterpolatedComponentRemap} on a mutable atlas icon, and what this mod did
 * by pre-baking 187 PNGs with a script until the remap moved into Golem Core.
 *
 * <p>The sprite ids come from {@link ModAutogenItems}' own shape table, so an item and its sprite can
 * only ever be named the same way: adding a shape to a material in {@link Mats} mints the item and its
 * texture together, with no file to draw and no generator to re-run.
 *
 * <p>A tint cannot stand in for this. The remap has an offset term and a multiply does not, so tinting
 * reaches a material's highlight or its shadow but never both — measured across the roster, mean error
 * 26.5/255 per channel and a worst case of 175.
 */
public final class AutogenSprites {

    private AutogenSprites() {}

    /**
     * Sprites with authored art, which the remap must leave alone.
     *
     * <p>Bismuth declares one flat colour for both ends of its band, so interpolating it produces a
     * silhouette rather than a fragment. 1.7 hit the same wall and answered it the same way, with an
     * override sprite ({@code ItemAutogen.aot(MAT_BISMUTH, "bedrock_ore_fragment_bismuth")}).
     */
    private static final Set<String> AUTHORED = Set.of("bedrock_ore_fragment_bismuth");

    private static final Set<String> DECLARED = new HashSet<>();
    private static boolean installed;

    /** Declares every material-shape remap. Client only, before the first resource load. */
    public static synchronized void install() {
        installed = true;
        sync();
        ModAutogenItems.onRegistered(AutogenSprites::sync);
    }

    /** Declares the pairs added since {@link #install}; a no-op before it. */
    public static synchronized void sync() {
        if (!installed) {
            return;
        }
        int sprites = 0;
        for (MaterialShapes shape : ModAutogenItems.shapeOrder()) {
            String art = ModAutogenItems.art(shape);
            if (art == null) {
                continue;
            }
            String base = ModAutogenItems.base(shape);
            Sprites.Remap remap = Sprites.remap(Sprites.Atlas.ITEMS).texture(art);
            for (NTMMaterial material : Mats.orderedList) {
                if (!material.autogen.contains(shape) || AUTHORED.contains(base + "_" + material.name)
                        || !DECLARED.add(base + "_" + material.name)) {
                    continue;
                }
                if (material.solidColorLight == material.solidColorDark) {
                    Constants.LOG.warn("{} declares one flat colour, so {}_{} needs an authored sprite",
                            material.name, base, material.name);
                    continue;
                }
                remap.permutation(material.name, material.solidColorLight, material.solidColorDark);
                sprites++;
            }
            if (!remap.isEmpty()) {
                remap.declare();
            }
        }
        for (ScriptedMaterials.Remap remap : ScriptedMaterials.remaps()) {
            if (!DECLARED.add(remap.source() + "_" + remap.material().name)) {
                continue;
            }
            Sprites.remap(remap.blockAtlas() ? Sprites.Atlas.BLOCKS : Sprites.Atlas.ITEMS)
                    .texture(remap.source())
                    .band(remap.bandLight(), remap.bandDark())
                    .permutation(remap.material().name, remap.material().solidColorLight, remap.material().solidColorDark)
                    .declare();
            sprites++;
        }
        Constants.LOG.info("Declared {} material-shape sprite(s) for load-time generation", sprites);
    }
}
