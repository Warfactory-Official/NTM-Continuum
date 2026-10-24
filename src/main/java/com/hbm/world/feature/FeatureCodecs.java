package com.hbm.world.feature;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.golem.core.data.Codecs;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Shared pieces of the feature configurations. Every field is optional and falls back to the 1.7 constant, so a
 * configured feature written as {@code "config": {}} generates exactly what the constant did. Defaults are suppliers:
 * the codecs are built before HBM's blocks are registered.
 */
public final class FeatureCodecs {

    private FeatureCodecs() {}

    public static <T> MapCodec<T> lazy(Codec<T> codec, String name, Supplier<T> fallback) {
        return Codecs.strictOptionalField(codec, name).xmap(o -> o.orElseGet(fallback), Optional::of);
    }

    public static <T> MapCodec<T> field(Codec<T> codec, String name, T fallback) {
        return lazy(codec, name, () -> fallback);
    }

    private static final Codec<Integer> POSITIVE = Codec.intRange(1, Integer.MAX_VALUE);

    /** An integer used as a random bound: zero or less would fail at generation, so the pack load rejects it. */
    public static MapCodec<Integer> positive(String name, int fallback) {
        return lazy(POSITIVE, name, () -> fallback);
    }

    public static MapCodec<Integer> positive(String name, Supplier<Integer> fallback) {
        return lazy(POSITIVE, name, fallback);
    }

    /** Rejects a size range a random draw cannot take: {@code max_size} must exceed {@code min_size}. */
    public static <C> Codec<C> sizeRange(Codec<C> codec, ToIntFunction<C> min, ToIntFunction<C> max) {
        return codec.flatXmap(c -> max.applyAsInt(c) > min.applyAsInt(c) ? DataResult.success(c)
                : DataResult.error(() -> "max_size must be greater than min_size"), DataResult::success);
    }

    public static MapCodec<BlockState> state(String name, Supplier<? extends Block> fallback) {
        return lazy(BlockState.CODEC, name, () -> fallback.get().defaultBlockState());
    }

    public static MapCodec<RuleTest> rule(String name, Supplier<RuleTest> fallback) {
        return lazy(RuleTest.CODEC, name, fallback);
    }

    public static RuleTest tag(TagKey<Block> tag) {
        return new TagMatchTest(tag);
    }

    public static RuleTest block(Block block) {
        return new BlockMatchTest(block);
    }

    /** The configuration a configured feature gets from {@code "config": {}}. */
    public static <C> C defaults(Codec<C> codec) {
        return codec.parse(JsonOps.INSTANCE, new JsonObject()).result().orElseThrow();
    }
}
