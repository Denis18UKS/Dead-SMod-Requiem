package net.minecraft.client.renderer;
public class Tessellator {
  public static int draws;
  private static final Tessellator INSTANCE=new Tessellator();
  private final BufferBuilder b=new BufferBuilder();
  public static Tessellator func_178181_a_(){return INSTANCE;}
  public BufferBuilder func_178180_c(){return b;}
  public void func_78381_a(){draws++;}
}
