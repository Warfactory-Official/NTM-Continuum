package com.hbm.scripting;

import com.hbm.Constants;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.type.IHazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The {@code NTMServerEvents} server-script global. Each server-script load replaces every listener.
 *
 * <pre>
 * NTMServerEvents.meltdown(e => { if (e.kind == 'rbmk') e.cancel() })
 *     e.level e.pos e.kind: rbmk zirnox pwr watz research dfc
 * NTMServerEvents.detonation(e => ...)
 *     e.level e.x e.y e.z e.pos e.radius e.kind: mk5 fleija solinium balefire tom mini_nuke
 * NTMServerEvents.radiation(e => { e.amount = e.amount / 2 })
 *     e.entity e.neutron e.amount: dose before armour reduction, writable
 * NTMServerEvents.hazard(e => { if (e.type == 'hbm:hot') e.cancel() })
 *     e.entity e.stack e.item e.type e.amount: level after modifiers, writable
 * NTMServerEvents.debug('key', value)   NTMServerEvents.debug('key')   shared across reloads
 * </pre>
 *
 * Every event: {@code e.cancel()}, {@code e.cancelled}. A cancelled meltdown leaves the reactor as it was, so
 * it posts again next tick while the condition holds. A cancelled blast entity is discarded before it
 * touches the world; a mk5 blast restored from a save posts again. Server side only.
 */
public final class NTMServerEvents {

    private static volatile NTMServerEvents current = new NTMServerEvents();
    private static final Map<String, Object> DEBUG = new ConcurrentHashMap<>();

    private final List<Consumer<Meltdown>> meltdown = new CopyOnWriteArrayList<>();
    private final List<Consumer<Detonation>> detonation = new CopyOnWriteArrayList<>();
    private final List<Consumer<Radiation>> radiation = new CopyOnWriteArrayList<>();
    private final List<Consumer<Hazard>> hazard = new CopyOnWriteArrayList<>();
    private final Set<Consumer<?>> failed = ConcurrentHashMap.newKeySet();

    private NTMServerEvents() {}

    static NTMServerEvents fresh() {
        current = new NTMServerEvents();
        return current;
    }

    public static NTMServerEvents current() {
        return current;
    }

    public void meltdown(Consumer<Meltdown> listener) {
        meltdown.add(listener);
    }

    public void detonation(Consumer<Detonation> listener) {
        detonation.add(listener);
    }

    public void radiation(Consumer<Radiation> listener) {
        radiation.add(listener);
    }

    public void hazard(Consumer<Hazard> listener) {
        hazard.add(listener);
    }

    public void debug(String key, Object value) {
        if (value == null) DEBUG.remove(key);
        else DEBUG.put(key, value);
    }

    public Object debug(String key) {
        return DEBUG.get(key);
    }

    /** @return true when a listener cancelled the meltdown */
    public static boolean postMeltdown(Level level, BlockPos pos, String kind) {
        NTMServerEvents events = current;
        if (events.meltdown.isEmpty() || level.isClientSide()) return false;
        return events.post("meltdown", events.meltdown, new Meltdown(level, pos.immutable(), kind)).cancelled;
    }

    /** @return true when a listener cancelled the blast */
    public static boolean postDetonation(Level level, double x, double y, double z, String kind, double radius) {
        NTMServerEvents events = current;
        if (events.detonation.isEmpty() || level.isClientSide()) return false;
        return events.post("detonation", events.detonation, new Detonation(level, x, y, z, kind, radius)).cancelled;
    }

    /** @return the dose to apply, NaN when cancelled */
    public static double postRadiation(LivingEntity entity, double amount, boolean neutron) {
        NTMServerEvents events = current;
        if (events.radiation.isEmpty() || entity.level().isClientSide()) return amount;
        Radiation event = events.post("radiation", events.radiation, new Radiation(entity, amount, neutron));
        return event.cancelled ? Double.NaN : event.amount;
    }

    /** @return the level to apply, NaN when cancelled */
    public static double postHazard(LivingEntity entity, ItemStack stack, IHazardType type, double level) {
        NTMServerEvents events = current;
        if (events.hazard.isEmpty() || entity.level().isClientSide()) return level;
        Hazard event = events.post("hazard", events.hazard,
                new Hazard(entity, stack, String.valueOf(HazardRegistry.getTypeId(type)), level));
        return event.cancelled ? Double.NaN : event.amount;
    }

    private <E extends Event> E post(String name, List<Consumer<E>> listeners, E event) {
        for (Consumer<E> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                if (failed.add(listener)) {
                    Constants.LOG.error("[scripts] NTMServerEvents.{} listener failed; further failures not logged: {}",
                            name, e.getMessage(), e);
                }
            }
        }
        return event;
    }

    public abstract static class Event {

        public boolean cancelled;

        public void cancel() {
            cancelled = true;
        }
    }

    public static final class Meltdown extends Event {

        public final Level level;
        public final BlockPos pos;
        public final String kind;

        Meltdown(Level level, BlockPos pos, String kind) {
            this.level = level;
            this.pos = pos;
            this.kind = kind;
        }
    }

    public static final class Detonation extends Event {

        public final Level level;
        public final double x, y, z;
        public final BlockPos pos;
        public final String kind;
        public final double radius;

        Detonation(Level level, double x, double y, double z, String kind, double radius) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.pos = BlockPos.containing(x, y, z);
            this.kind = kind;
            this.radius = radius;
        }
    }

    public static final class Radiation extends Event {

        public final LivingEntity entity;
        public final boolean neutron;
        public double amount;

        Radiation(LivingEntity entity, double amount, boolean neutron) {
            this.entity = entity;
            this.amount = amount;
            this.neutron = neutron;
        }
    }

    public static final class Hazard extends Event {

        public final LivingEntity entity;
        public final ItemStack stack;
        public final String item;
        public final String type;
        public double amount;

        Hazard(LivingEntity entity, ItemStack stack, String type, double amount) {
            this.entity = entity;
            this.stack = stack;
            this.item = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            this.type = type;
            this.amount = amount;
        }
    }
}
