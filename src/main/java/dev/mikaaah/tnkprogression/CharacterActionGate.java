package dev.mikaaah.tnkprogression;

/** Transient per-connection pacing. Read-only refreshes cannot starve real selections. */
public final class CharacterActionGate {
 private long action=Long.MIN_VALUE,refresh=Long.MIN_VALUE;
 public boolean accept(long tick,boolean fullRefresh){
  long previous=fullRefresh?refresh:action;
  int interval=fullRefresh?20:2;
  if(previous!=Long.MIN_VALUE && tick>=previous && tick-previous<interval)return false;
  if(fullRefresh)refresh=tick;else action=tick;
  return true;
 }
}
