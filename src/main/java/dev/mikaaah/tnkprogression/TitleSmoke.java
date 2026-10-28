package dev.mikaaah.tnkprogression;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.network.chat.Component;

public final class TitleSmoke {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run(MinecraftServer server)throws Exception {
        var p=FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(UUID.fromString("657bf016-419a-43dd-9b37-e0f29f08009c"),"TitleTest"));
        var n=ProgressionAPI.profile(p);for(String key:Set.copyOf(n.getAllKeys()))n.remove(key);
        check(TitleConfig.current.titles().size()>=43,"catalog loaded");
        check(!Titles.grant(p,"tnk:invented"),"unknown title rejected");
        var dispatcher=server.getCommands().getDispatcher();
        check(dispatcher.execute("tnkprogress title grant @s tnk:ars_mystic",p.createCommandSourceStack().withPermission(2))==1,"FTB-style namespaced grant command");
        check(!Titles.grant(p,"tnk:ars_mystic"),"duplicate grant idempotent");
        check(Titles.selected(p).equals("tnk:ars_mystic"),"first title equipped");
        String expected=LevelBadges.glyph(ProgressionAPI.level(p))+" [Mystic] TitleTest";
        check(p.getDisplayName().getString().equals(expected),"readable no-pack name fallback");
        p.refreshDisplayName();check(p.getDisplayName().getString().equals(expected),"title prefix does not duplicate on refresh");
        check(Titles.grant(p,"tnk:dragon_slayer"),"second title granted");
        check(Titles.selected(p).equals("tnk:ars_mystic"),"new grant preserves selection");
        check(!Titles.select(p,"tnk:monarch"),"locked title cannot be selected");
        check(dispatcher.execute("tnkprogress title select tnk:dragon_slayer",p.createCommandSourceStack().withPermission(0))==1,"player can select an unlocked title");
        boolean denied=false;try{dispatcher.execute("tnkprogress title grant @s tnk:monarch",p.createCommandSourceStack().withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){denied=true;}
        check(denied,"regular player cannot grant titles");
        check(Titles.revoke(p,"tnk:dragon_slayer") && Titles.selected(p).isEmpty(),"revoke removes active prefix");
        check(!Titles.earn(p,"tnk:dragon_slayer"),"automatic sync respects revocation");
        check(Titles.grant(p,"tnk:dragon_slayer"),"explicit grant restores revoked title");
        check(Titles.selected(p).isEmpty(),"hidden selection stays hidden after grants");
        check(!Titles.select(p,"tnk:ars_mystic"),"perk swap cooldown cannot be bypassed by hide/restore");
        n.putLong("title_swap_after",0);
        check(Titles.select(p,"tnk:ars_mystic"),"select owned title again");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),p.registryAccess());
        try {
            var payload=new TitleBadgePayload(p.getUUID(),Titles.display(p));TitleBadgePayload.CODEC.encode(buffer,payload);
            var copy=TitleBadgePayload.CODEC.decode(buffer);check(copy.playerId().equals(p.getUUID()) && copy.display().equals(payload.display()) && buffer.readableBytes()==0,"title component payload roundtrip");
        }finally{buffer.release();}
        var saved=ProgressionData.get(server).save(new CompoundTag(),p.registryAccess());
        var loaded=ProgressionData.load(saved,p.registryAccess()).entry("player:"+p.getUUID());
        check(loaded.getCompound("titles_unlocked").getBoolean("tnk:ars_mystic") && loaded.getString("title_selected").equals("tnk:ars_mystic"),"titles and selection persist");
        var skill=TitleConfig.get("tnk:skill_wayfarer").orElseThrow();
        check(!TitleConfig.qualifies(skill,100,Set.of()) && TitleConfig.qualifies(skill,40,Set.of("hunter_subclass_1_mastery")),"node titles require actual converted skill unlock");
        var points=TitleConfig.get("tnk:paragon").orElseThrow();
        check(!TitleConfig.qualifies(points,99,Set.of()) && TitleConfig.qualifies(points,100,Set.of()),"spent points title threshold");
        Titles.checkSkills(p); // Missing optional Pufferfish never awards a substitute character-level title.
        check(!Titles.unlocked(p,"tnk:paragon"),"absent integration never invents skill purchases");
        check(dispatcher.execute("tnkprogress reward @s tnk:title-smoke 5",p.createCommandSourceStack().withPermission(2))==1,"namespaced one-time XP reward parses");
        check(dispatcher.execute("tnkprogress reward @s tnk:title-smoke 5",p.createCommandSourceStack().withPermission(2))==0,"namespaced XP reward cannot duplicate");
        check(dispatcher.execute("tnkprogress engineering @s tnk:first_workshop",p.createCommandSourceStack().withPermission(2))==1,"namespaced engineering commission parses");
        dispatcher.execute("tnkprogress title clear",p.createCommandSourceStack().withPermission(0));check(p.getDisplayName().getString().equals(LevelBadges.glyph(ProgressionAPI.level(p))+" TitleTest"),"hiding title retains only level and username");
        TnkProgression.LOG.info("TNK_TITLES_SMOKE_OK");
    }
}
