package com.hbm.inventory.recipes;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Vanilla custom-recipe serializers. Call {@link #init()} during common bootstrap before registry flush. */
public final class ModRecipes {

    private ModRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Constants.MOD_ID);

    public static final RegistryObject<RecipeSerializer<RBMKFuelDisassemblyRecipe>> RBMK_FUEL_DISASSEMBLY =
            SERIALIZERS.register("rbmk_fuel_disassembly", () -> RBMKFuelDisassemblyRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<ComponentCopyShapelessRecipe>> COMPONENT_COPY_SHAPELESS =
            SERIALIZERS.register("component_copy_shapeless", () -> ComponentCopyShapelessRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<dev.golem.core.world.ComponentShapedRecipe>> COMPONENT_SHAPED =
            SERIALIZERS.register("component_shaped", () -> dev.golem.core.world.ComponentShapedRecipe.SERIALIZER);

    public static void init() {}
}
