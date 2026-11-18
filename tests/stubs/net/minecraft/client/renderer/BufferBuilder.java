package net.minecraft.client.renderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.vector.Matrix4f;
import com.mojang.blaze3d.vertex.IVertexBuilder;
public class BufferBuilder implements IVertexBuilder {
  public static int totalVertices;
  public void func_181668_a(int glMode,VertexFormat fmt){}
  public IVertexBuilder func_227888_a_(Matrix4f matrix,float x,float y,float z){return this;}
  public IVertexBuilder func_225586_a_(int r,int g,int b,int a){return this;}
  public void func_181675_d(){totalVertices++;}
}
