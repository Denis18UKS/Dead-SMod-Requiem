package net.mcreator.horrormod.unified;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.WorldSavedData;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Actual, successfully placed template locations, rather than hypothetical grid coordinates.
 * WorldSavedData persists the entries in the world save. Java names + SRG bridges support
 * the 1.16.5 source- and runtime-namespaces without shipping Minecraft stub classes.
 */
public final class StructureRecords extends WorldSavedData {
    private static final String DATA_NAME = "horrormod_requiem_structures_v2";
    private static final ConcurrentLinkedQueue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private final List<Site> sites = new ArrayList<>();

    public StructureRecords() { super(DATA_NAME); }

    public static final class Site {
        public final int x, y, z;
        public final String type;
        public Site(String type, int x, int y, int z) {this.type=type;this.x=x;this.y=y;this.z=z;}
    }
    private static final class Pending {
        final Object world; final Site site;
        Pending(Object world, Site site) {this.world=world;this.site=site;}
    }

    public static void placed(Object reader, Object blockPos, String type) {
        // Called by injected instruction immediately after Template.addBlocksToWorld (void).
        // Never assume the candidate chunk has generated a structure until this is invoked.
        try {
            if (!WorldgenPolicy.KEY.equals(type) && !WorldgenPolicy.EXIT.equals(type)) return;
            Object world = WorldgenPolicy.call(reader, "func_201672_e", "getWorld");
            if (world == null) return;
            QUEUE.add(new Pending(world,new Site(type,
                    WorldgenPolicy.getCoord(blockPos,"func_177958_n","getX"),
                    WorldgenPolicy.getCoord(blockPos,"func_177956_o","getY"),
                    WorldgenPolicy.getCoord(blockPos,"func_177952_p","getZ"))));
        } catch (Exception ex) { WorldgenPolicy.logOnce("record structure", ex); }
    }

    /** Call on server main thread, not from world-generation worker threads. */
    public static void flush(Object world) throws Exception {
        if (world == null) return;
        List<Pending> deferred=new ArrayList<>();
        Pending item;
        while ((item=QUEUE.poll())!=null) {
            if (item.world==world) {
                try {get(world).add(item.site);}
                catch (Exception e) {
                    QUEUE.add(item); // Do not lose a recorded structure if saved-data access fails.
                    QUEUE.addAll(deferred);
                    throw e;
                }
            } else deferred.add(item);
        }
        QUEUE.addAll(deferred);
    }

    public static Site nearest(Object world, int x, int z) throws Exception {
        StructureRecords data=get(world);
        synchronized (data) {
            Site nearest=null; long shortest=Long.MAX_VALUE;
            for (Site s:data.sites) {
                long dx=(long)s.x-x, dz=(long)s.z-z, dist=dx*dx+dz*dz;
                if (dist < shortest) {shortest=dist; nearest=s;}
            }
            return nearest;
        }
    }

    private synchronized void add(Site site) {
        for (Site old: sites) if (old.x==site.x && old.z==site.z && old.type.equals(site.type)) return;
        sites.add(site);
        // WorldSavedData#setDirty, a stable 1.16.5 API; the SRG call is used at runtime.
        try {
            Object res=WorldgenPolicy.call(this,"func_76185_a","setDirty");
            if (res==null) { /* void methods return null, including successful calls */ }
        } catch (Exception e) {WorldgenPolicy.logOnce("mark persistent structure index dirty",e);}
    }

    public void func_76184_a(CompoundNBT nbt) {loadEntries(nbt);}
    public CompoundNBT func_189551_b(CompoundNBT nbt) {return saveEntries(nbt);}
    public void load(CompoundNBT nbt) {loadEntries(nbt);}
    public CompoundNBT save(CompoundNBT nbt) {return saveEntries(nbt);}

    private synchronized void loadEntries(CompoundNBT nbt) {
        sites.clear();
        try {
            String raw=(String)WorldgenPolicy.invoke(nbt,new String[]{"func_74779_i","getString"},"ActualLocations");
            if (raw==null || raw.length()>4000000) return;
            for (String line:raw.split("\n")) {
                String[] part=line.split(",",4);
                if (part.length != 4 || (!WorldgenPolicy.KEY.equals(part[0]) && !WorldgenPolicy.EXIT.equals(part[0]))) continue;
                sites.add(new Site(part[0],Integer.parseInt(part[1]),Integer.parseInt(part[2]),Integer.parseInt(part[3])));
            }
        } catch (Exception e) {WorldgenPolicy.logOnce("load saved structure locations",e);}
    }
    private synchronized CompoundNBT saveEntries(CompoundNBT nbt) {
        StringBuilder raw=new StringBuilder();
        for (Site s:sites) raw.append(s.type).append(',').append(s.x).append(',').append(s.y).append(',').append(s.z).append('\n');
        try {WorldgenPolicy.invoke(nbt,new String[]{"func_74778_a","putString"},"ActualLocations",raw.toString());}
        catch (Exception e) {WorldgenPolicy.logOnce("write saved structure locations",e);}
        return nbt;
    }

    private static StructureRecords get(Object world) throws Exception {
        Object store = findDataStorage(world);
        if (store==null) throw new IllegalStateException("No DimensionSavedDataManager found on " + world.getClass());
        for (Method m: store.getClass().getMethods()) {
            Class<?>[] param=m.getParameterTypes();
            if (param.length==2 && Supplier.class.isAssignableFrom(param[0]) && param[1]==String.class
                    && (m.getName().equals("func_215752_a") || m.getName().equals("computeIfAbsent") || m.getName().equals("getOrCreate"))) {
                Object result=m.invoke(store,(Supplier<StructureRecords>)StructureRecords::new,DATA_NAME);
                if (result instanceof StructureRecords) return (StructureRecords)result;
            }
        }
        throw new IllegalStateException("Missing storage getOrCreate(Supplier,String)");
    }
    private static Object findDataStorage(Object world) throws Exception {
        Object found=findGetter(world,"net.minecraft.world.storage.DimensionSavedDataManager");
        if (found!=null) return found;
        Object provider=WorldgenPolicy.call(world,"func_72863_F","getChunkProvider","getChunkSource");
        return findGetter(provider,"net.minecraft.world.storage.DimensionSavedDataManager");
    }
    private static Object findGetter(Object target, String wanted) throws Exception {
        if (target==null) return null;
        for (Method m: target.getClass().getMethods()) {
            if (m.getParameterCount()==0 && m.getReturnType().getName().equals(wanted)) return m.invoke(target);
        }
        return null;
    }
}
