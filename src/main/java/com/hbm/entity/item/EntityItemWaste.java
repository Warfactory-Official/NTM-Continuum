package com.hbm.entity.item;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.CompatItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Dropped nuclear-waste item: invulnerable and never despawns. 1.7's EntityItem delta is re-based onto 26.1 ItemEntity - hurtServer/hurtClient are final, so invulnerability rides the vanilla invulnerable flag.
 */
public class EntityItemWaste extends CompatItemEntity {

    public EntityItemWaste(EntityType<? extends EntityItemWaste> type, Level level) {
        super(type, level);
        // 1.7: hurtServer final; invulnerability via vanilla flag
        setInvulnerable(true);
        // 1.7: never expires; age=-32768 freezes ageing
        setUnlimitedLifetime();
    }

    public EntityItemWaste(Level level, double x, double y, double z, ItemStack stack) {
        this(ModNtmEntities.WASTE_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
    }
}
