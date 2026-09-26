package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.inventory.recipes.AnnihilatorRecipes.Milestone;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.annihilator.add('#c:ingots/steel', '1000000', 'hbm:blueprints')    key: #tag | namespaced item | fluid name
 * e.annihilator.add('water', 5000000, 'minecraft:diamond')
 * e.annihilator.remove('#c:ingots/steel')                               every milestone of that key
 * e.annihilator.removeAll()
 * ids: hbm:annihilator/&lt;key&gt;, one per milestone
 * </pre>
 * Amount: items swallowed, or mB for a fluid; number or decimal string.
 */
public final class AnnihilatorHandler extends TableHandler {

    private record Row(Object key, Milestone milestone) {}

    @Override
    protected Object registry() {
        return AnnihilatorRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        AnnihilatorRecipes.register();
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Object, AnnihilatorRecipes.AnnihilatorRecipe> entry : AnnihilatorRecipes.recipes.entrySet()) {
            for (Milestone milestone : entry.getValue().milestones) rows.add(new Row(entry.getKey(), milestone));
        }
        return RecipeIds.views(machine(), rows, row -> keySpec(row.key()),
                row -> List.of(row.milestone().amount() + "x " + keySpec(row.key())),
                row -> List.of(RecipeIds.spec(row.milestone().payout())));
    }

    @Override
    protected void delete(RecipeView view) {
        Row row = (Row) view.recipe();
        AnnihilatorRecipes.removeMilestone(row.key(), row.milestone());
    }

    @Override
    public void reset() {
        AnnihilatorRecipes.reset();
    }

    public void add(String key, Object amount, Spec.Item payout) {
        edit("add " + key, () -> AnnihilatorRecipes.addMilestone(
                key(key), new Milestone(amount(amount), ScriptSpecs.stack(payout))));
    }

    public void remove(String key) {
        edit("remove " + key, () -> {
            if (!AnnihilatorRecipes.remove(key(key))) throw new IllegalArgumentException("no milestones for " + key);
        });
    }

    public void removeAll() {
        edit("removeAll", AnnihilatorRecipes::removeAll);
    }

    private static String keySpec(Object key) {
        return key instanceof String tag ? "#" + tag : RecipeIds.spec(key);
    }

    private static Object key(String spec) {
        if (spec.startsWith("#")) return ScriptSpecs.tag(spec).location().toString();
        if (spec.indexOf(':') >= 0) return ScriptSpecs.item(spec);
        return ScriptSpecs.fluidType(spec);
    }

    private static BigInteger amount(Object spec) {
        if (spec instanceof Number number) return BigInteger.valueOf(number.longValue());
        return new BigInteger(String.valueOf(spec).trim());
    }
}
