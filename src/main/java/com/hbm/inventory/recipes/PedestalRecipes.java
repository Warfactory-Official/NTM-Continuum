package com.hbm.inventory.recipes;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code PedestalRecipes}, as data: the ritual recipes whose sets the clay tablets reveal and the pedestal
 * ({@code BlockPedestal}) performs - the output, the nine inputs (an item id, a {@code #tag}, or an enum-variant
 * item as {@code id@variant}), the extra condition and the recipe set - in 1.7's registration order, which the
 * tablet's seeded draw and the pedestal's first-match search both depend on.
 */
public final class PedestalRecipes {

    public enum PedestalExtraCondition { NONE, FULL_MOON, NEW_MOON, SUN, GOOD_KARMA, BAD_KARMA }

    /** One ingredient: {@code id} is an item id, a {@code #tag}, or {@code id@variant}. */
    public record Input(String id, int count) {}

    public static final class PedestalRecipe {
        public final String output;
        public final int outputCount;
        public final Input[] input;
        public PedestalExtraCondition extra = PedestalExtraCondition.NONE;
        public int recipeSet = 0;

        public PedestalRecipe(String output, int outputCount, Input... input) {
            this.output = output;
            this.outputCount = outputCount;
            this.input = input;
        }

        public PedestalRecipe extra(PedestalExtraCondition extra) { this.extra = extra; return this; }
        public PedestalRecipe set(int set) { this.recipeSet = set; return this; }
    }

    public static final List<PedestalRecipe> recipes = new ArrayList<>();
    @SuppressWarnings("unchecked")
    public static final ArrayList<PedestalRecipe>[] recipeSets = new ArrayList[2];

    static {
        for (int i = 0; i < recipeSets.length; i++) recipeSets[i] = new ArrayList<>();
        registerDefaults();
    }

    private PedestalRecipes() {}

    private static Input i(String id) { return new Input(id, 1); }
    private static Input i(String id, int count) { return new Input(id, count); }

    // 1.7 GunFactory.EnumModSpecial ordinals for weapon_mod_special
    private static final String SCOPE = "hbm:weapon_mod_special@1";
    private static final String SILENCER = "hbm:weapon_mod_special@0";
    private static final String FURNITURE_BLACK = "hbm:weapon_mod_special@9";

    private static void registerDefaults() {
        register(new PedestalRecipe("hbm:gun_light_revolver_dani", 1,
                null, i("#c:plates/lead"), null,
                i("#c:plates/gold"), i("hbm:gun_light_revolver"), i("#c:plates/gold"),
                null, i("#c:plates/lead"), null));

        register(new PedestalRecipe("hbm:gun_maresleg_broken", 1,
                i("hbm:barbed_wire"), i("#c:plates/weaponsteel"), i("hbm:barbed_wire"),
                i("#c:plates/weaponsteel"), i("hbm:gun_maresleg"), i("#c:plates/weaponsteel"),
                i("hbm:barbed_wire"), i("#c:plates/weaponsteel"), i("hbm:barbed_wire")));

        register(new PedestalRecipe("hbm:gun_heavy_revolver_lilmac", 1,
                null, i(SCOPE), null,
                i("hbm:powder_magic"), i("hbm:gun_heavy_revolver"), i("#c:plates/weaponsteel"),
                null, i("#c:grips/bone"), i("minecraft:apple", 3)));

        register(new PedestalRecipe("hbm:gun_heavy_revolver_protege", 1,
                i("hbm:dungeon_chain", 16), i("#c:gems/cinnabar"), i("hbm:dungeon_chain", 16),
                i("hbm:scrap_nuclear"), i("hbm:gun_heavy_revolver"), i("hbm:scrap_nuclear"),
                i("hbm:dungeon_chain", 16), i("#c:gems/cinnabar"), i("hbm:dungeon_chain", 16)));

        register(new PedestalRecipe("hbm:gun_amat_subtlety", 1,
                i("#c:ingots/starmetal"), i("#c:plates_cast/aluminium"), i("#c:ingots/starmetal"),
                i("#c:plates_cast/aluminium"), i("hbm:gun_amat"), i("#c:plates_cast/aluminium"),
                i("#c:ingots/starmetal"), i("#c:plates_cast/aluminium"), i("#c:ingots/starmetal")));

        register(new PedestalRecipe("hbm:gun_amat_penance", 1,
                i("#c:ingots/starmetal"), i("#c:plates_cast/dura_steel"), i("#c:ingots/starmetal"),
                i(SILENCER), i("hbm:gun_amat"), i(FURNITURE_BLACK),
                i("#c:ingots/starmetal"), i("#c:plates_cast/dura_steel"), i("#c:ingots/starmetal")));

        register(new PedestalRecipe("hbm:gun_flamer_daybreaker", 1,
                i("#c:plates_cast/gold"), i("hbm:canned_conserve_slime"), i("#c:plates_cast/gold"),
                i("#c:ingots/phosphorus"), i("hbm:gun_flamer"), i("#c:ingots/phosphorus"),
                i("#c:plates_cast/gold"), i("hbm:stick_dynamite"), i("#c:plates_cast/gold"))
                .extra(PedestalExtraCondition.SUN));

        register(new PedestalRecipe("hbm:gun_autoshotgun_sexy", 1,
                i("hbm:bolt_spike", 16), i("hbm:wild_p"), i("hbm:bolt_spike", 16),
                i("hbm:card_qos"), i("hbm:gun_autoshotgun"), i("hbm:card_aos"),
                i("hbm:bolt_spike", 16), i("#c:ingots/starmetal", 16), i("hbm:bolt_spike", 16)));

        register(new PedestalRecipe("hbm:gun_minigun_lacunae", 1,
                null, i("hbm:powder_magic", 4), null,
                i("hbm:item_secret_selenium_steel", 4), i("hbm:gun_minigun"), i("hbm:item_secret_selenium_steel", 4),
                null, i("hbm:powder_magic", 4), null)
                .extra(PedestalExtraCondition.FULL_MOON));

        register(new PedestalRecipe("hbm:gun_laser_pistol_morning_glory", 1,
                null, i("hbm:morning_glory"), null,
                i("hbm:item_secret_selenium_steel", 2), i("hbm:gun_laser_pistol"), i("hbm:item_secret_selenium_steel", 2),
                null, i("#c:gems/emerald", 16), null));

        register(new PedestalRecipe("hbm:gun_folly", 1,
                i("hbm:item_secret_folly", 4), i("hbm:item_secret_controller", 2), i("hbm:item_secret_folly", 4),
                i("#c:ingots/bscco", 16), i("#c:storage_blocks/starmetal", 64), i("#c:ingots/bscco", 16),
                i("hbm:item_secret_folly", 4), i("hbm:item_secret_controller", 2), i("hbm:item_secret_folly", 4))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));

        register(new PedestalRecipe("hbm:gun_aberrator", 1,
                null, i("hbm:item_secret_aberrator"), null,
                i("hbm:item_secret_aberrator"), i("#c:mechanisms/saturnite", 4), i("hbm:item_secret_aberrator"),
                null, i("hbm:item_secret_aberrator"), null).set(1));
        register(new PedestalRecipe("hbm:gun_aberrator_eott", 1,
                i("hbm:item_secret_aberrator"), i("hbm:item_secret_aberrator"), i("hbm:item_secret_aberrator"),
                i("hbm:item_secret_aberrator"), i("#c:mechanisms/saturnite", 16), i("hbm:item_secret_aberrator"),
                i("hbm:item_secret_aberrator"), i("hbm:item_secret_aberrator"), i("hbm:item_secret_aberrator"))
                .extra(PedestalExtraCondition.GOOD_KARMA).set(1));

        register(new PedestalRecipe("hbm:ammo_secret_folly_sm", 1,
                i("#c:ingots/starmetal"), i("hbm:powder_magic"), i("#c:ingots/starmetal"),
                i("hbm:powder_magic"), i("hbm:chunk_ore_moonstone"), i("hbm:powder_magic"),
                i("#c:ingots/starmetal"), i("hbm:powder_magic"), i("#c:ingots/starmetal"))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));
        register(new PedestalRecipe("hbm:ammo_secret_folly_nuke", 1,
                i("#c:ingots/starmetal"), i("hbm:powder_magic"), i("#c:ingots/starmetal"),
                i("hbm:powder_magic"), i("hbm:ammo_standard_nuke_high", 4), i("hbm:powder_magic"),
                i("#c:ingots/starmetal"), i("hbm:powder_magic"), i("#c:ingots/starmetal"))
                .extra(PedestalExtraCondition.FULL_MOON).set(1));
        register(new PedestalRecipe("hbm:ammo_secret_p35_800", 5,
                null, null, null,
                null, i("hbm:item_secret_aberrator"), null,
                null, null, null).set(1));
        register(new PedestalRecipe("hbm:ammo_secret_p35_800_bl", 10,
                null, null, null,
                null, i("hbm:item_secret_aberrator", 3), null,
                null, null, null).set(1));
    }

    /**
     * 1.7 {@code AStack.matchesRecipe(stack, true)} plus the pedestal's exact-count check: the item (or tag, or
     * item and variant) must match and the stack must hold exactly the ingredient's count.
     */
    public static boolean matches(Input input, net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() != input.count()) return false;
        String id = input.id();
        if (id.startsWith("#")) {
            return stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    dev.golem.core.Ids.parse(id.substring(1))));
        }
        int variant = -1;
        int at = id.indexOf('@');
        if (at > 0) {
            variant = Integer.parseInt(id.substring(at + 1));
            id = id.substring(0, at);
        }
        if (!dev.golem.core.world.ItemStacks.matchesItemId(stack, id)) return false;
        return variant < 0 || com.hbm.items.ModDataComponents.ITEM_VARIANT.getOr(stack, 0) == variant;
    }

    /** The recipe's product, or empty if its item is not registered in this port. */
    public static net.minecraft.world.item.ItemStack output(PedestalRecipe recipe) {
        net.minecraft.resources.Identifier id = dev.golem.core.Ids.parse(recipe.output);
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)) return net.minecraft.world.item.ItemStack.EMPTY;
        return new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id),
                recipe.outputCount);
    }

    public static void register(PedestalRecipe recipe) {
        recipes.add(recipe);
        int set = Math.abs(recipe.recipeSet) % recipeSets.length;
        recipeSets[set].add(recipe);
    }
}
