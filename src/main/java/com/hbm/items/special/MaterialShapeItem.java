package com.hbm.items.special;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.world.item.Item;

/** A registered item that carries one real material and one material shape. */
public class MaterialShapeItem extends ItemCustomLore {
    public final NTMMaterial material;
    public final MaterialShapes shape;

    public MaterialShapeItem(Item.Properties properties, NTMMaterial material, MaterialShapes shape) {
        super(properties);
        this.material = material;
        this.shape = shape;
    }

    /** Uses the registration's existing canonical material tags, retaining plain lore items for other tags. */
    public static ItemCustomLore fromTags(Item.Properties properties, String... tags) {
        NTMMaterial material = null;
        MaterialShapes shape = null;
        for (var tag : tags) {
            for (var candidate : MaterialShapes.values()) {
                // TINY and DUSTTINY share the same native tag; DUSTTINY names the material dust form.
                if (candidate == MaterialShapes.TINY) continue;
                var prefix = "c:" + candidate.tagPlural + "/";
                if (!tag.startsWith(prefix)) continue;
                var found = Mats.matByName.get(tag.substring(prefix.length()));
                if (found == null) continue;
                if (material != null && (material != found || shape != candidate))
                    throw new IllegalArgumentException("Item tags declare several material shapes");
                material = found;
                shape = candidate;
            }
        }
        return material == null ? new ItemCustomLore(properties) : new MaterialShapeItem(properties, material, shape);
    }
}
