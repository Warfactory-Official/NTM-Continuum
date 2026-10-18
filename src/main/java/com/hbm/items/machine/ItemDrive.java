package com.hbm.items.machine;

import com.hbm.items.ModIntermediateItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code ItemDrive}: one item per drive type, id {@code drive_<type>}. */
public class ItemDrive extends Item {

    public enum EnumDriveType {
        FLASH_EMPTY,
        DISK_EMPTY,
        FLASH_BROKEN,
        DISK_BROKEN,

        FLASH_FLIGHTSIM,			// precalc for spaceflight
        FLASH_PARTICLESIM,			// precalc for fusion

        DISK_FLIGHTDATA,			// raw data from satellite
        DISK_FLIGHTDATA_PROCESSED,	// processed data from satellite
        DISK_ORBITDATA,				// raw sensor relay data
        DISK_ORBITDATA_PROCESSED,	// processed data from sensor relay

        KLAUS,						// kkklanker
    }

    private final EnumDriveType type;

    public ItemDrive(Properties properties, EnumDriveType type) {
        super(properties);
        this.type = type;
    }

    public EnumDriveType type() {
        return type;
    }

    public static @Nullable EnumDriveType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemDrive drive ? drive.type : null;
    }

    public static ItemStack stack(EnumDriveType type, int count) {
        return new ItemStack(ModIntermediateItems.drive(type), count);
    }
}
