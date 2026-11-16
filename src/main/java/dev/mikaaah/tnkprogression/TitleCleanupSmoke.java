package dev.mikaaah.tnkprogression;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in dedicated-server checks; never executes in a normal pack. */
public final class TitleCleanupSmoke {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void run(ServerStartedEvent event){
        if(!Boolean.getBoolean("tnkprogression.titleCleanupSmoke"))return;
        var server=event.getServer();var world=server.overworld();
        var profile=new GameProfile(UUID.randomUUID(),"TitleCleanupTest");
        var player=new ServerPlayer(server,world,profile,ClientInformation.createDefault());
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player,CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        player.setGameMode(GameType.SURVIVAL);world.players().add(player);
        try{
            verifyMigration();
            check(TitleConfig.current.titles().size()==42,"42 active titles");
            check(TitleConfig.current.titles().stream().map(d->d.label().toLowerCase(Locale.ROOT)).distinct().count()==42,"no duplicate active names");
            check(TitleConfig.legacy().size()==16,"legacy definitions preserved");
            var n=ProgressionAPI.profile(player);n.putInt("level",42);n.putLong("xp",1234);
            check(Titles.grant(player,"tnk:letsdo_creator"),"legacy title IDs still grantable by authorized commands");
            check(Titles.selectPassive(player,"tnk:letsdo_creator"),"old passive remains selectable");
            TitlePerks.apply(player);var perks=TitlePerks.forTitle("tnk:letsdo_creator");
            check(perks.size()==1 && perks.getFirst().amount()==.08,"original legacy bonuses retained");
            var snapshot=CharacterState.snapshot(player);
            check(snapshot.titles().stream().filter(CharacterState.Title::legacy).count()==1,"only owned legacy badges are sent");
            check(snapshot.titles().stream().filter(t->!t.legacy()).count()==42,"normal catalogue excludes legacy colour variants");
            check(CharacterState.JSON.fromJson(CharacterState.JSON.toJson(snapshot),CharacterState.Snapshot.class).equals(snapshot),"legacy and unlock info survive packet JSON roundtrip");
            check(TitleConfig.reload(),"catalogue migration/reload succeeds");
            check(Titles.selected(player).equals("tnk:letsdo_creator") && Titles.passive(player).equals("tnk:letsdo_creator"),"reload preserves both selected slots");
            var saved=ProgressionData.get(server).save(new CompoundTag(),world.registryAccess());
            var loaded=ProgressionData.load(saved,world.registryAccess()).entry("player:"+player.getUUID());
            check(loaded.getCompound("titles_unlocked").getBoolean("tnk:letsdo_creator") && loaded.getInt("level")==42 && loaded.getLong("xp")==1234,"title ownership and XP persist");
            Titles.checkSkills(player);
            for(var title:TitleConfig.current.titles())if(title.description().startsWith("Quest reward:"))check(!Titles.unlocked(player,title.id()),"no automatic quest or boss grants: "+title.id());
            var source=player.createCommandSourceStack().withPermission(2);
            check(server.getCommands().getDispatcher().execute("tnkprogress title earn @s tnk:engineer",source)==1,"quest command awards chapter title");
            check(server.getCommands().getDispatcher().execute("tnkprogress title earn @s tnk:engineer",source)==0,"repeated quest command is idempotent");
            Titles.revoke(player,"tnk:engineer");
            check(server.getCommands().getDispatcher().execute("tnkprogress title earn @s tnk:engineer",source)==0,"quest earn respects revocations");
            check(!Titles.earn(player,"tnk:owner"),"staff titles cannot be earned by quest commands");
            check(Titles.grant(player,"tnk:owner") && Titles.cosmeticOnly("tnk:owner"),"owner stays staff-only and cosmetic");
            check(TitleConfig.get("tnk:owner").orElseThrow().colour().equals("7DABF2"),"Owner uses clean blue");
            check(Titles.selected(player).equals("tnk:letsdo_creator"),"new grants never replace saved cosmetic choice");
            TnkProgression.LOG.info("TNK_TITLE_CLEANUP_SMOKE_OK: 42 unique active / 16 legacy, original perks, saved ownership and slots, JSON roundtrip, manual quest commands, revocation, no automatic quest or boss awards, blue cosmetic Owner");
        }catch(Throwable failure){TnkProgression.LOG.error("TNK_TITLE_CLEANUP_SMOKE_FAILED",failure);}
        finally{world.players().remove(player);server.halt(false);}
    }
    private static void verifyMigration() throws Exception {
        var path=net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("tnk-titles.json");
        String original=java.nio.file.Files.readString(path);
        try {
            var json=com.google.gson.JsonParser.parseString(original).getAsJsonObject();
            for(var element:json.getAsJsonArray("titles")){
                var title=element.getAsJsonObject();
                if(title.get("id").getAsString().equals("tnk:skill_wayfarer")){
                    var skills=new com.google.gson.JsonArray();skills.add("wayfarer_10");
                    title.add("requiredSkills",skills);title.addProperty("skillCategory","puffish_skills:character");
                }
                if(title.get("id").getAsString().equals("tnk:planner"))title.addProperty("description","Quest reward: complete the pack's custom storage quest.");
            }
            java.nio.file.Files.writeString(path,json.toString());
            check(TitleConfig.reload(),"legacy skill catalogue migration succeeds");
            check(TitleConfig.get("tnk:skill_wayfarer").orElseThrow().requiredSkills().equals(List.of("hunter_subclass_1_mastery")),"obsolete node migrates even after category was already renamed");
            check(TitleConfig.get("tnk:planner").orElseThrow().description().contains("custom storage quest"),"custom quest descriptions survive reload");
        } finally {java.nio.file.Files.writeString(path,original);check(TitleConfig.reload(),"restore disposable catalogue fixture");}
    }
}
