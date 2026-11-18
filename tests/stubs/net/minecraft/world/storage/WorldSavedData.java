package net.minecraft.world.storage;
import net.minecraft.nbt.CompoundNBT;
public abstract class WorldSavedData {
    private boolean dirty;
    public WorldSavedData(String id) {}
    public abstract void func_76184_a(CompoundNBT nbt);
    public abstract CompoundNBT func_189551_b(CompoundNBT nbt);
    public void func_76185_a() {dirty=true;}
    public void setDirty(){dirty=true;}
    public boolean isDirty(){return dirty;}
}
