package net.mcreator.horrormod.unified;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Java-8 runtime hooks added to original class files by PatchJar. No side-only references. */
public final class DeadHooks {
    private static final String DEAD = "horrormod:death_dim";
    private static final String SECRET = "horrormod:secretdim";
    private static final String MARKER = "DeadsModStructureCompass";
    private static volatile boolean warned;
    private DeadHooks() {}

    public static void onPlayerTick(Object event) {
        try {
            Object player = WorldgenPolicy.field(event, "player");
            if (player == null) return;
            Object world = WorldgenPolicy.field(player, "field_70170_p", "world");
            if (!isServerWorld(world) || !DEAD.equals(dimension(world))) return;
            Object ticks = WorldgenPolicy.field(player, "field_70173_aa", "ticksExisted");
            if (!(ticks instanceof Number) || ((Number) ticks).intValue() % 80 != 0) return;
            giveOrUpdate(player, world);
        } catch (Exception e) { warn(e); }
    }

    public static void onDeathDimensionEntry(Object event) {
        try {
            if (!DEAD.equals(dimensionKey(invoke(event, "getTo")))) return;
            Object player = invoke(event, "getPlayer");
            Object world = WorldgenPolicy.field(player, "field_70170_p", "world");
            if (isServerWorld(world)) giveOrUpdate(player, world);
        } catch (Exception e) { warn(e); }
    }

    /** Forge PlayerChangedDimensionEvent: the original procedure is replaced to prevent repeated NBT template placement. */
    public static void onSecretDimensionEntry(Object event) {
        try {
            if (!SECRET.equals(dimensionKey(invoke(event, "getTo")))) return;
            Object player = invoke(event, "getPlayer");
            Object world = WorldgenPolicy.field(player, "field_70170_p", "world");
            if (!isServerWorld(world)) return;
            if (!SECRET.equals(dimension(world))) return; // Do not undo original story-related forced exits.
            // 9x16 obsidian arrival bridge in front of the full 16x28x19 pumpkin prefab.
            Object obsidian = blockState("minecraft:obsidian");
            Object pumpkin = blockState("minecraft:carved_pumpkin");
            Object air = blockState("minecraft:air");
            Object center = blockPos(8, 80, 8);
            Object current = invoke(world, new String[]{"func_180495_p", "getBlockState"}, center);
            Object markerBlock = current == null || current == NO_METHOD ? null : invoke(current, new String[]{"func_177230_c", "getBlock"});
            Object obsidianBlock = registry("BLOCKS", "minecraft:obsidian");
            if (obsidian == null || pumpkin == null || air == null || obsidianBlock == null) return;
            if (markerBlock != obsidianBlock) {
                // Structure front starts at z=16. Existing custom pumpkin_platform.nbt is preserved.
                // The prefab contains portals, chests, and the giant pumpkin geometry.
                boolean prefabPlaced = false;
                try { prefabPlaced = placeTemplate(world, "horrormod:pumpkin_platform", 0, 81, 16); }
                catch (Exception e) { warn(e); }
                for (int x=4; x<=12; x++) for (int z=1; z<=16; z++) {
                    setBlock(world, blockPos(x, 80, z), obsidian);
                    // Only clear the safe arrival end, not the prefab's doorway at z=16.
                    if (z<=14) for (int y=81; y<=83; y++) setBlock(world, blockPos(x, y, z), air);
                }
                if (!prefabPlaced) {
                    // Fallback marker if original template cannot be resolved.
                    setBlock(world, blockPos(8, 81, 15), pumpkin);
                }
            }
            // Face the giant pumpkin (yaw 0 degrees, +Z). Coordinates are within this dimension.
            Object result = invoke(player, new String[]{"func_200619_a", "teleport"}, world, 8.5D, 81D, 8.5D, 0F, 0F);
            if (result == NO_METHOD) invoke(player, new String[]{"func_70080_a", "setPositionAndRotation"}, 8.5D, 81D, 8.5D, 0F, 0F);
        } catch (Exception e) { warn(e); }
    }

    private static void giveOrUpdate(Object player, Object world) throws Exception {
        Object inventory = WorldgenPolicy.field(player, "field_71071_by", "inventory");
        if (inventory == null) return;
        Object main = WorldgenPolicy.field(inventory, "field_70462_a", "mainInventory");
        if (!(main instanceof List)) return;
        List<?> items = (List<?>)main;
        Object compass = null;
        for (Object stack : items) {
            if (stack == null) continue;
            Object tag = invoke(stack, new String[]{"func_77978_p", "getTag"});
            if (tag == null || tag == NO_METHOD) continue;
            Object flag = invoke(tag, new String[]{"func_74767_n", "getBoolean"}, MARKER);
            if (Boolean.TRUE.equals(flag)) { compass = stack; break; }
        }
        if (compass == null) {
            Object item = registry("ITEMS", "minecraft:compass");
            if (item == null) return;
            Class<?> itemClass = Class.forName("net.minecraft.item.Item");
            Class<?> stackClass = Class.forName("net.minecraft.item.ItemStack");
            compass = stackClass.getConstructor(itemClass).newInstance(item);
        }
        Object tag = invoke(compass, new String[]{"func_196082_o", "getOrCreateTag"});
        if (tag == NO_METHOD || tag == null) return;
        invoke(tag, new String[]{"func_74757_a", "putBoolean"}, MARKER, true);
        invoke(tag, new String[]{"func_74757_a", "putBoolean"}, "LodestoneTracked", false);
        invoke(tag, new String[]{"func_74778_a", "putString"}, "LodestoneDimension", DEAD);
        int[] coords = WorldgenPolicy.nearest(WorldgenPolicy.seed(world),
            (int)Math.floor((Double)invoke(player, new String[]{"func_226277_ct_", "getPosX"})),
            (int)Math.floor((Double)invoke(player, new String[]{"func_226281_cx_", "getPosZ"})));
        Object pos = Class.forName("net.minecraft.nbt.CompoundNBT").getConstructor().newInstance();
        invoke(pos, new String[]{"func_74768_a", "putInt"}, "X", coords[0]);
        invoke(pos, new String[]{"func_74768_a", "putInt"}, "Y", coords[1]);
        invoke(pos, new String[]{"func_74768_a", "putInt"}, "Z", coords[2]);
        invoke(tag, new String[]{"func_218657_a", "put"}, "LodestonePos", pos);
        Object display = Class.forName("net.minecraft.nbt.CompoundNBT").getConstructor().newInstance();
        invoke(display, new String[]{"func_74778_a", "putString"}, "Name", "{\"text\":\"Компас мёртвых\",\"color\":\"dark_purple\",\"italic\":false}");
        invoke(tag, new String[]{"func_218657_a", "put"}, "display", display);
        // Never auto-add a second compass if one with our marker exists.
        boolean present = false;
        for (Object stack : items) if (stack == compass) {present = true;break;}
        if (!present) {
            Object inserted = invoke(inventory, new String[]{"func_70441_a", "addItemStackToInventory"}, compass);
            if (!Boolean.TRUE.equals(inserted)) {
                invoke(player, new String[]{"func_71019_a", "dropItem"}, compass, false);
            }
        }
    }

    private static boolean placeTemplate(Object world, String id, int x, int y, int z) throws Exception {
        Object manager = invoke(world, new String[]{"func_184163_y", "getStructureTemplateManager"});
        Object location = Class.forName("net.minecraft.util.ResourceLocation").getConstructor(String.class).newInstance(id);
        Object template = invoke(manager, new String[]{"func_200220_a", "getTemplateDefaulted"}, location);
        if (template == null || template == NO_METHOD) return false;
        Object settings = Class.forName("net.minecraft.world.gen.feature.template.PlacementSettings").getConstructor().newInstance();
        Class<?> rotClass = Class.forName("net.minecraft.util.Rotation");
        Class<?> mirClass = Class.forName("net.minecraft.util.Mirror");
        Object rotation = rotClass.getField("NONE").get(null);
        Object mirror = mirClass.getField("NONE").get(null);
        invoke(settings, new String[]{"func_186220_a", "setRotation"}, rotation);
        invoke(settings, new String[]{"func_186214_a", "setMirror"}, mirror);
        invoke(settings, new String[]{"func_186218_a", "setBoundingBox"}, new Object[]{null});
        invoke(settings, new String[]{"func_186222_a", "setIgnoreEntities"}, false);
        Object random = WorldgenPolicy.field(world, "field_73012_v", "rand", "random");
        if (!(random instanceof java.util.Random)) random = new java.util.Random(WorldgenPolicy.seed(world));
        Object placed = invoke(template, new String[]{"func_237144_a_", "addBlocksToWorld"}, world, blockPos(x,y,z), settings, random);
        return placed != NO_METHOD;
    }

    private static Object blockPos(int x, int y, int z) throws Exception {
        return Class.forName("net.minecraft.util.math.BlockPos").getConstructor(int.class,int.class,int.class).newInstance(x,y,z);
    }
    private static Object blockState(String id) throws Exception {
        Object block = registry("BLOCKS", id);
        return block == null ? null : invoke(block, new String[]{"func_176223_P", "getDefaultState"});
    }
    private static void setBlock(Object world, Object pos, Object state) throws Exception {
        Object res = invoke(world, new String[]{"func_180501_a", "setBlockState"}, pos, state, 3);
        if (res == NO_METHOD) throw new IllegalStateException("ServerWorld.setBlockState is unavailable");
    }
    private static Object registry(String field, String id) throws Exception {
        Class<?> cls = Class.forName("net.minecraftforge.registries.ForgeRegistries");
        Field f = cls.getField(field);
        Object key = Class.forName("net.minecraft.util.ResourceLocation").getConstructor(String.class).newInstance(id);
        Object result = invoke(f.get(null), new String[]{"getValue"}, key);
        return result == NO_METHOD ? null : result;
    }
    private static boolean isServerWorld(Object world) {
        return world != null && isInstance("net.minecraft.world.server.ServerWorld", world);
    }
    // Not using explicit net.minecraft types keeps hooks safe on both sides of a dedicated server.
    private static boolean isInstance(String name, Object object) {
        try { return object != null && Class.forName(name).isInstance(object); }
        catch (Exception e) { return false; }
    }
    private static String dimension(Object world) throws Exception {
        return dimensionKey(invoke(world, new String[]{"func_234923_W_", "getDimensionKey"}));
    }
    private static String dimensionKey(Object key) {
        if (key == null || key == NO_METHOD) return "";
        String s = key.toString();
        if (s.contains(DEAD)) return DEAD;
        if (s.contains(SECRET)) return SECRET;
        return s;
    }

    private static final Object NO_METHOD = new Object();
    private static Object invoke(Object target, String name, Object... args) throws Exception {return invoke(target, new String[]{name}, args);}
    private static Object invoke(Object target, String[] names, Object... args) throws Exception {
        if (target == null) return NO_METHOD;
        for (Class<?> c=target.getClass();c!=null;c=c.getSuperclass()) {
            for (Method m:c.getDeclaredMethods()) {
                if (m.getParameterCount()!=args.length) continue;
                boolean named=false;
                for (String name:names) if (m.getName().equals(name)) {named=true;break;}
                if (!named) continue;
                Class<?>[] params=m.getParameterTypes();
                boolean compatible=true;
                for (int i=0;i<params.length;i++) if (args[i]!=null && !boxed(params[i]).isAssignableFrom(args[i].getClass())) {compatible=false;break;}
                if (!compatible) continue;
                m.setAccessible(true);
                return m.invoke(target,args);
            }
        }
        return NO_METHOD;
    }
    private static Class<?> boxed(Class<?> cls) {
        if (!cls.isPrimitive()) return cls;
        if (cls==int.class) return Integer.class;
        if (cls==boolean.class) return Boolean.class;
        if (cls==double.class) return Double.class;
        if (cls==float.class) return Float.class;
        if (cls==long.class) return Long.class;
        return cls;
    }
    private static void warn(Exception e) {
        if (!warned) {warned=true;System.err.println("[Dead's Mod Unified] Runtime hook failed: "+e);e.printStackTrace();}
    }
}
