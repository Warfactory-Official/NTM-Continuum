package com.hbm.wiaj.actors;

import net.minecraft.nbt.CompoundTag;

/**
 * Very basic actor base class that holds NBT, not very useful on its own
 * @author hbm
 */
public abstract class ActorBase implements ISpecialActor {

    protected CompoundTag data = new CompoundTag();

    @Override
    public void setActorData(CompoundTag data) {
        this.data = data;
    }

    @Override
    public void setDataPoint(String tag, Object o) {
        if(o instanceof String) this.data.putString(tag, (String) o);
        if(o instanceof Integer) this.data.putInt(tag, (Integer) o);
        if(o instanceof Float) this.data.putFloat(tag, (Float) o);
        if(o instanceof Double) this.data.putDouble(tag, (Double) o);
        if(o instanceof Boolean) this.data.putBoolean(tag, (Boolean) o);
        if(o instanceof Byte) this.data.putByte(tag, (Byte) o);
        if(o instanceof Short) this.data.putShort(tag, (Short) o);
    }
}
