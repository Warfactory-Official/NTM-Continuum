package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** A plasma-forge operation with the additional plasma-energy ignition requirement. */
public class PlasmaForgeRecipe extends GenericRecipe {

    public long ignitionTemp;

    public PlasmaForgeRecipe(String name) {
        super(name);
    }

    public PlasmaForgeRecipe setInputEnergy(long ignitionTemp) {
        this.ignitionTemp = ignitionTemp;
        return this;
    }

    /** The row codec of the plasma forge table: the generic fields plus {@code ignition_temp} (ntm-next's name). */
    public static MapCodec<PlasmaForgeRecipe> codec() {
        return GenericRecipeCodec.codec(PlasmaForgeRecipe::new, Codec.LONG.optionalFieldOf("ignition_temp", 0L),
                recipe -> recipe.ignitionTemp, PlasmaForgeRecipe::setInputEnergy);
    }

    @Override
    public String describeExtra() {
        return "ignition=" + ignitionTemp;
    }

    @Override
    public List<Component> print() {
        List<Component> list = new ArrayList<>();
        header(list);
        autoSwitch(list);
        duration(list);
        power(list);
        list.add(Component.translatable("gui.recipe.plasmaIn")
                .append(": " + BobMathUtil.getShortNumber(ignitionTemp) + "TU/t")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        input(list);
        output(list);
        return list;
    }
}
