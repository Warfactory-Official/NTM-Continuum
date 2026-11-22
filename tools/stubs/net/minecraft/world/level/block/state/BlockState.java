package net.minecraft.world.level.block.state;
import net.minecraft.world.level.block.state.properties.Property;
public class BlockState { public boolean is(net.minecraft.world.level.block.Block b){return false;} public <T extends Comparable<T>> T getValue(Property<T> p){return null;}
 public <T extends Comparable<T>,V extends T> BlockState setValue(Property<T> p,V v){return this;} public net.minecraft.world.level.block.Block getBlock(){return null;} }
