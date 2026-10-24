package io.github.riveyder.shahed.item;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
/** Head equipment without armor bonuses, durability, or maintenance. */
public final class GogglesItem extends Item implements Equipable {
 public GogglesItem(Properties p){super(p);}
 @Override public EquipmentSlot getEquipmentSlot(){return EquipmentSlot.HEAD;}
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){return swapWithEquipmentSlot(this,l,p,h);}
}
