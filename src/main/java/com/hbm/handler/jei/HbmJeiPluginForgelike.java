package com.hbm.handler.jei;

//? if jei && forgelike {

import com.hbm.Constants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;

/**
 * The Forge-like {@code @JeiPlugin} entrypoint, shared by NeoForge and Forge. Thin by design — all
 * category/recipe/catalyst logic lives in {@link HbmJeiPluginCommon} (compiled against
 * {@code jei-*-common-api}), so every loader shares it and only the annotated entrypoint is per-module.
 *
 * <p>Fabric needs its own copy ({@link HbmJeiPluginFabric}) because JEI discovers plugins differently
 * there — a {@code jei_mod_plugin} entrypoint in {@code fabric.mod.json} rather than an annotation
 * scan — but NeoForge and Forge discover this class the same way and need nothing loader-specific in
 * it, so widening the guard beats keeping a third identical copy.
 *
 * <p>On Forge 1.20.1 the class also opts out of "REI Plugin Compatibilities", which runs JEI plugins inside REI when
 * JEI is absent: HBM has a native REI plugin ({@code com.hbm.handler.rei}), and both would list every category twice.
 * That mod exists for no later target.
 */
//? if forge {
/*@me.shedaniel.rei.plugincompatibilities.api.REIPluginCompatIgnore
*///?}
@JeiPlugin
public class HbmJeiPluginForgelike implements IModPlugin {

    private static final Identifier UID = dev.golem.core.Ids.of(Constants.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        HbmJeiPluginCommon.registerItemSubtypes(registration);
    }

    @Override
    public void registerExtraIngredients(IExtraIngredientRegistration registration) {
        HbmJeiPluginCommon.registerExtraIngredients(registration);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        HbmJeiPluginCommon.registerCategories(registration);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        HbmJeiPluginCommon.registerRecipes(registration);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        HbmJeiPluginCommon.registerGuiHandlers(registration);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        HbmJeiPluginCommon.registerRecipeTransferHandlers(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        HbmJeiPluginCommon.registerCatalysts(registration);
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        HbmJeiPluginCommon.onRuntimeAvailable(runtime);
    }
}
//?}
