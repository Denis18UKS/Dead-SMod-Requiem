package net.mcreator.horrormod.unified;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-only RenderWorldLast overlay. Reads the real NBT template's external voxel-face
 * edge mesh (precomputed by tools/structure_mesh.py) and renders view-dependent white
 * silhouettes even when terrain blocks obstruct the target. Nothing is rendered if the
 * selected compass target has not actually been recorded by the server.
 *
 * This class is never loaded on a physical dedicated server.
 */
public final class ClientOutline {
    private static final double MAX_DISTANCE_SQ = 128d * 128d;
    private static final int[] DX = {1,-1,0,0,0,0};
    private static final int[] DY = {0,0,1,-1,0,0};
    private static final int[] DZ = {0,0,0,0,1,-1};
    private static final List<Edge> KEY = load("key");
    private static final List<Edge> EXIT = load("exit");
    private static boolean logged;

    private ClientOutline() {}

    private static final class Edge {
        final double ax,ay,az,bx,by,bz;
        final int faces;
        Edge(double ax,double ay,double az,double bx,double by,double bz,int faces){
            this.ax=ax;this.ay=ay;this.az=az;this.bx=bx;this.by=by;this.bz=bz;this.faces=faces;
        }
    }

    private static List<Edge> load(String mode) {
        List<Edge> out = new ArrayList<>();
        try (InputStream is = ClientOutline.class.getResourceAsStream("/assets/horrormod/requiem/mesh_"+mode+".bin")) {
            if(is==null)throw new IllegalStateException("Missing contour mesh for "+mode);
            DataInputStream in = new DataInputStream(is);
            if(in.readInt()!=0x52334d45)throw new IllegalStateException("Invalid contour mesh header");
            int count=in.readInt();
            if(count<0 || count>40000)throw new IllegalStateException("Unexpected mesh size: "+count);
            for(int i=0;i<count;i++)out.add(new Edge(in.readShort(),in.readShort(),in.readShort(),
                    in.readShort(),in.readShort(),in.readShort(),in.readUnsignedByte()));
        } catch(Exception ex) {System.err.println("[Requiem] Cannot load "+mode+" outline: "+ex);}
        return out;
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        try {
            Object client = Class.forName("net.minecraft.client.Minecraft").getMethod("func_71410_x").invoke(null);
            Object player = WorldgenPolicy.field(client,"field_71439_g","player");
            if(player==null)return;
            Object world=WorldgenPolicy.field(player,"field_70170_p","world");
            if(!DeadHooks.isDeathDimension(world))return;
            Object stack=WorldgenPolicy.call(player,"func_184614_ca","getMainHandItem","getHeldItemMainhand");
            if(!DeadHooks.isCompass(stack))
                stack=WorldgenPolicy.call(player,"func_184592_cb","getOffhandItem","getHeldItemOffhand");
            if(!DeadHooks.isCompass(stack))return;
            Object tag=WorldgenPolicy.call(stack,"func_77978_p","getTag");
            if(tag==null)return;
            Object target=WorldgenPolicy.invoke(tag,new String[]{"func_74775_l","getCompound"},"LodestonePos");
            if(target==null)return;
            // A fake missing compound can produce (0,0,0); require NBT contains the target key.
            Object has=WorldgenPolicy.invoke(tag,new String[]{"func_74764_b","contains"},"LodestonePos");
            if(!Boolean.TRUE.equals(has))return;
            int ox=num(target,"X"),oy=num(target,"Y"),oz=num(target,"Z");
            String mode=(String)WorldgenPolicy.invoke(tag,new String[]{"func_74779_i","getString"},"RequiemMode");
            List<Edge> edges="exit".equals(mode)?EXIT:KEY;
            if(edges.isEmpty())return;
            Object renderer=WorldgenPolicy.field(client,"field_71460_t","gameRenderer");
            Object camera=WorldgenPolicy.call(renderer,"func_215311_a","getActiveRenderInfo","getMainCamera");
            Object vec=WorldgenPolicy.call(camera,"func_216785_c","getProjectedView","getPosition");
            double px=coord(vec,"field_72450_a","x"),py=coord(vec,"field_72448_b","y"),pz=coord(vec,"field_72449_c","z");
            double dx=ox+8-px,dy=oy+6-py,dz=oz+8-pz;
            if(dx*dx+dy*dy+dz*dz>MAX_DISTANCE_SQ)return;
            // RenderWorldLastEvent requires buffered vertices + the camera pose matrix.
            MatrixStack pose=event.getMatrixStack();
            pose.func_227860_a_();
            try {
                pose.func_227861_a_(-px,-py,-pz);
                Matrix4f matrix=pose.func_227866_c_().func_227870_a_();
                RenderSystem.disableTexture();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();
                draw(matrix,edges,ox,oy,oz,px,py,pz,3f,60);
                draw(matrix,edges,ox,oy,oz,px,py,pz,1.5f,255);
            } finally {
                RenderSystem.lineWidth(1f);
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.enableTexture();
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                pose.func_227865_b_();
            }
        } catch(Throwable ex) {
            if(!logged) {logged=true;System.err.println("[Requiem] Outline renderer: "+ex);ex.printStackTrace();}
        }
    }

    private static void draw(Matrix4f matrix,List<Edge> edges,int ox,int oy,int oz,
                             double cx,double cy,double cz,float width,int alpha) {
        Tessellator t=Tessellator.func_178181_a_();
        BufferBuilder b=t.func_178180_c();
        IVertexBuilder vertices=b;
        RenderSystem.lineWidth(width);
        b.func_181668_a(GL11.GL_LINES,DefaultVertexFormats.field_181706_f);
        for(Edge e:edges) {
            double x=(e.ax+e.bx)*.5+ox,y=(e.ay+e.by)*.5+oy,z=(e.az+e.bz)*.5+oz;
            boolean visible=false,hidden=false;
            for(int f=0;f<6;f++)if((e.faces&(1<<f))!=0) {
                double dot=(cx-x)*DX[f]+(cy-y)*DY[f]+(cz-z)*DZ[f];
                if(dot>=0.001)visible=true;else hidden=true;
            }
            if(!visible || (!hidden && Integer.bitCount(e.faces)>1))continue;
            vertices.func_227888_a_(matrix,(float)(ox+e.ax),(float)(oy+e.ay),(float)(oz+e.az))
                    .func_225586_a_(255,255,255,alpha).func_181675_d();
            vertices.func_227888_a_(matrix,(float)(ox+e.bx),(float)(oy+e.by),(float)(oz+e.bz))
                    .func_225586_a_(255,255,255,alpha).func_181675_d();
        }
        t.func_78381_a();
    }
    private static int num(Object tag,String key) throws Exception {
        Object n=WorldgenPolicy.invoke(tag,new String[]{"func_74762_e","getInt"},key);
        return n instanceof Number?((Number)n).intValue():0;
    }
    private static double coord(Object pos,String srg,String named) throws Exception {
        Object v=WorldgenPolicy.field(pos,srg,named);
        return v instanceof Number?((Number)v).doubleValue():0.0;
    }
}
