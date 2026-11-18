package net.minecraft.block;
import net.minecraft.block.material.Material;
public class BlockState {
  private final Block block;
  public BlockState(Block block){this.block=block;}
  public Block func_177230_c(){return block;}
  public Material func_185904_a(){return new Material(true);}
}
