package dev.mikaaah.tnkprogression;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Titles are personal, permanent cosmetic achievements. The wearer selects one, or hides them. */
public final class Titles {
    public static final ResourceLocation FONT=ResourceLocation.parse("tnkprogression:title_badges");
    private static final Map<UUID,Component> CLIENT_TITLES=new HashMap<>();
    public static boolean unlocked(ServerPlayer p,String id){return ProgressionAPI.profile(p).getCompound("titles_unlocked").getBoolean(id);}
    public static String selected(ServerPlayer p){
        String id=ProgressionAPI.profile(p).getString("title_selected");
        return unlocked(p,id) && TitleConfig.get(id).isPresent()?id:"";
    }
    public static Component display(ServerPlayer p){return TitleConfig.get(selected(p)).map(Titles::badge).orElse(Component.empty());}
    public static Component badge(TitleConfig.Definition title){
        // Language fallback stays readable without the optional artwork pack. Its translations select original glyphs.
        return Component.translatableWithFallback(title.translationKey(),"["+title.label()+"]")
            .withStyle(Style.EMPTY.withFont(FONT).withColor(0xFFFFFF).withBold(false).withItalic(false)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Component.literal(title.label()+" — "+title.description())
                    .withStyle(Style.EMPTY.withFont(ResourceLocation.withDefaultNamespace("default")).withColor(title.rgb())))));
    }
    public static boolean earn(ServerPlayer p,String id){
        return !ProgressionAPI.profile(p).getCompound("titles_revoked").getBoolean(id) && unlock(p,id);
    }
    public static boolean grant(ServerPlayer p,String id){
        if(TitleConfig.get(id).isEmpty())return false;
        var n=ProgressionAPI.profile(p);var revoked=n.getCompound("titles_revoked");revoked.remove(id);n.put("titles_revoked",revoked);ProgressionData.get(p.server).setDirty();
        return unlock(p,id);
    }
    private static boolean unlock(ServerPlayer p,String id){
        var def=TitleConfig.get(id);if(def.isEmpty() || unlocked(p,id))return false;
        CompoundTag n=ProgressionAPI.profile(p),earned=n.getCompound("titles_unlocked");earned.putBoolean(id,true);n.put("titles_unlocked",earned);
        if(TitleConfig.current.autoEquipFirst() && !n.contains("title_selected"))n.putString("title_selected",id);
        ProgressionData.get(p.server).setDirty();LevelBadges.changed(p);
        p.sendSystemMessage(Component.literal("Title unlocked: "+def.get().label()).withStyle(style->style.withColor(def.get().rgb())));
        return true;
    }
    public static boolean select(ServerPlayer p,String id){
        if(!id.isEmpty() && (!unlocked(p,id) || TitleConfig.get(id).isEmpty()))return false;
        ProgressionAPI.profile(p).putString("title_selected",id);ProgressionData.get(p.server).setDirty();LevelBadges.changed(p);return true;
    }
    public static boolean revoke(ServerPlayer p,String id){
        if(TitleConfig.get(id).isEmpty() && !unlocked(p,id))return false;
        var n=ProgressionAPI.profile(p);var revoked=n.getCompound("titles_revoked");revoked.putBoolean(id,true);n.put("titles_revoked",revoked);
        var earned=n.getCompound("titles_unlocked");earned.remove(id);n.put("titles_unlocked",earned);
        if(n.getString("title_selected").equals(id))n.putString("title_selected","");
        ProgressionData.get(p.server).setDirty();LevelBadges.changed(p);return true;
    }
    public static Component client(UUID id){return CLIENT_TITLES.getOrDefault(id,Component.empty());}
    public static void accept(UUID id,Component display,net.minecraft.world.entity.player.Player viewer){
        CLIENT_TITLES.put(id,display.copy());var target=viewer.level().getPlayerByUUID(id);if(target!=null)target.refreshDisplayName();
    }
    public static void clearClient(){CLIENT_TITLES.clear();}
    public static void checkSkills(ServerPlayer p){
        if(!ActivityGuard.player(p))return;
        Map<String,Optional<SkillBridge.TitleSnapshot>> snapshots=new HashMap<>();
        for(var def:TitleConfig.current.titles()) {
            if(!def.automatic() || unlocked(p,def.id()))continue;
            var snapshot=snapshots.computeIfAbsent(def.skillCategory(),category->SkillBridge.titleSnapshot(p,category));
            if(snapshot.isPresent() && TitleConfig.qualifies(def,snapshot.get().spent(),snapshot.get().unlocked()))earn(p,def.id());
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p)checkSkills(p);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p && p.tickCount%100==0)checkSkills(p);}
}
