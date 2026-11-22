package net.neoforged.neoforge.network.handling;
public interface IPayloadHandler<T> { void handle(T p,IPayloadContext c); }
