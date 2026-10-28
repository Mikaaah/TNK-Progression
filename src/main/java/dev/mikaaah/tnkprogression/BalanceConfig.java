package dev.mikaaah.tnkprogression;

import com.google.gson.*;
import java.nio.file.*;
import net.neoforged.fml.loading.FMLPaths;

/** Reviewable release preset; empirical pack balance still needs playtesting. */
public final class BalanceConfig {
 public double levelBase=150,levelFactor=45,levelExponent=1.35;
 public double activityGrowth=0.12,requirementExponent=0.65,rewardExponent=0.50;
 public static BalanceConfig current=new BalanceConfig();
 public static boolean reload(){try{
  var path=FMLPaths.CONFIGDIR.get().resolve("tnk-balance.json");var g=new GsonBuilder().setPrettyPrinting().create();
  if(!Files.exists(path))Files.writeString(path,g.toJson(new BalanceConfig()));
  var c=g.fromJson(Files.readString(path),BalanceConfig.class);
  for(double v:new double[]{c.levelBase,c.levelFactor,c.levelExponent,c.activityGrowth,c.requirementExponent,c.rewardExponent})if(!Double.isFinite(v)||v<=0)throw new IllegalArgumentException("Invalid curve");
  if(c.levelExponent>3 || c.requirementExponent>2 || c.rewardExponent>c.requirementExponent || c.levelFactor>10000 || c.levelBase>10000 || c.activityGrowth>1)throw new IllegalArgumentException("Unbounded curve");
  current=c;return true;
 }catch(Exception ex){TnkProgression.LOG.error("Keeping previous balance preset",ex);return false;}}
 public static long bounded(double value){return value>=Long.MAX_VALUE?Long.MAX_VALUE:Math.max(1,(long)value);}
}
