package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import java.util.HashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Damages and knocks back entities, returning the knockback each player received. */
public interface IEntityProcessor {

    HashMap<Player, Vec3> process(ExplosionVNT explosion, Level world, double x, double y, double z, float size);
}
