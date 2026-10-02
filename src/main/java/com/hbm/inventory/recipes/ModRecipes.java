package com.hbm.inventory.recipes;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import com.hbm.registration.Reg;

/** Vanilla custom-recipe serializers. Call {@link #init()} during common bootstrap before registry flush. */
public final class ModRecipes {

    private ModRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Constants.MOD_ID);
    private static final Reg REG = Reg.of(SERIALIZERS);

    public static final RegistryObject<RecipeSerializer<RBMKFuelDisassemblyRecipe>> RBMK_FUEL_DISASSEMBLY =
            REG.register(Registries.RECIPE_SERIALIZER, "rbmk_fuel_disassembly", () -> RBMKFuelDisassemblyRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<ComponentCopyShapelessRecipe>> COMPONENT_COPY_SHAPELESS =
            REG.register(Registries.RECIPE_SERIALIZER, "component_copy_shapeless", () -> ComponentCopyShapelessRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<dev.golem.core.world.ComponentShapedRecipe>> COMPONENT_SHAPED =
            REG.register(Registries.RECIPE_SERIALIZER, "component_shaped", () -> dev.golem.core.world.ComponentShapedRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<dev.golem.core.world.ComponentSmeltingRecipe>> COMPONENT_SMELTING =
            REG.register(Registries.RECIPE_SERIALIZER, "component_smelting", () -> dev.golem.core.world.ComponentSmeltingRecipe.SERIALIZER);

    /** 1.7 ShapelessOreRecipe with metadata/NBT, tag and getDict fluid-container ingredients. */
    public static final RegistryObject<RecipeSerializer<ComponentShapelessRecipe>> COMPONENT_SHAPELESS =
            REG.register(Registries.RECIPE_SERIALIZER, "component_shapeless", () -> ComponentShapelessRecipe.SERIALIZER);

    public static void init() {
        // the container<amount><fluid> dictionary behind every fluid ingredient
        com.hbm.inventory.FluidContainerRegistry.register();
    }
}
