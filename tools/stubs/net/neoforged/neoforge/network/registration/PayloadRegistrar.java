package net.neoforged.neoforge.network.registration;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload; import net.minecraft.network.codec.StreamCodec;
public class PayloadRegistrar { public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> t,StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf,T> c,net.neoforged.neoforge.network.handling.IPayloadHandler<T> h){return this;} }
