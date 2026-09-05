package com.hbm.blocks.bomb;

public class BlockDynamite extends BlockTNTBase {
    public BlockDynamite(Properties properties) { super(properties); }
    @Override public int kindId() { return 1; }
    @Override public String textureName() { return "dynamite"; }
    @Override public float blastStrength() { return 8F; }
}
