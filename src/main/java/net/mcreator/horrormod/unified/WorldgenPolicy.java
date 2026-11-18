package net.mcreator.horrormod.unified;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Deterministic, no-per-chunk-RNG world generation locations.
 * Compile-only Java 8 class: all Minecraft version coupling is in the bytecode patcher.
 * This class is packaged with the original mod classes in one distributable jar.
 */
public final class WorldgenPolicy {
    private WorldgenPolicy() {}

    public static final String KEY = "key";
    public static final String EXIT = "exit";
    public static final String OVERWORLD_DUNGEON = "overworld_dungeon";
    public static final String SPHERE = "sphere";
    public static final String PUMPKIN = "pumpkin";

    public static int spacing(String id) {
        if (KEY.equals(id)) return 20;
        if (EXIT.equals(id)) return 24;
        if (OVERWORLD_DUNGEON.equals(id)) return 48;
        if (SPHERE.equals(id)) return 96;
        return 0;
    }

    /** Both native biomes belong to the Dead Dimension, so either dungeon may generate in both. */
    public static boolean biomeMatches(Object configured, Object current) {
        if (configured == null || current == null) return false;
        String actual = current.toString();
        if (!configured.toString().startsWith("horrormod:")) return configured.equals(current);
        return "horrormod:death".equals(actual) || "horrormod:dead_woods_biome".equals(actual);
    }

    /** Keep a recognizable portion of each NBT dungeon above the local terrain. */
    public static int chooseStructureY(java.util.Random rng, int minY, int terrainY) {
        return Math.max(minY, Math.min(225, terrainY - 4));
    }

    /** The original MCreator procedure checked one random block for deathblock,
     * which rejected nearly all otherwise valid dungeon positions. The caller
     * already checks the death dimension and is registered only in allowed biomes. */
    public static boolean allowDeathDungeon(Object dependencies) {
        if (!(dependencies instanceof java.util.Map)) return false;
        java.util.Map<?,?> m = (java.util.Map<?,?>) dependencies;
        return m.containsKey("world") && m.containsKey("x") && m.containsKey("y") && m.containsKey("z");
    }

    public static boolean shouldPlace(Object reader, Object blockPos, String id) {
        if (PUMPKIN.equals(id)) return false; // Spawn altar is built on entering /Secreat, not randomly.
        try {
            int cx = getCoord(blockPos, "func_177958_n", "getX") >> 4;
            int cz = getCoord(blockPos, "func_177952_p", "getZ") >> 4;
            Object world = call(reader, "func_201672_e", "getWorld");
            long seed = seed(world != null ? world : reader);
            int distance = spacing(id);
            if (distance <= 0) return false;
            int rx = Math.floorDiv(cx, distance), rz = Math.floorDiv(cz, distance);
            return cx == candidate(seed, id, rx, rz, false) && cz == candidate(seed, id, rx, rz, true);
        } catch (Exception e) {
            // An unavailable API must never turn this into uncontrolled worldgen.
            logOnce("generation coordinates", e);
            return false;
        }
    }

    public static int candidate(long seed, String id, int regionX, int regionZ, boolean z) {
        int spacing = spacing(id);
        long salt = id.hashCode() * 0x9e3779b97f4a7c15L;
        long h = mix(seed ^ salt ^ (regionX * 0x632be59bd9b4e019L) ^ (regionZ * 0x9e3779b97f4a7c15L)
                ^ (z ? 0x7a188a1fd6215f77L : 0x33e4d90318dd7cb9L));
        int radius = spacing / 4;
        int jitter = (int)Math.floorMod(h, (long)radius * 2 + 1) - radius;
        return (z ? regionZ : regionX) * spacing + spacing / 2 + jitter;
    }

    public static int[] nearest(long seed, int blockX, int blockZ) {
        long bestDist = Long.MAX_VALUE;
        int bestX = 0, bestZ = 0;
        for (String id : new String[]{KEY, EXIT}) {
            int distance = spacing(id);
            int regionX = Math.floorDiv(blockX >> 4, distance);
            int regionZ = Math.floorDiv(blockZ >> 4, distance);
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                int x = candidate(seed, id, regionX + dx, regionZ + dz, false) * 16 + 8;
                int z = candidate(seed, id, regionX + dx, regionZ + dz, true) * 16 + 8;
                long d = (long)(x - blockX) * (x - blockX) + (long)(z - blockZ) * (z - blockZ);
                // Once on top of a structure, guide toward another candidate.
                if (d < 32L * 32) continue;
                if (d < bestDist) { bestDist = d; bestX = x; bestZ = z; }
            }
        }
        return new int[]{bestX, 70, bestZ};
    }

    public static long seed(Object world) {
        try {
            Object v = call(world, "func_72905_C", "getSeed");
            return v instanceof Number ? ((Number)v).longValue() : 0;
        } catch (Exception e) { return 0; }
    }

    public static int getCoord(Object pos, String srg, String name) throws Exception {
        Object result = call(pos, srg, name);
        if (!(result instanceof Number)) throw new IllegalStateException("Cannot obtain BlockPos coordinate");
        return ((Number)result).intValue();
    }

    public static Object call(Object target, String... possibleNames) throws Exception {
        if (target == null) return null;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != 0) continue;
                for (String name : possibleNames) if (name.equals(m.getName())) {
                    m.setAccessible(true);
                    return m.invoke(target);
                }
            }
        }
        return null;
    }

    public static Object field(Object target, String... names) throws Exception {
        if (target == null) return null;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
                catch (NoSuchFieldException ignored) {}
            }
        }
        return null;
    }

    private static long mix(long x) {
        x ^= x >>> 30; x *= 0xbf58476d1ce4e5b9L;
        x ^= x >>> 27; x *= 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }


    public static Object invoke(Object target, String[] names, Object... args) throws Exception {
        if (target == null) return null;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != args.length) continue;
                boolean named = false;
                for (String name : names) if (name.equals(m.getName())) named = true;
                if (!named) continue;
                Class<?>[] types = m.getParameterTypes();
                boolean compatible = true;
                for (int i = 0; i < types.length; i++) {
                    if (args[i] == null) continue;
                    Class<?> type = types[i].isPrimitive() ? primitiveBox(types[i]) : types[i];
                    if (!type.isAssignableFrom(args[i].getClass())) {compatible = false; break;}
                }
                if (!compatible) continue;
                m.setAccessible(true);
                return m.invoke(target, args);
            }
        }
        return null;
    }
    private static Class<?> primitiveBox(Class<?> t) {
        if (t==int.class) return Integer.class;
        if (t==long.class) return Long.class;
        if (t==float.class) return Float.class;
        if (t==double.class) return Double.class;
        if (t==boolean.class) return Boolean.class;
        if (t==byte.class) return Byte.class;
        if (t==short.class) return Short.class;
        if (t==char.class) return Character.class;
        return t;
    }

    private static volatile boolean warned;
    public static void logOnce(String label, Exception e) {
        if (!warned) { warned = true; System.err.println("[Dead's Mod Unified] " + label + ": " + e); }
    }
}
