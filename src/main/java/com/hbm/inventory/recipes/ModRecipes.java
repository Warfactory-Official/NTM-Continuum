package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.FluidContainerRegistry;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ComponentShapedRecipe;
import dev.golem.core.world.ComponentSmeltingRecipe;
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

    public static final RegistryObject<RecipeSerializer<ComponentShapedRecipe>> COMPONENT_SHAPED =
            REG.register(Registries.RECIPE_SERIALIZER, "component_shaped", () -> ComponentShapedRecipe.SERIALIZER);

    public static final RegistryObject<RecipeSerializer<ComponentSmeltingRecipe>> COMPONENT_SMELTING =
            REG.register(Registries.RECIPE_SERIALIZER, "component_smelting", () -> ComponentSmeltingRecipe.SERIALIZER);

    /** 1.7 ShapelessOreRecipe with metadata/NBT, tag and getDict fluid-container ingredients. */
    public static final RegistryObject<RecipeSerializer<ComponentShapelessRecipe>> COMPONENT_SHAPELESS =
            REG.register(Registries.RECIPE_SERIALIZER, "component_shapeless", () -> ComponentShapelessRecipe.SERIALIZER);

    // 1.7 IRecipe handlers and the Java-decided crafting recipes
    public static final RegistryObject<RecipeSerializer<GrenadeCraftingRecipe>> GRENADE_UNIVERSAL =
            REG.register(Registries.RECIPE_SERIALIZER, "grenade_universal", () -> GrenadeCraftingRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<CargoShellCraftingRecipe>> AMMO_ARTY_CARGO =
            REG.register(Registries.RECIPE_SERIALIZER, "ammo_arty_cargo", () -> CargoShellCraftingRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<ScrapsCraftingRecipe>> SCRAPS_SPLIT =
            REG.register(Registries.RECIPE_SERIALIZER, "scraps_split", () -> ScrapsCraftingRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<ScrapsPowderRecipe>> SCRAPS_FROM_POWDERS =
            REG.register(Registries.RECIPE_SERIALIZER, "scraps_from_powders", () -> ScrapsPowderRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<MKUCraftingRecipe>> SYRINGE_MKUNICORN =
            REG.register(Registries.RECIPE_SERIALIZER, "syringe_mkunicorn", () -> MKUCraftingRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<ZirnoxWasteRecipe>> ZIRNOX_WASTE =
            REG.register(Registries.RECIPE_SERIALIZER, "zirnox_waste", () -> ZirnoxWasteRecipe.SERIALIZER);
    public static final RegistryObject<RecipeSerializer<FluidDuctRetypeRecipe>> FLUID_DUCT_RETYPE =
            REG.register(Registries.RECIPE_SERIALIZER, "fluid_duct_retype", () -> FluidDuctRetypeRecipe.SERIALIZER);

    public static void init() {
        // the container<amount><fluid> dictionary behind every fluid ingredient
        FluidContainerRegistry.register();
    }
}
