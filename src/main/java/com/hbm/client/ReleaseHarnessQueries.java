package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import com.hbm.tileentity.machine.TileEntityChimneyBase;
import dev.golem.core.harness.HarnessQueryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * The {@code hbm_release} harness query. Inert unless the
 * harness listener runs.
 * <ul>
 *   <li>{@code mode=tooltip, slot=N}: the client's own tooltip lines ({@code Screen.getTooltipFromItem}) of player
 *       inventory slot N, each as its text and the colour of every styled run, read while the scenario holds (or does
 *       not hold) left Shift.</li>
 *   <li>{@code mode=machine, x, y, z, radius}: the integrated server's view of every chimney core within the radius
 *       (position, {@code onTicks}), the smoke tanks of a polluting machine at x, y, z, and the region's pollution
 *       there.</li>
 *   <li>{@code mode=pad, x, y, z}: the launch pad (any block of it resolves to the core's tile) tanks, power and state.</li>
 * </ul>
 */
public final class ReleaseHarnessQueries {

    private ReleaseHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_release", ReleaseHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        String mode = params.has("mode") ? params.get("mode").getAsString() : "tooltip";
        if (mode.equals("tooltip")) return tooltip(mc, params);
        return ServerQueries.answer(mc, "hbm_release", params.deepCopy(), (level, p) -> {
            JsonObject json = new JsonObject();
            try {
                BlockPos at = new BlockPos(p.get("x").getAsInt(), p.get("y").getAsInt(), p.get("z").getAsInt());
                if (mode.equals("machine")) {
                    int r = p.has("radius") ? p.get("radius").getAsInt() : 8;
                    JsonArray chimneys = new JsonArray();
                    for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -2, -r), at.offset(r, 2, r))) {
                        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) continue;
                        if (level.getBlockEntity(pos) instanceof TileEntityChimneyBase chimney) {
                            JsonObject o = new JsonObject();
                            o.addProperty("x", pos.getX());
                            o.addProperty("y", pos.getY());
                            o.addProperty("z", pos.getZ());
                            o.addProperty("on_ticks", chimney.onTicks);
                            o.addProperty("type", String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE
                                    .getKey(chimney.getType())));
                            chimneys.add(o);
                        }
                    }
                    json.add("chimneys", chimneys);
                    BlockEntity be = level.getBlockEntity(at);
                    if (be instanceof TileEntityMachinePolluting polluting) {
                        json.add("smoke", tanks(polluting.getSmokeTanks()));
                    }
                    json.addProperty("soot", PollutionHandler.getPollution(level, at, PollutionType.SOOT));
                    json.addProperty("poison", PollutionHandler.getPollution(level, at, PollutionType.POISON));
                    json.addProperty("heavymetal", PollutionHandler.getPollution(level, at, PollutionType.HEAVYMETAL));
                } else if (mode.equals("pad")) {
                    BlockEntity be = null;
                    for (BlockPos pos : BlockPos.betweenClosed(at.offset(-3, -1, -3), at.offset(3, 1, 3))) {
                        if (level.getBlockEntity(pos) instanceof TileEntityLaunchPadBase found) {
                            be = found;
                            json.addProperty("core", pos.getX() + " " + pos.getY() + " " + pos.getZ());
                            break;
                        }
                    }
                    if (be instanceof TileEntityLaunchPadBase pad) {
                        json.addProperty("found", true);
                        json.addProperty("power", pad.power);
                        json.addProperty("state", pad.state);
                        json.add("tanks", tanks(pad.tanks));
                        JsonArray slots = new JsonArray();
                        for (int i = 0; i < pad.inventory.getSlots(); i++) {
                            ItemStack s = pad.inventory.getStackInSlot(i);
                            slots.add(s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem())
                                    + " x" + s.getCount());
                        }
                        json.add("slots", slots);
                    } else {
                        json.addProperty("found", false);
                    }
                }
            } catch (Throwable t) {
                json.addProperty("error", t.toString());
            }
            return json;
        });
    }

    private static JsonArray tanks(FluidTankNTM[] tanks) {
        JsonArray out = new JsonArray();
        for (FluidTankNTM tank : tanks) {
            JsonObject o = new JsonObject();
            o.addProperty("type", tank.getTankType().getName());
            o.addProperty("fill", tank.getFill());
            o.addProperty("max", tank.getMaxFill());
            out.add(o);
        }
        return out;
    }

    private static JsonObject tooltip(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        if (mc.player == null) return json;
        int slot = params.has("slot") ? params.get("slot").getAsInt() : 0;
        ItemStack stack = mc.player.getInventory().getItem(slot);
        json.addProperty("item", String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        JsonArray lines = new JsonArray();
        JsonArray colours = new JsonArray();
        for (Component line : Screen.getTooltipFromItem(mc, stack)) {
            lines.add(line.getString());
            StringBuilder runs = new StringBuilder();
            line.visit((Style style, String text) -> {
                if (!text.isEmpty()) {
                    if (runs.length() > 0) runs.append('|');
                    runs.append(style.getColor() == null ? "-" : style.getColor().toString())
                            .append(style.isItalic() ? "/i" : "")
                            .append(':').append(text);
                }
                return Optional.empty();
            }, Style.EMPTY);
            colours.add(runs.toString());
        }
        json.add("lines", lines);
        json.add("styled", colours);
        return json;
    }
}
