package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;

/**
 * The JSON form of molten material (1.7 {@code Mats.MaterialStack}: a material and an amount in quanta, 72 to the
 * ingot), shared by every recipe table that melts or casts: {@code {"material": "iron", "amount": 144}}. A material is
 * named by its {@link NTMMaterial#name} (ntm-next's {@code NTMMaterial.CODEC} key), never by its 1.7 number, so a
 * datapack row reads the same on every node and in ntm-next.
 */
public final class MaterialCodecs {

    private MaterialCodecs() {}

    /** A material by name ({@code "steel"}); aliases registered with {@link Mats#alias} are accepted. */
    public static final Codec<NTMMaterial> MATERIAL = Codec.STRING.comapFlatMap(name -> {
        NTMMaterial material = Mats.byName(name);
        return material == null ? DataResult.error(() -> "unknown material " + name) : DataResult.success(material);
    }, material -> material.name);

    /** {@code {"material", "amount"}}; the amount is in quanta and must be positive. */
    public static final Codec<Mats.MaterialStack> STACK = RecordCodecBuilder.<Mats.MaterialStack>create(i -> i.group(
            MATERIAL.fieldOf("material").forGetter(stack -> stack.material),
            Codec.INT.fieldOf("amount").forGetter(stack -> stack.amount)
    ).apply(i, Mats.MaterialStack::new)).flatXmap(MaterialCodecs::checkStack, MaterialCodecs::checkStack);

    /** A list of molten stacks, as an array (the form the machines read). */
    public static final Codec<Mats.MaterialStack[]> STACKS = STACK.listOf().xmap(
            list -> list.toArray(Mats.MaterialStack[]::new), Arrays::asList);

    /** One line per stack ({@code iron:144}), for comparing a converted table with its source. */
    public static String describe(Mats.MaterialStack stack) {
        return stack == null ? "-" : stack.material.name + ":" + stack.amount;
    }

    public static String describe(Mats.MaterialStack[] stacks) {
        return stacks == null ? "-" : describe(Arrays.asList(stacks));
    }

    public static String describe(List<Mats.MaterialStack> stacks) {
        StringBuilder out = new StringBuilder("[");
        for (Mats.MaterialStack stack : stacks) out.append(describe(stack)).append(',');
        return out.append(']').toString();
    }

    /** Rejects a row the machine cannot run (MapCodec.validate is missing from 1.20.1's DataFixerUpper). */
    private static DataResult<Mats.MaterialStack> checkStack(Mats.MaterialStack stack) {
        return stack.amount > 0 ? DataResult.success(stack) : DataResult.error(() -> "material amount must be positive: " + stack.material.name + " " + stack.amount);
    }
}
