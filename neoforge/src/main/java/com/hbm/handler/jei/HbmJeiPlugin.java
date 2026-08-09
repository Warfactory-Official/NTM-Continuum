package com.hbm.handler.jei;

import com.hbm.Constants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IExtraIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;

/**
 * NeoForge {@code @JeiPlugin} entrypoint. Thin by design — all category/recipe/catalyst logic lives in
 * {@link HbmJeiPluginCommon} (compiled against {@code jei-*-common-api}), so both loaders share it and only
 * the annotated entrypoint is per-module (JEI discovers {@code @JeiPlugin} by classpath annotation scan,
 * and the loader APIs differ). Mirrors the {@code HbmClient}/{@code HbmClientNeoForge} split.
 */
@JeiPlugin
public class HbmJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "jei_plugin");

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
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        HbmJeiPluginCommon.registerCatalysts(registration);
    }
}
