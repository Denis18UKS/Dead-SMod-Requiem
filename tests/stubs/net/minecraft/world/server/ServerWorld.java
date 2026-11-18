package net.minecraft.world.server;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
public class ServerWorld {
  private final DimensionSavedDataManager data = new DimensionSavedDataManager();
  public DimensionSavedDataManager getSavedData(){return data;}
  public Object func_234923_W_(){return "ResourceKey[minecraft:dimension / horrormod:death_dim]";}
  public long func_72905_C(){return 12345L;}
  public boolean func_175623_d(BlockPos pos){return pos.func_177956_o()>=81 && pos.func_177958_n()==1 && pos.func_177952_p()==0;}
  public BlockState func_180495_p(BlockPos pos){return new BlockState(new Block("minecraft:stone"));}
}
