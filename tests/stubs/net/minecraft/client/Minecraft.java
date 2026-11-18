package net.minecraft.client;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.renderer.GameRenderer;
public class Minecraft {
  public PlayerEntity field_71439_g;
  public final GameRenderer field_71460_t=new GameRenderer();
  private static final Minecraft INSTANCE = new Minecraft();
  public static Minecraft func_71410_x(){return INSTANCE;}
}
