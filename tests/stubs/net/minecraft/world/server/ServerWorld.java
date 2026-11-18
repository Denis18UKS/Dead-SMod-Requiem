package net.minecraft.world.server;
import net.minecraft.world.storage.DimensionSavedDataManager;
public class ServerWorld {
  private final DimensionSavedDataManager data = new DimensionSavedDataManager();
  public DimensionSavedDataManager getSavedData(){return data;}
  public Object func_234923_W_(){return "ResourceKey[minecraft:dimension / horrormod:death_dim]";}
  public long func_72905_C(){return 12345L;}
}
