package com.hbm.gametest.computer;

//? if computercraft {
import dev.golem.core.computer.ComputerComponent;
import dev.golem.core.computer.PeripheralExposure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Asks for peripherals the way CC does: through Core's {@link PeripheralExposure#find}, the loader's own lookup
 * (NeoForge capability, Fabric {@code PeripheralLookup}), so a test covers the registration and not only HBM's
 * resolver. CC hands out Core's peripheral wrapper; these return the HBM component behind it.
 */
public final class PeripheralTestSupport {

    private PeripheralTestSupport() {}

    public static @Nullable ComputerComponent find(GameTestHelper helper, BlockPos relative, @Nullable Direction side) {
        return at(helper.getLevel(), helper.absolutePos(relative), side);
    }

    public static @Nullable ComputerComponent at(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return PeripheralExposure.component(PeripheralExposure.find(level, pos, side));
    }

    /** The peripheral at {@code relative}, asserted to be a {@code type} with CC type name {@code name}. */
    public static <P extends ComputerComponent> P expect(GameTestHelper helper, BlockPos relative, @Nullable Direction side,
                                                   Class<P> type, String name) {
        ComputerComponent found = find(helper, relative, side);
        helper.assertTrue(found != null, "no peripheral at " + relative + " side " + side + ", expected " + name);
        helper.assertTrue(type.isInstance(found), "peripheral at " + relative + " is "
                + found.getClass().getSimpleName() + ", expected " + type.getSimpleName());
        helper.assertTrue(name.equals(found.type()), "peripheral type " + found.type() + ", expected " + name);
        return type.cast(found);
    }

    public static void expectNone(GameTestHelper helper, BlockPos relative, @Nullable Direction side) {
        ComputerComponent found = find(helper, relative, side);
        helper.assertTrue(found == null, "unexpected peripheral " + (found == null ? "" : found.type()) + " at "
                + relative + " side " + side);
    }

    /** {@code result[index]} as a number, failing on anything else. */
    public static double number(GameTestHelper helper, Object[] result, int index) {
        helper.assertTrue(result != null && result.length > index && result[index] instanceof Number,
                "result[" + index + "] is not a number: " + java.util.Arrays.toString(result));
        return ((Number) result[index]).doubleValue();
    }
}
//?}
