package net.minecraft.core;
public enum Direction { NORTH,EAST,SOUTH,WEST,UP,DOWN;
 public enum Axis { X,Y,Z; public boolean isVertical(){return false;} }
 public Direction getOpposite(){return this;} public Direction getClockWise(){return this;} public Direction getCounterClockWise(){return this;}
 public Axis getAxis(){return null;} public String getName(){return "";} public static Direction byName(String s){return null;} }
