package com.hbm.uninos;

/**
 * Declares an endpoint to be a BUFFER: storage that is the round's RESIDUAL rather than one of its endpoints.
 *
 * <p>A buffer absorbs what the real providers still hold, or covers what they could not, and never both in one
 * round; it is not gathered into the receiver tiers at all, which is what stops it being its own counterparty.
 * Within the pool its receiver priority reads "how full I want this one" - surplus fills the high tiers first,
 * a deficit drains the low tiers first.
 *
 * <p>An implementor MUST expose both roles, since the residual needs a fill and a headroom from the same object.
 *
 * <p>ONE predicate spans both nets rather than one per net: the answer is a property of the machine, not of the
 * commodity, and a class implementing both handler interfaces would otherwise inherit two unrelated defaults of
 * the same name and fail to compile.
 */
public interface IBufferedEndpoint {

    default boolean bufferedEndpoint() {
        return false;
    }
}
