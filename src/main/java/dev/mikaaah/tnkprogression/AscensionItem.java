package dev.mikaaah.tnkprogression;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.*;

/** Preserves the consumable origin reselection introduced in 1.4.0. */
public final class AscensionItem extends Item {
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("tnkprogression");
 public static final DeferredItem<AscensionItem> SIGIL=ITEMS.register("origin_ascension",()->new AscensionItem(new Properties().stacksTo(1).fireResistant()));
 private AscensionItem(Properties properties){super(properties);}
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  var stack=player.getItemInHand(hand);if(level.isClientSide)return InteractionResultHolder.success(stack);
  if(!(player instanceof ServerPlayer p)||p instanceof FakePlayer||!stack.is(this))return InteractionResultHolder.fail(stack);
  Origins.ascend(p);stack.consume(1,p);return InteractionResultHolder.consume(stack);
 }
 @Override public boolean isFoil(ItemStack stack){return true;}
 @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
  lines.add(Component.literal("Unlock 15 ascended origins and choose your origin again."));
  lines.add(Component.literal("Keeps your levels, skills, titles and activity progress."));
  lines.add(Component.literal("Consumed on use. Further changes require another sigil."));
 }
}
