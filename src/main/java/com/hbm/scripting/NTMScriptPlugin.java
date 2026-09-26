package com.hbm.scripting;

import com.hbm.api.content.Spec;
import dev.golem.core.script.GolemScriptPlugin;
import net.minecraft.world.item.ItemStack;

/** Named by {@code kubejs.plugins.txt}. */
public final class NTMScriptPlugin extends GolemScriptPlugin {

    @Override
    protected void declare() {
        bindStartup("NTMEvents", NTMEvents::fresh);
        bindServer("NTMRecipes", () -> NTMRecipes.INSTANCE);
        bindClient("NTMRecipes", () -> NTMRecipes.INSTANCE);
        bindServer("NTMServerEvents", NTMServerEvents::fresh);
        typeWrapper(Spec.Item.class, Spec.Item::new, String.class, ItemStack.class);
        typeWrapper(Spec.Ingredient.class, Spec.Ingredient::new, String.class, ItemStack.class);
        typeWrapper(Spec.Fluid.class, Spec.Fluid::new, String.class);
        typeWrapper(Spec.Molten.class, Spec.Molten::new, String.class);
        onStartupScriptsLoaded(ScriptedContent::scriptsLoaded);
        assets(ScriptedContent::generateAssets);
        data(ScriptedContent::generateData);
        lang(ScriptedContent::generateLang);
    }
}
