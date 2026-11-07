package dev.mikaaah.tnkprogression.mixin;
import dev.mikaaah.tnkprogression.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Villager.class)
public abstract class OriginTradingMixin {
    @Unique private final Map<MerchantOffer,Integer> tnk$discounts = new IdentityHashMap<>();
    @Inject(method="updateSpecialPrices", at=@At("HEAD"))
    private void tnk$clearOwnDiscount(Player player, CallbackInfo callback) {
        tnk$discounts.forEach((offer, discount) -> offer.addToSpecialPriceDiff(discount));
        tnk$discounts.clear();
    }
    @Inject(method="updateSpecialPrices", at=@At("RETURN"))
    private void tnk$buyingPrices(Player player, CallbackInfo callback) {
        if (!(player instanceof ServerPlayer serverPlayer) || !ActivityGuard.player(serverPlayer)) return;
        double fraction = Origins.bonus(player, OriginRules.Perk.TRADE_DISCOUNT);
        if (fraction <= 0) return;
        for (var offer : ((Villager)(Object)this).getOffers()) {
            int discount = (int)Math.floor(offer.getCostA().getCount() * fraction);
            if (discount > 0) { offer.addToSpecialPriceDiff(-discount); tnk$discounts.put(offer, discount); }
        }
    }
    @Inject(method="resetSpecialPrices", at=@At("RETURN"))
    private void tnk$closed(CallbackInfo callback) { tnk$discounts.clear(); }
    @ModifyArg(method="rewardTradeXp", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/ExperienceOrb;<init>(Lnet/minecraft/world/level/Level;DDDI)V"), index=4)
    private int tnk$tradeXp(int amount) {
        if (!(((Villager)(Object)this).getTradingPlayer() instanceof ServerPlayer player) || !ActivityGuard.player(player)) return amount;
        return amount + Origins.extra(player, "trading_xp", amount, Origins.bonus(player, OriginRules.Perk.TRADE_XP));
    }
}
