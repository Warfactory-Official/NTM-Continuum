package com.hbm.inventory.recipes.loader;

import com.hbm.Constants;
import com.hbm.config.GeneralConfig;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeTables;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * A recipe row's {@code "requires"}: the config branches 1.7 took while registering its tables. A row is in its table
 * while every literal holds; {@code !} negates one. 1.7 wrote {@code GeneralConfig.enableLBSM &&
 * GeneralConfig.enableLBSMSimpleChemsitry ? a : b}; the data rows are {@code a} with {@code lbsm_simple_chemistry} and
 * {@code b} with {@code !lbsm_simple_chemistry} (each LBSM switch is ANDed with LBSM itself, as 1.7 always wrote it).
 *
 * <ul>
 *   <li>{@code enable_528}, {@code expensive_mode}, {@code enable_528_pressurized} (1.7 {@code enable528PressurizedRecipes},
 *   off unless 528 mode is on)</li>
 *   <li>{@code lbsm} and {@code lbsm_simple_chemistry}, {@code lbsm_simple_centrifuge}, {@code lbsm_simple_crafting},
 *   {@code lbsm_simple_alloy}, {@code lbsm_unlock_anvil}, {@code lbsm_simple_armor}, {@code lbsm_simple_tool},
 *   {@code lbsm_simple_medicine}</li>
 *   <li>{@code #<tag>}: the item tag has an item (1.7's ore-dictionary compat branches)</li>
 * </ul>
 *
 * Tables re-test their rows whenever the flags changed ({@link #fingerprint}), so a config change applies at the next
 * rebuild. The named flags are also Golem Core recipe flags ({@link #init}): a crafting or smelting file carrying
 * {@code "golem:requires": [literals]} (Dejson {@code Crafting.requires}) is removed after each datapack load while a
 * literal fails, which is how 1.7's {@code CraftingManager} config branches are stated.
 */
public final class RecipeConditions {

    private RecipeConditions() {}

    private static final Map<String, BooleanSupplier> FLAGS = new LinkedHashMap<>();

    static {
        FLAGS.put("enable_528", () -> GeneralConfig.enable528);
        FLAGS.put("expensive_mode", () -> GeneralConfig.enableExpensiveMode);
        FLAGS.put("enable_528_pressurized", () -> GeneralConfig.enable528PressurizedRecipes);
        FLAGS.put("lbsm", () -> GeneralConfig.enableLBSM);
        FLAGS.put("lbsm_simple_chemistry", () -> GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleChemsitry);
        FLAGS.put("lbsm_simple_centrifuge", () -> GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCentrifuge);
        FLAGS.put("lbsm_simple_crafting", () -> GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting);
        FLAGS.put("lbsm_simple_alloy", () -> GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleAlloy);
        FLAGS.put("lbsm_unlock_anvil", () -> GeneralConfig.enableLBSM && GeneralConfig.enableLBSMUnlockAnvil);
        // 1.7 ArmorRecipes / ToolRecipes / ConsumableRecipes: GeneralConfig.enableLBSM && enableLBSMSimple*Recipes
        FLAGS.put("lbsm_simple_armor", () -> GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleArmorRecipes);
        FLAGS.put("lbsm_simple_tool", () -> GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleToolRecipes);
        FLAGS.put("lbsm_simple_medicine", () -> GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleMedicineRecipes);
    }

    private static boolean registered;

    /**
     * Declares every named flag as a Golem Core recipe flag ({@code RecipeTables.flag}), read by the crafting and
     * smelting files' {@code golem:requires}. Called once from {@code MainRegistry.bootstrap}, after the config loaded.
     */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        FLAGS.forEach(RecipeTables::flag);
    }

    /** Whether every literal of {@code requires} holds now (null or empty: always). {@code where} names the row in logs. */
    public static boolean holds(String[] requires, String where) {
        if (requires == null) return true;
        for (String literal : requires) {
            boolean negated = literal.startsWith("!");
            String flag = negated ? literal.substring(1) : literal;
            boolean value;
            if (flag.startsWith("#")) {
                value = tagHasItem(flag.substring(1));
            } else {
                BooleanSupplier supplier = FLAGS.get(flag);
                if (supplier == null) {
                    Constants.LOG.warn("Recipe {} requires the unknown flag {}; leaving it out", where, literal);
                    return false;
                }
                value = supplier.getAsBoolean();
            }
            if (value == negated) return false;
        }
        return true;
    }

    /** The current value of every named flag, as one string: a table whose rows were tested under another rebuilds. */
    public static String fingerprint() {
        StringBuilder out = new StringBuilder();
        for (BooleanSupplier flag : FLAGS.values()) out.append(flag.getAsBoolean() ? '1' : '0');
        return out.toString();
    }

    private static boolean tagHasItem(String tag) {
        for (Holder<Item> ignored : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Ids.parse(tag)))) return true;
        return false;
    }
}
