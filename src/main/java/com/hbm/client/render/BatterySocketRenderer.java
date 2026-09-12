package com.hbm.client.render;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The battery socket: the {@code Socket} cradle, and the {@code Battery} or {@code Capacitor} body of
 * whatever pack is plugged into it, wearing that pack's own texture.
 */
public class BatterySocketRenderer extends GemBER<TileEntityBatterySocket, BatterySocketRenderState> {

    private static final Surface EMPTY = Surface.cutout("hbm:textures/models/machines/battery_socket.png");

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of("hbm", "models/battery_socket.obj"));
    private final Map<Identifier, Surface> skins = new HashMap<>();

    /** Match the 1.12.2 RenderBatterySocket placement: centre, facing-rotate, then its model offset. */
    @Override
    protected void setup(Gem.Model<BatterySocketRenderState> declaration) {
        Gem.Part<BatterySocketRenderState> socket = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(-0.5F, 0F, 0.5F);

        socket.child(model.part("Socket"), EMPTY);
        pack(socket, "Battery");
        pack(socket, "Capacitor");
    }

    private void pack(Gem.Part<BatterySocketRenderState> socket, String part) {
        socket.child(model.part(part), EMPTY)
                .visibleIf(state -> part.equals(state.batteryPart))
                .skin(this::skinOf);
    }

    private Surface skinOf(BatterySocketRenderState state) {
        Identifier texture = state.batteryTexture;
        return texture == null ? EMPTY : skins.computeIfAbsent(texture, id -> Surface.cutout(id.toString()));
    }

    @Override
    public BatterySocketRenderState createRenderState() {
        return new BatterySocketRenderState();
    }

    @Override
    public void extract(TileEntityBatterySocket be, BatterySocketRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 5;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };

        ItemStack stack = be.syncStack;
        if (stack != null && stack.getItem() instanceof ItemBatteryPack battery) {
            state.batteryPart = battery.pack.isCapacitor() ? "Capacitor" : "Battery";
            state.batteryTexture = battery.pack.texture;
        } else {
            state.batteryPart = null;
            state.batteryTexture = null;
        }
    }
}
