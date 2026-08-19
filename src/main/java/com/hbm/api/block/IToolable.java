package com.hbm.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A block that reacts to being worked on with one of HBM's construction tools — the 1.12.2
 * {@code IToolable}. The canonical use is in-world component upgrading: hit a plain casing with the
 * boltgun (spending bolts) and it becomes the bolted casing that a reactor structure actually accepts.
 *
 * <p>Ported 1:1 apart from the hit-position parameters, which are folded into a single {@link Vec3}
 * since 26.1 hands the tool a {@code UseOnContext} rather than three loose floats.
 */
public interface IToolable {

    /** @return true if the tool did something (and should therefore charge the player for it). */
    boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool);

    enum ToolType {
        SCREWDRIVER,
        HAND_DRILL,
        DEFUSER,
        WRENCH,
        TORCH,
        BOLT;

        public static final ToolType[] VALUES = values();

        /** Items that count as this tool. */
        public final List<Supplier<? extends Item>> tools = new ArrayList<>();

        private static @Nullable Map<Item, ToolType> byItem;

        /**
         * Registered by {@link Supplier}, not {@code Item} or {@code ItemStack}: tools are declared
         * during bootstrap, before the item registry is flushed, and building an {@code ItemStack}
         * before data components are bound throws outright (IMPLEMENTATION.md §22).
         */
        public void register(Supplier<? extends Item> item) {
            tools.add(item);
            byItem = null;
        }

        /** Representative stacks for the "requires" overlay; runtime only. */
        public List<ItemStack> stacksForDisplay() {
            List<ItemStack> stacks = new ArrayList<>();
            for (Supplier<? extends Item> item : tools) stacks.add(new ItemStack(item.get()));
            return stacks;
        }

        public static @Nullable ToolType getType(ItemStack stack) {
            if (stack.isEmpty()) return null;
            Map<Item, ToolType> map = byItem;
            if (map == null) {
                map = new HashMap<>();
                for (ToolType type : VALUES) {
                    for (Supplier<? extends Item> item : type.tools) map.put(item.get(), type);
                }
                byItem = map;
            }
            return map.get(stack.getItem());
        }
    }
}
