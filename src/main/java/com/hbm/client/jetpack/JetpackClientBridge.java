package com.hbm.client.jetpack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Client-only adapter for the common jetpack state owned by the gameplay side.
 *
 * <p>The client renderer must not make the common {@code JetpackHandler} load client classes. The
 * bootstrap installs a provider after both sides' classes exist; until then all visual queries are safe
 * no-ops. This is also the seam for the gameplay agent's UUID/state map and avoids copying state into a
 * second client-owned authority.
 */
public final class JetpackClientBridge {

    private JetpackClientBridge() {}

    @FunctionalInterface
    public interface StateProvider {
        @Nullable State state(Player player);
    }

    @FunctionalInterface
    public interface FuelProvider {
        @Nullable Fuel fuel(ItemStack stack);
    }

    /** The synchronized fields plus the four client interpolation values the renderer needs. */
    public interface State {
        boolean opening();
        boolean hover();
        float thrust();
        float prevThrust();
        int jetpackFlyTime();
        int prevJetpackFlyTime();
        int failureTicks();
        boolean useCompactHUD();
        long animTime();

        default boolean active() {
            return opening() && System.currentTimeMillis() - animTime() > 1000L;
        }
    }

    /** Fuel data used for the HUD and glider exhaust tint. */
    public record Fuel(String type, int amount, int capacity) {}

    private static StateProvider states = player -> null;
    private static FuelProvider fuels = stack -> null;

    public static void install(StateProvider provider, FuelProvider fuelProvider) {
        states = provider == null ? player -> null : provider;
        fuels = fuelProvider == null ? stack -> null : fuelProvider;
    }

    @Nullable
    public static State state(Player player) {
        return player == null ? null : states.state(player);
    }

    @Nullable
    public static Fuel fuel(ItemStack stack) {
        return stack == null ? null : fuels.fuel(stack);
    }
}
