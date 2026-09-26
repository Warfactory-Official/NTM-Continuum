package com.hbm.api.content.recipes;

import java.util.List;

/**
 * One machine recipe as scripts see it. {@code inputs}/{@code outputs} are in spec form
 * ({@code "2x minecraft:stone"}, {@code "#c:plates/steel"}, {@code "1000x water"}, {@code "72x iron"});
 * {@code recipe} is the machine's own object.
 */
public record RecipeView(String id, String machine, List<String> inputs, List<String> outputs, Object recipe) {

    public boolean hasInput(String spec) {
        return inputs.stream().anyMatch(in -> RecipeIds.sameSpec(in, spec));
    }

    public boolean hasOutput(String spec) {
        return outputs.stream().anyMatch(out -> RecipeIds.sameSpec(out, spec));
    }
}
