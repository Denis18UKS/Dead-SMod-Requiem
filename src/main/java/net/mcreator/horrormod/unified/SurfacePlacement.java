package net.mcreator.horrormod.unified;


/**
 * Places key/exit NBT templates ABOVE the real terrain of the Dead Dimension.
 * Called from the original generator in place of its OCEAN_FLOOR_WG heightmap
 * lookup. Never uses fixed Y and never builds on an existing bedrock roof.
 *
 * No client-only classes: safe for Forge 1.16.5 dedicated servers.
 */
public final class SurfacePlacement {
    private SurfacePlacement() {}

    private static final int[] SAMPLE = {0, 7, 15};
    private static final int MIN_SURFACE = 24;
    private static final int MAX_SURFACE = 108; // 14-block template + sky clearance in 128-high generator
    private static final int MAX_SLOPE = 10;
    private static final int TEMPLATE_WIDTH = 16;

    /**
     * Called in place of ISeedReader.getHeight(heightmap,x,z).
     * The actual origin for the exit template gets a +2 offset in original code.
     * Return -1 for unsafe sites; the ASM hook aborts the generator on -1.
     */
    public static int findOpenSurface(Object reader, Object heightmap, int x, int z, String kind) {
        try {
            // A world created before v3.3 can still contain the old Nether-style
            // bedrock ROOF: never use it as the floor of a structure.
            if (!isOpenAt(reader, x, z) || !isOpenAt(reader, x+15, z+15)) return -1;
            int lowest = Integer.MAX_VALUE;
            int highest = Integer.MIN_VALUE;
            int highX=x, highZ=z;
            for (int dx : SAMPLE) for (int dz : SAMPLE) {
                Object n = WorldgenPolicy.invoke(reader,
                    new String[]{"func_201676_a", "getHeight"}, heightmap, x+dx, z+dz);
                if (!(n instanceof Number)) return -1;
                int level = ((Number)n).intValue();
                // Reject the bedrock floor, roof, implausibly high cliffs and void.
                if (level < MIN_SURFACE || level > MAX_SURFACE) return -1;
                lowest = Math.min(lowest, level);
                if (level>highest) {highest=level;highX=x+dx;highZ=z+dz;}
            }
            if (highest - lowest > MAX_SLOPE) return -1;
            int floorOffset = WorldgenPolicy.EXIT.equals(kind) ? 2 : 0;
            int result = highest - floorOffset;
            if (result < MIN_SURFACE || result + floorOffset + templateHeight(kind) >= 124) return -1;
            Object below = blockState(reader,highX,highest-1,highZ);
            String ground = blockId(below);
            if (ground.isEmpty() || ground.endsWith(":bedrock") || isAir(below)) return -1;
            return result;
        } catch (Exception e) {
            WorldgenPolicy.logOnce("exposed dungeon surface",e);
            return -1;
        }
    }

    /** Use the selected origin; do not randomize it or bury it again. */
    public static int chosenY(java.util.Random random, int unusedMin, int actualY) {
        return actualY;
    }

    public static int templateHeight(String kind) {
        return WorldgenPolicy.EXIT.equals(kind) ? 12 : 14;
    }

    private static boolean isOpenAt(Object reader, int x, int z) throws Exception {
        // The world has no skylight, so canSeeSky() is not suitable here.
        // Explicitly check that the old bedrock/solid roof was removed.
        return isAir(blockState(reader,x,126,z)) && isAir(blockState(reader,x,122,z));
    }

    /**
     * Runs before the original Template.addBlocksToWorld call. Clears foliage
     * and small overhangs inside the future NBT volume, flattens a one-block
     * approach ring and fills gaps under the 16x16 floor with native stone.
     * This is limited to a site already approved by findOpenSurface().
     */
    public static boolean prepare(Object reader, Object originalPos, String kind) {
        try {
            final int x=WorldgenPolicy.getCoord(originalPos,"func_177958_n","getX");
            final int y=WorldgenPolicy.getCoord(originalPos,"func_177956_o","getY");
            final int z=WorldgenPolicy.getCoord(originalPos,"func_177952_p","getZ");
            final Object nativeState=defaultState("horrormod:deathblock");
            final Object airState=defaultState("minecraft:air");
            if (!hasSetBlockMethod(reader) || nativeState == null || airState == null) {
                throw new IllegalStateException("Missing registered surface blocks");
            }
            // Prepare the central ground under the 16x16 NBT (max 12 support blocks).
            for (int dx=0; dx<TEMPLATE_WIDTH; dx++) for (int dz=0; dz<TEMPLATE_WIDTH; dz++) {
                for (int depth=1; depth<=12; depth++) {
                    int yy=y-depth;
                    Object existing=blockState(reader,x+dx,yy,z+dz);
                    if (!isAir(existing)) break;
                    setBlock(reader,x+dx,yy,z+dz,nativeState);
                }
            }
            // Remove terrain, trees and vegetation that could obscure the model.
            // The NBT is then placed on this prepared open, accessible surface.
            int top=y+templateHeight(kind)+2;
            for (int dx=-1; dx<=TEMPLATE_WIDTH; dx++) for (int dz=-1; dz<=TEMPLATE_WIDTH; dz++) {
                boolean interior=(dx>=0 && dx<TEMPLATE_WIDTH && dz>=0 && dz<TEMPLATE_WIDTH);
                int high=interior ? top : y+4;
                for (int yy=y; yy<=high; yy++) {
                    Object existing=blockState(reader,x+dx,yy,z+dz);
                    if (!isAir(existing)) setBlock(reader,x+dx,yy,z+dz,airState);
                }
            }
            return true;
        } catch(Exception e) {
            WorldgenPolicy.logOnce("prepare dungeon surface",e);
            return false;
        }
    }

    private static boolean hasSetBlockMethod(Object reader) {
        for (java.lang.reflect.Method m: reader.getClass().getMethods()) {
            if (m.getParameterCount() == 3 &&
                (m.getName().equals("func_180501_a") || m.getName().equals("setBlockState"))) return true;
        }
        return false;
    }

    private static Object defaultState(String id) throws Exception {
        Class<?> registries=Class.forName("net.minecraftforge.registries.ForgeRegistries");
        Object blocks=registries.getField("BLOCKS").get(null);
        Object loc=Class.forName("net.minecraft.util.ResourceLocation")
            .getConstructor(String.class).newInstance(id);
        Object block=WorldgenPolicy.invoke(blocks,new String[]{"getValue"},loc);
        return WorldgenPolicy.invoke(block,new String[]{"func_176223_P","getDefaultState"});
    }
    private static Object blockState(Object reader,int x,int y,int z) throws Exception {
        Object pos=blockPos(x,y,z);
        return WorldgenPolicy.invoke(reader,new String[]{"func_180495_p","getBlockState"},pos);
    }
    private static Object blockPos(int x,int y,int z) throws Exception {
        return Class.forName("net.minecraft.util.math.BlockPos")
            .getConstructor(int.class,int.class,int.class).newInstance(x,y,z);
    }
    private static boolean isAir(Object state) throws Exception {
        if (state==null) return false;
        String id=blockId(state);
        return "minecraft:air".equals(id) || "minecraft:cave_air".equals(id) ||
               "minecraft:void_air".equals(id);
    }
    private static String blockId(Object state) throws Exception {
        if (state==null)return "";
        Object block=WorldgenPolicy.invoke(state,new String[]{"func_177230_c","getBlock"});
        if (block==null)return "";
        Object registry=Class.forName("net.minecraftforge.registries.ForgeRegistries")
             .getField("BLOCKS").get(null);
        Object key=WorldgenPolicy.invoke(registry,new String[]{"getKey"},block);
        if (key!=null) return key.toString();
        return block.toString();
    }
    private static void setBlock(Object reader,int x,int y,int z,Object state) throws Exception {
        Object result=WorldgenPolicy.invoke(reader,new String[]{"func_180501_a","setBlockState"},
            blockPos(x,y,z),state,Integer.valueOf(2));
        // Setter can return void/null or boolean; both are valid in game.
        if (Boolean.FALSE.equals(result)) throw new IllegalStateException("Cannot edit surface block");
    }
}
