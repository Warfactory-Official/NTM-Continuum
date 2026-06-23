package com.hbm.uninos;

import java.util.function.Supplier;

@FunctionalInterface
public interface INetworkProvider<T extends NodeNet<?, ?, ?, T>> extends Supplier<T> {
}
