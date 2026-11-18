package net.minecraftforge.event.entity.player;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.server.ServerWorld;
public class PlayerInteractEvent {
    private final PlayerEntity player;
    private final ItemStack stack;
    private final ServerWorld world;
    private boolean canceled;
    public PlayerInteractEvent(PlayerEntity p,ItemStack s,ServerWorld w){player=p;stack=s;world=w;}
    public PlayerEntity getPlayer(){return player;}
    public ItemStack getItemStack(){return stack;}
    public ServerWorld getWorld(){return world;}
    public void setCanceled(boolean canceled){this.canceled=canceled;}
    public boolean isCanceled(){return canceled;}
    public void setCancellationResult(Object result){}
    public static class RightClickItem extends PlayerInteractEvent {
        public RightClickItem(PlayerEntity p,ItemStack s,ServerWorld w){super(p,s,w);}
    }
    public static class RightClickBlock extends PlayerInteractEvent {
        public RightClickBlock(PlayerEntity p,ItemStack s,ServerWorld w){super(p,s,w);}
    }
}
