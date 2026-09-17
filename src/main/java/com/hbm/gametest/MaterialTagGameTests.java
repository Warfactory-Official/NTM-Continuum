package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.ModMaterialTags;
import com.hbm.inventory.material.NTMMaterial;
import dev.golem.core.Ids;
import dev.golem.core.dejson.Data;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The half of the material tags that a declaration cannot state: whether it is complete.
 *
 * <p>{@link ModMaterialTags} says which shapes a material comes in, and the generator turns each
 * into a tag. What neither can see is an item that exists in the registry and is in no tag at all —
 * the one failure mode of a derived tag set, and a silent one. Nothing crashes; a recipe that asks
 * for {@code #c:plates/steel} simply refuses a steel plate, in a machine, for a player, with no
 * entry in any log. So the check runs the other way round here, from the registry back to the tags.
 */
public final class MaterialTagGameTests {

    private static final String ITEM_TAGS = "data/c/tags/item/";

    private MaterialTagGameTests() {}

    /**
     * Every {@code <prefix>_<material>} item the registry holds is in the {@code c:} tag its shape
     * implies. This is the test that keeps the declaration honest as items are added: registering
     * {@code hbm:plate_foo} without giving {@code foo} a {@code PLATE} makes this red.
     */
    public static void everyShapeItemIsTagged(GameTestHelper helper) {
        ModMaterialTags.declare();
        List<String> problems = new ArrayList<>();
        int checked = 0;

        for (NTMMaterial material : Mats.orderedList) {
            for (MaterialShapes shape : MaterialShapes.values()) {
                Item item = BuiltInRegistries.ITEM
                        .getOptional(Ids.of("hbm", shape.itemPrefix + "_" + material.name))
                        .orElse(null);
                if (item == null) {
                    continue;
                }
                checked++;
                TagKey<Item> tag = TagKey.create(Registries.ITEM,
                        Ids.of("c", shape.tagPlural + "/" + material.name));
                if (!new ItemStack(item).is(tag)) {
                    problems.add("hbm:" + shape.itemPrefix + "_" + material.name
                            + " exists but is in no c:" + shape.tagPlural + "/" + material.name);
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(checked > 0, "no material shape item is registered, so nothing was checked");
        Constants.LOG.info("[TAGS] {} shape items over {} materials, every one in the tag its shape implies",
                checked, Mats.orderedList.size());
        helper.succeed();
    }

    /**
     * Every item a declared tag names is registered, and is in that tag at runtime.
     *
     * <p>The second half is the delivery check the models have too: the tag file this asks about is
     * the one {@code dejsonGenerate} wrote, so membership proves it was generated, packed, renamed
     * for the target's directory layout, and read by the tag loader.
     */
    public static void declaredTagsAreLoaded(GameTestHelper helper) {
        ModMaterialTags.declare();
        List<String> problems = new ArrayList<>();
        int tags = 0;
        int members = 0;

        for (Map.Entry<String, JsonObject> file : Data.files().entrySet()) {
            String path = file.getKey();
            if (!path.startsWith(ITEM_TAGS)) {
                continue;
            }
            String tagPath = path.substring(ITEM_TAGS.length(), path.length() - ".json".length());
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Ids.of("c", tagPath));
            tags++;
            for (JsonElement value : file.getValue().getAsJsonArray("values")) {
                String id = value.getAsString();
                if (id.startsWith("#")) {
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.getOptional(Ids.parse(id)).orElse(null);
                if (item == null) {
                    problems.add("c:" + tagPath + " names " + id + ", which is not registered");
                    continue;
                }
                members++;
                if (!new ItemStack(item).is(tag)) {
                    problems.add(id + " was declared into c:" + tagPath + " but is not in it");
                }
            }
        }

        helper.assertTrue(problems.isEmpty(), join(problems));
        helper.assertTrue(tags > 0 && members > 0, "no item tag was declared, so nothing was checked");
        Constants.LOG.info("[TAGS] {} declared item tags loaded, {} memberships hold", tags, members);
        helper.succeed();
    }

    private static String join(List<String> problems) {
        if (problems.size() <= 8) {
            return String.join("; ", problems);
        }
        return String.join("; ", problems.subList(0, 8))
                + "; and " + (problems.size() - 8) + " more";
    }
}
