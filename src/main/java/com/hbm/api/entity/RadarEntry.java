package com.hbm.api.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * One blip on a radar: 1.7 {@code api.hbm.entity.RadarEntry}. The name is a translation key for
 * detectables and the player's plain name for players, exactly as upstream mixed the two.
 *
 * <p>The dimension is the level's id string here; 1.7 carried the integer dimension id. Nothing reads it
 * back except the radar screen, which only compares it for equality.
 */
public class RadarEntry {

    /** Name used for radar display, uses I18n for lookup. */
    public String unlocalizedName = "";
    /** The type of dot to show on the radar as well as the redstone level in tier mode. */
    public int blipLevel;
    public int posX;
    public int posY;
    public int posZ;
    public String dim = "";
    public int entityID;
    /** Whether this radar entry should be counted for the redstone output. Server-side only, as upstream. */
    public boolean redstone;

    public RadarEntry() { } // blank ctor for packets

    public RadarEntry(String name, int level, int x, int y, int z, String dim, int entityID, boolean redstone) {
        this.unlocalizedName = name;
        this.blipLevel = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.dim = dim;
        this.entityID = entityID;
        this.redstone = redstone;
    }

    public RadarEntry(IRadarDetectableNT detectable, Entity entity, boolean redstone) {
        this(detectable.getRadarName(), detectable.getBlipLevel(), (int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getY()), (int) Math.floor(entity.getZ()), dimOf(entity), entity.getId(), redstone);
    }

    public RadarEntry(Player player) {
        this(player.getName().getString(), IRadarDetectableNT.PLAYER,
                (int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()),
                dimOf(player), player.getId(), true);
    }

    public static String dimOf(Entity entity) {
        return dev.golem.core.Ids.keyId(entity.level().dimension()).toString();
    }

    public void fromBytes(RegistryFriendlyByteBuf buf) {
        this.unlocalizedName = buf.readUtf();
        this.blipLevel = buf.readShort();
        this.posX = buf.readInt();
        this.posY = buf.readInt();
        this.posZ = buf.readInt();
        this.dim = buf.readUtf();
        this.entityID = buf.readInt();
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.unlocalizedName);
        buf.writeShort(this.blipLevel);
        buf.writeInt(this.posX);
        buf.writeInt(this.posY);
        buf.writeInt(this.posZ);
        buf.writeUtf(this.dim);
        buf.writeInt(this.entityID);
    }
}
