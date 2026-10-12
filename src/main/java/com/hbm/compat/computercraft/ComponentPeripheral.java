package com.hbm.compat.computercraft;

//? if computercraft {
import com.hbm.api.computer.ComputerComponents;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Another mod's component ({@link ComputerComponents}) as a CC: Tweaked peripheral: its methods by name, each run on
 * the server thread ({@code executeMainThreadTask}), an {@link IllegalArgumentException} becoming the Lua error. It
 * keeps no snapshot: every method reaches the live block entity on the server thread.
 */
final class ComponentPeripheral extends SnapshotPeripheral<BlockEntity> implements IDynamicPeripheral {

    private final ComputerComponents.Component<?> component;
    private final String[] names;

    ComponentPeripheral(ComputerComponents.Component<?> component, BlockEntity machine) {
        super(machine, component.name(), 0, 0);
        this.component = component;
        this.names = component.methods().keySet().toArray(new String[0]);
    }

    @Override
    protected void capture(BlockEntity machine) {}

    @Override
    public String[] getMethodNames() {
        return names.clone();
    }

    @Override
    public MethodResult callMethod(IComputerAccess computer, ILuaContext context, int method, IArguments arguments)
            throws LuaException {
        if (method < 0 || method >= names.length) throw new LuaException("No such method");
        String name = names[method];
        Object[] args = arguments.getAll();
        return context.executeMainThreadTask(() -> {
            try {
                return component.call(machine(), name, args);
            } catch (IllegalArgumentException e) {
                throw new LuaException(e.getMessage());
            }
        });
    }
}
//?}
