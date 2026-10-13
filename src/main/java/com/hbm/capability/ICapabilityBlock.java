package com.hbm.capability;

import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.level.block.Block;

/** A block that exposes capabilities to automation, implemented by the machine's own block class. */
public interface ICapabilityBlock {

    /** The flag-set exposure. {@link MachineCaps#NONE} is legal and means "nothing". */
    MachineCaps caps();

    /**
     * Registrations that are not a flag set: a face-gated mapper, or a shell resolving to a remote
     * controller.
     */
    default void declareExtraCaps(RegistryObject<? extends Block> self) {
    }
}
