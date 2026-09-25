package com.hbm.scripting;

import com.hbm.Constants;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingMold;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ModMoldItems;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * {@code NTMEvents.shapes}.
 *
 * <pre>
 * e.create('GEAR', 'gear', 'gears', 144, '%s Gear')          tag c:gears/&lt;material&gt;, 2 ingots of material
 * e.create(...).autogen()                                     one item per material listing it, gear_&lt;material&gt;
 * e.create(...).autogen('kubejs:item/gear')                   greyscale art, else hbm:items/&lt;prefix&gt;
 * e.create(...).mold()                                        mold_gear: foundry mold, casts 1
 * e.create(...).mold('large', 9)                              mold_gears: basin, casts 9
 * e.create(...).mold('gear_pair', 'small', 2, 'kubejs:item/mold_gear')   name, size, amount, sprite
 * </pre>
 * A mold's default sprite is {@code hbm:items/mold_base}; it is made on the anvil from a blank mold and
 * {@code amount} items of the shape, like the built-in molds.
 */
public final class ShapesEvent {

    private static int nextMoldId = 1000;

    ShapesEvent() {}

    public ShapeBuilder create(String name, String itemPrefix, String tagPlural, int quantity, String displayTemplate) {
        return new ShapeBuilder(MaterialShapes.register(name.toUpperCase(Locale.US), itemPrefix, tagPlural, quantity,
                displayTemplate));
    }

    public static final class ShapeBuilder {

        private final MaterialShapes shape;

        private ShapeBuilder(MaterialShapes shape) {
            this.shape = shape;
        }

        public ShapeBuilder autogen() {
            return autogen(Constants.MOD_ID + ":items/" + shape.itemPrefix);
        }

        public ShapeBuilder autogen(String art) {
            ModAutogenItems.addShape(shape, shape.itemPrefix, art);
            return this;
        }

        public ShapeBuilder mold() {
            return mold("small", 1);
        }

        public ShapeBuilder mold(String size, int amount) {
            return mold(amount == 1 ? shape.itemPrefix : shape.itemPrefix + "s", size, amount);
        }

        public ShapeBuilder mold(String name, String size, int amount) {
            return mold(name, size, amount, Constants.MOD_ID + ":items/mold_base");
        }

        public ShapeBuilder mold(String name, String size, int amount, String sprite) {
            int moldSize = switch (size.toLowerCase(Locale.US)) {
                case "small" -> 0;
                case "large" -> 1;
                default -> throw new IllegalArgumentException("Mold size " + size + " is not small or large");
            };
            RegistryObject<ItemMold> mold = ModMoldItems.register(ScriptedContent.items(ModMoldItems.ITEMS),
                    new ItemMold.MoldShape(nextMoldId++, moldSize, name, shape, amount));
            ScriptedContent.itemAsset("mold_" + name, sprite,
                    "Foundry Mold (" + shape.displayName + (amount > 1 ? " x" + amount : "") + ")");
            ScriptedRecipes.enqueueContent(AnvilRecipes.class, "anvil mold_" + name, () -> AnvilRecipes.addSmithing(
                    new AnvilSmithingMold(new ItemStack(mold.get()), example(shape).count(amount), shape, amount)));
            return this;
        }

        private static AStack example(MaterialShapes shape) {
            for (NTMMaterial material : Mats.orderedList) {
                if (ModAutogenItems.has(shape, material)) return new MaterialStack(material, shape);
                if (BuiltInRegistries.ITEM.getTagOrEmpty(material.tag(shape)).iterator().hasNext()) {
                    return new TagStack(material.tag(shape));
                }
            }
            return new TagStack(Mats.MAT_IRON.tag(shape));
        }
    }
}
