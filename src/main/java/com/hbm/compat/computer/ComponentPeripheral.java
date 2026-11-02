package com.hbm.compat.computer;

import com.hbm.api.computer.ComputerComponents;
import dev.golem.core.computer.ComponentMethods;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Another mod's component ({@link ComputerComponents}): its methods by name, each run on the server thread, an
 * {@link IllegalArgumentException} becoming the Lua error. It keeps no snapshot: every method reaches the live block
 * entity on the server thread.
 */
final class ComponentPeripheral extends SnapshotPeripheral<BlockEntity> {

    private final ComponentMethods methods;

    ComponentPeripheral(ComputerComponents.Component<?> component, BlockEntity machine) {
        super(machine, component.name(), 0, 0);
        Map<String, ComponentMethods.Invoker> invokers = new LinkedHashMap<>();
        for (String name : component.methods().keySet()) {
            invokers.put(name, (self, arguments) -> component.call(machine(), name, arguments));
        }
        this.methods = ComponentMethods.dynamic(invokers);
    }

    @Override
    protected void capture(BlockEntity machine) {}

    @Override
    public ComponentMethods methods() {
        return methods;
    }
}
