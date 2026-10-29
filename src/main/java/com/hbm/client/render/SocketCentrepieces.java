package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Authored socket centrepieces replace the generic item body and share its corner arcs.
 */
public final class SocketCentrepieces {
    private static final Map<Item, Centrepiece> PIECES = new IdentityHashMap<>();

    private SocketCentrepieces() {
    }

    public static void register(Item item, Centrepiece centrepiece) {
        if (PIECES.putIfAbsent(item, centrepiece) != null) {
            throw new IllegalStateException("Socket centrepiece already registered for " + item);
        }
    }

    public static @Nullable Centrepiece of(ItemStack stack) {
        return PIECES.get(stack.getItem());
    }

    public record Centrepiece(Supplier<WavefrontObj> model, String part, Identifier texture,
                              float scale, float degreesPerTick) {
    }
}
