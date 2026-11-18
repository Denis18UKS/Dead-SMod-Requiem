package net.minecraft.item;
import net.minecraft.util.IItemProvider;
import net.minecraft.nbt.CompoundNBT;
public class ItemStack {
  private CompoundNBT tag;
  public ItemStack(IItemProvider item){}
  public CompoundNBT func_196082_o(){if(tag==null) tag=new CompoundNBT();return tag;}
  public CompoundNBT func_77978_p(){return tag;}
}
