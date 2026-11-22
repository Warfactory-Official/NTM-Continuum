package fissionworks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The one packet the console sends: a command for the console block entity at pos. */
@EventBusSubscriber(modid = FissionWorks.ID, bus = EventBusSubscriber.Bus.MOD)
public final class Net {
    private Net() {}

    public record Command(BlockPos pos, int op, int a, int b) implements CustomPacketPayload {
        public static final Type<Command> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(FissionWorks.ID, "command"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Command> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Command::pos,
                ByteBufCodecs.VAR_INT, Command::op,
                ByteBufCodecs.VAR_INT, Command::a,
                ByteBufCodecs.VAR_INT, Command::b,
                Command::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Command.TYPE, Command.CODEC, Net::handle);
    }

    private static void handle(Command c, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player.distanceToSqr(Vec3.atCenterOf(c.pos())) <= 64
                    && player.level().getBlockEntity(c.pos()) instanceof Console console) {
                console.command(player, c.op(), c.a(), c.b());
            }
        });
    }
}
