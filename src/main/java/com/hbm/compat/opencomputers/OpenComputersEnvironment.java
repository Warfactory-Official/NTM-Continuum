package com.hbm.compat.opencomputers;

//? if opencomputers {
/*import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/^*
 * Bridges an HBM {@link SnapshotPeripheral} into an OpenComputers component environment.
 *
 * <p>Implements:
 * <ul>
 *   <li>{@link SidedEnvironment}: connects to OpenComputers cables natively without an adapter block,
 *       respecting HBM's per-machine reach rules (ports, folded multiblock cells, turret bases).</li>
 *   <li>{@link ManagedPeripheral}: exposes the peripheral's Lua functions and dispatches calls.</li>
 *   <li>{@link NamedBlock}: reports the component name matching 1.7 / 1.12.2 HBM (e.g. {@code ntm_geiger},
 *       {@code breeding_reactor}, {@code fusion_torus}).</li>
 * </ul>
 ^/
public final class OpenComputersEnvironment extends AbstractManagedEnvironment
        implements SidedEnvironment, ManagedPeripheral, NamedBlock {

    private final BlockEntity target;
    private final SnapshotPeripheral<?> peripheral;
    private final String componentName;
    private final Map<String, Method> methods = new HashMap<>();
    private final String[] methodNames;

    public OpenComputersEnvironment(BlockEntity target, SnapshotPeripheral<?> peripheral) {
        this.target = target;
        this.peripheral = peripheral;
        this.componentName = peripheral.getType();
        setNode(Network.newNode(this, Visibility.Network).withComponent(this.componentName).create());

        List<String> names = new ArrayList<>();
        for (Method method : peripheral.getClass().getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
            LuaFunction annotation = method.getAnnotation(LuaFunction.class);
            if (annotation != null) {
                String[] exposed = annotation.value();
                if (exposed.length == 0) {
                    addMethod(method.getName(), method, names);
                } else {
                    for (String name : exposed) {
                        addMethod(name, method, names);
                    }
                }
            }
        }
        this.methodNames = names.toArray(new String[0]);
    }

    private void addMethod(String name, Method method, List<String> names) {
        if (methods.putIfAbsent(name, method) == null) {
            names.add(name);
        }
    }

    @Override
    public @Nullable Node sidedNode(Direction side) {
        Level level = target.getLevel();
        if (level instanceof ServerLevel server) {
            BlockPos pos = target.getBlockPos();
            if (OpenComputersPeripherals.canConnectAt(server, pos, side)) {
                return node();
            }
        }
        return null;
    }

    @Override
    public boolean canConnect(Direction side) {
        Level level = target.getLevel();
        if (level != null) {
            return OpenComputersPeripherals.canConnectAt(level, target.getBlockPos(), side);
        }
        return true;
    }

    @Override
    public String[] methods() {
        return methodNames.clone();
    }

    @Override
    public Object[] invoke(String method, Context context, Arguments args) throws Exception {
        Method m = methods.get(method);
        if (m == null) throw new NoSuchMethodException(method);

        Object[] invokeArgs = convertArguments(m, args, context);
        try {
            Object result = m.invoke(peripheral, invokeArgs);
            if (result instanceof Object[] arr) {
                return arr;
            } else if (result != null) {
                return new Object[]{result};
            } else {
                return new Object[0];
            }
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) throw ex;
            throw e;
        }
    }

    private Object[] convertArguments(Method method, Arguments args, Context context) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] out = new Object[paramTypes.length];
        int argIdx = 0;
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> pt = paramTypes[i];
            if (pt == Context.class) {
                out[i] = context;
            } else if (pt == Arguments.class) {
                out[i] = args;
            } else if (argIdx < args.count()) {
                out[i] = coerceArgument(args, argIdx++, pt);
            } else {
                out[i] = defaultValue(pt);
            }
        }
        return out;
    }

    private static Object coerceArgument(Arguments args, int index, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            return args.checkInteger(index);
        } else if (type == double.class || type == Double.class) {
            return args.checkDouble(index);
        } else if (type == boolean.class || type == Boolean.class) {
            return args.checkBoolean(index);
        } else if (type == String.class) {
            return args.checkString(index);
        } else if (type == byte.class || type == Byte.class) {
            return (byte) args.checkInteger(index);
        } else if (type == short.class || type == Short.class) {
            return (short) args.checkInteger(index);
        } else if (type == long.class || type == Long.class) {
            return (long) args.checkDouble(index);
        } else if (type == float.class || type == Float.class) {
            return (float) args.checkDouble(index);
        } else {
            return args.checkAny(index);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == double.class) return 0.0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }

    @Override
    public String preferredName() {
        return componentName;
    }

    @Override
    public int priority() {
        return 0;
    }

    public BlockEntity target() {
        return target;
    }

    public SnapshotPeripheral<?> peripheral() {
        return peripheral;
    }
}
*///?}
