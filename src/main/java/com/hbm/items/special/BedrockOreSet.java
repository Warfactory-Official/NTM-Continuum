package com.hbm.items.special;

import com.hbm.api.registry.NamedRegistry;
import com.hbm.blocks.ModOreBlocks;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** The immutable ordered ore kinds carried by a raw sample and paid out by the ore slopper. */
public record BedrockOreSet(String id, List<BedrockOreType> kinds, @Nullable Component origin) {
    public static final NamedRegistry<BedrockOreSet> SETS = new NamedRegistry<>("bedrock ore set", BedrockOreSet::id);
    public static final BedrockOreSet NTM = SETS.register(new BedrockOreSet("hbm", List.of(BedrockOreType.VALUES), null));
    public static final AtomicReference<Function<Level, BedrockOreSet>> OF = new AtomicReference<>(level -> NTM);
    public static final AtomicReference<Supplier<List<ItemStack>>> SAMPLES = new AtomicReference<>(
            () -> List.of(new ItemStack(ModOreBlocks.bedrock_ore_base.get())));
    public BedrockOreSet {
        kinds = List.copyOf(kinds);
    }
    public int indexOf(BedrockOreType kind) { return kinds.indexOf(kind); }
}
