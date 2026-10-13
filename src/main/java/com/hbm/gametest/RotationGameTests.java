package com.hbm.gametest;

import com.hbm.Constants;
import dev.golem.core.dejson.BlockStates;
import java.lang.reflect.Method;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks that a block carrying orientation has <em>said</em> what a rotation does to it.
 *
 * <p>{@code Block.rotate} defaults to returning the state unchanged. A mod block that does not override it is
 * indistinguishable from one that decided not to turn, and the difference shows up only when a structure template
 * is placed at a rotation: a cable whose arms face the authored direction while its neighbours face the placed one
 * is visibly broken and its network never forms.
 *
 * <p>So the assertion is not "every directional block turns" but "every directional block states what it does":
 * did any class between this block and {@code Block} declare {@code rotate}? A stated no-op passes
 * ({@code RBMKBase} returns the state unchanged because an RBMK column is symmetric). Silence fails.
 *
 * <p>What counts as orientation is <em>measured, not listed</em>. Every vanilla block is driven through its own
 * {@code rotate}, and the property objects that move are what this version considers directional; any property
 * whose values are {@link Direction}s counts too. Identity rather than name matters: a chest's {@code type}
 * rotates and a slab's {@code type} does not.
 *
 * <p>The blind spot is a direction encoded as something else (an integer, a set of booleans vanilla does not also
 * use), which nothing can measure.
 */
public final class RotationGameTests {

    private RotationGameTests() {}

    public static void everyDirectionalBlockStatesItsRotation(GameTestHelper helper) {
        Set<Property<?>> directional = learnFromVanilla();
        helper.assertTrue(directional.size() > 5,
                "the vanilla sweep found only " + directional.size()
                        + " directional properties; the measurement is broken");

        List<String> problems = new ArrayList<>();
        int carrying = 0;
        for (Block block : BlockStates.blocksIn(Constants.MOD_ID)) {
            Set<String> carried = new LinkedHashSet<>();
            for (Property<?> property : block.defaultBlockState().getProperties()) {
                if (directional.contains(property) || isDirection(property)) {
                    carried.add(property.getName());
                }
            }
            if (carried.isEmpty()) {
                continue;
            }
            carrying++;
            if (!statesRotation(block)) {
                problems.add(block.getName().getString() + " carries " + carried
                        + " and no class between it and Block declares rotate");
            }
        }

        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        Constants.LOG.info("[ROTATION] {} directional properties measured from vanilla; "
                        + "{} hbm blocks carry one and every one of them states its rotation",
                directional.size(), carrying);
        helper.succeed();
    }

    /** The property objects some vanilla block's own rotate moves. */
    private static Set<Property<?>> learnFromVanilla() {
        Set<Property<?>> directional = new LinkedHashSet<>();
        for (Block block : BlockStates.blocksIn("minecraft")) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
                if (rotated == state) {
                    continue;
                }
                for (Property<?> property : state.getProperties()) {
                    if (!state.getValue(property).equals(rotated.getValue(property))) {
                        directional.add(property);
                    }
                }
            }
        }
        return directional;
    }

    private static boolean isDirection(Property<?> property) {
        for (Object value : property.getPossibleValues()) {
            return value instanceof Direction;
        }
        return false;
    }

    /**
     * Whether any class between this block and {@code Block} declares {@code rotate}.
     *
     * <p>Reflection on a vanilla method name, which is safe here because GameTests only run against
     * a development runtime, where the names are the mapped ones on every target.
     */
    private static boolean statesRotation(Block block) {
        for (Class<?> type = block.getClass(); type != null && type != Block.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("rotate") && method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == BlockState.class
                        && method.getParameterTypes()[1] == Rotation.class) {
                    return true;
                }
            }
        }
        return false;
    }
}
