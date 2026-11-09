package dev.mikaaah.tnkprogression;
import com.google.gson.*;
import java.nio.file.*;
import net.neoforged.fml.loading.FMLPaths;

/** Versioned curve: old values are retained solely for migration of offline characters. */
public final class BalanceConfig {
 public int version=3;
 public double levelBase=0,levelFactor=1000,levelExponent=1;
 public double legacyLevelBase=150,legacyLevelFactor=45,legacyLevelExponent=1.35;
 /** Kept for binary/source compatibility with old integrations; no activity milestones are awarded. */
 public double activityGrowth=.12,requirementExponent=.65,rewardExponent=.50;
 public static BalanceConfig current=new BalanceConfig();
 public static boolean reload(){try{
  var path=FMLPaths.CONFIGDIR.get().resolve("tnk-balance.json");var g=new GsonBuilder().setPrettyPrinting().create();
  BalanceConfig c;
  if(Files.exists(path)){
   var tree=JsonParser.parseString(Files.readString(path)).getAsJsonObject();c=g.fromJson(tree,BalanceConfig.class);
   if(!tree.has("version")||tree.get("version").getAsInt()<3){
    c.legacyLevelBase=c.levelBase;c.legacyLevelFactor=c.levelFactor;c.legacyLevelExponent=c.levelExponent;
    c.levelBase=0;c.levelFactor=1000;c.levelExponent=1;c.version=3;
    if(!Files.exists(path.resolveSibling("tnk-balance.pre-direct-xp.json")))Files.copy(path,path.resolveSibling("tnk-balance.pre-direct-xp.json"));
   }
  }else c=new BalanceConfig();
  for(double v:new double[]{c.levelBase,c.levelFactor,c.levelExponent,c.legacyLevelBase,c.legacyLevelFactor,c.legacyLevelExponent})if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException("Invalid curve");
  if(c.levelFactor<=0||c.levelExponent<=0||c.levelExponent>3||c.levelFactor>10000||c.levelBase>10000)throw new IllegalArgumentException("Unbounded curve");
  Files.writeString(path,g.toJson(c));current=c;return true;
 }catch(Exception ex){TnkProgression.LOG.error("Keeping previous balance preset",ex);return false;}}
 public static long bounded(double value){return value>=Long.MAX_VALUE?Long.MAX_VALUE:Math.max(1,(long)value);}
 public long previousCost(int level){return bounded(legacyLevelBase+legacyLevelFactor*Math.pow(level,legacyLevelExponent));}
}
