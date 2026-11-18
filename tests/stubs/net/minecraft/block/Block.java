package net.minecraft.block;
import net.minecraft.block.material.Material;
public class Block {
  private final String id;
  public Block(String id){this.id=id;}
  public BlockState func_176223_P(){return new BlockState(this);}
  public String toString(){return id;}
}
