package com.hbm.gametest;

import com.hbm.interfaces.ILookOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;
import java.util.List;

/** Dedicated-server contract checks for all block look-overlay implementations. */
public final class LookOverlayGameTests {

    private static final List<String> OVERLAY_BLOCKS = List.of(
            "com.hbm.blocks.network.FluidValve",
            "com.hbm.blocks.network.BlockFluidDuct",
            "com.hbm.blocks.generic.BlockToolConvertible",
            "com.hbm.blocks.machine.BlockMachineBattery",
            "com.hbm.blocks.machine.FoundryOutlet",
            "com.hbm.blocks.machine.FoundryCastingBase",
            "com.hbm.blocks.machine.HeaterFirebox",
            "com.hbm.blocks.machine.HeaterElectric",
            "com.hbm.blocks.machine.HeaterOilburner",
            "com.hbm.blocks.machine.MachineCatalyticCracker",
            "com.hbm.blocks.machine.MachineCondenser",
            "com.hbm.blocks.machine.MachineDeuteriumExtractor",
            "com.hbm.blocks.machine.MachineDeuteriumTower",
            "com.hbm.blocks.machine.MachineDrain",
            "com.hbm.blocks.machine.MachineFractionTower",
            "com.hbm.blocks.machine.MachineHeatBoiler",
            "com.hbm.blocks.machine.MachineHeatBoilerIndustrial",
            "com.hbm.blocks.machine.MachineIndustrialTurbine",
            "com.hbm.blocks.machine.MachinePump",
            "com.hbm.blocks.machine.MachineSawmill",
            "com.hbm.blocks.machine.MachineSteamEngine",
            "com.hbm.blocks.machine.MachineStirling",
            "com.hbm.blocks.machine.MachineTurbineGas",
            "com.hbm.blocks.machine.fusion.MachineFusionBoiler",
            "com.hbm.blocks.machine.fusion.MachineFusionBreeder",
            "com.hbm.blocks.machine.fusion.MachineFusionMHDT",
            "com.hbm.blocks.machine.storage.MachineBatterySocket",
            "com.hbm.blocks.machine.storage.MachineFENSU");

    private LookOverlayGameTests() {}

    /** Loads every server-registered producer and checks that its method descriptors stay client-free. */
    public static void blockClassesAreDedicatedServerSafe(GameTestHelper helper) {
        try {
            for (Method method : ILookOverlay.class.getDeclaredMethods()) {
                assertServerType(helper, method.getReturnType(), "ILookOverlay." + method.getName());
                for (Class<?> parameter : method.getParameterTypes()) {
                    assertServerType(helper, parameter, "ILookOverlay." + method.getName());
                }
            }

            for (String name : OVERLAY_BLOCKS) {
                Class<?> block = Class.forName(name, false, ILookOverlay.class.getClassLoader());
                helper.assertTrue(ILookOverlay.class.isAssignableFrom(block),
                        name + " no longer implements ILookOverlay");
                Method method = block.getDeclaredMethod("getLookOverlay", Level.class, BlockPos.class, Player.class);
                assertServerType(helper, method.getReturnType(), name + ".getLookOverlay");
                for (Class<?> parameter : method.getParameterTypes()) {
                    assertServerType(helper, parameter, name + ".getLookOverlay");
                }
            }
            helper.succeed();
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.fail("Look-overlay server classloading failed: " + exception);
        }
    }

    private static void assertServerType(GameTestHelper helper, Class<?> type, String owner) {
        String name = type.getName();
        helper.assertFalse(name.startsWith("net.minecraft.client.") || name.startsWith("com.hbm.client."),
                owner + " references client-only type " + name);
    }
}
