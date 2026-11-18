package net.mcreator.horrormod.unified;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;
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
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT);
            GL11.glPushMatrix();
            try {
                // The event pose matrix contains Forge's camera transformations.
                Object stackMatrix=WorldgenPolicy.invoke(event,new String[]{"getMatrixStack"});
                Object last=WorldgenPolicy.call(stackMatrix,"func_227866_c_","getLast","last");
                Object matrix=WorldgenPolicy.call(last,"func_227870_a_","getMatrix","pose");
                if(matrix!=null) {
                    Class<?> system=Class.forName("com.mojang.blaze3d.systems.RenderSystem");
                    for(java.lang.reflect.Method method:system.getDeclaredMethods()) {
                        if(method.getName().equals("multMatrix") && method.getParameterCount()==1 && method.getParameterTypes()[0].isInstance(matrix)) {
                            method.invoke(null,matrix);break;
                        }
                    }
                }
                GL11.glTranslated(-px,-py,-pz);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthMask(false);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
                // A soft halo then a thin high-contrast main contour.
                draw(edges,ox,oy,oz,px,py,pz,3.3f,0.24f);
                draw(edges,ox,oy,oz,px,py,pz,1.45f,0.98f);
            } finally {
                GL11.glDepthMask(true);
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        } catch(Throwable ex) {
            if(!logged) {logged=true;System.err.println("[Requiem] Outline renderer: "+ex);ex.printStackTrace();}
        }
    }

    private static void draw(List<Edge> edges,int ox,int oy,int oz,double cx,double cy,double cz,float width,float alpha) {
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(1f,1f,1f,alpha);
        for(Edge e:edges) {
            double x=(e.ax+e.bx)*.5+ox,y=(e.ay+e.by)*.5+oy,z=(e.az+e.bz)*.5+oz;
            boolean visible=false,hidden=false;
            for(int f=0;f<6;f++)if((e.faces&(1<<f))!=0) {
                double dot=(cx-x)*DX[f]+(cy-y)*DY[f]+(cz-z)*DZ[f];
                if(dot>=0.001)visible=true;else hidden=true;
            }
            // Silhouette edge = one front face and one back face; open/single faces count too.
            if(!visible || (!hidden && Integer.bitCount(e.faces)>1))continue;
            GL11.glVertex3d(ox+e.ax,oy+e.ay,oz+e.az);
            GL11.glVertex3d(ox+e.bx,oy+e.by,oz+e.bz);
        }
        GL11.glEnd();
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
