package dev.mikaaah.tnkprogression;

import java.util.*;

/** Immutable player-position snapshot, with bounded candidate caches for 16-block cubes. */
public final class NearbyPlayerIndex<T> {
 public record Point<T>(T value,double x,double y,double z){
  double distanceSquared(double px,double py,double pz){double dx=x-px,dy=y-py,dz=z-pz;return dx*dx+dy*dy+dz*dz;}
 }
 private record Cell(int x,int y,int z){}
 private static final int MAX_CELLS=2048;
 private static final double DIAMETER=16*Math.sqrt(3);
 private final List<Point<T>> players;
 private final Map<Cell,List<Point<T>>> cells=new HashMap<>();
 private int searches;
 public NearbyPlayerIndex(List<Point<T>> players){this.players=List.copyOf(players);}
 public T nearest(double x,double y,double z){
  if(players.isEmpty())return null;
  if(players.size()==1)return players.getFirst().value;
  var cell=new Cell((int)Math.floor(x/16),(int)Math.floor(y/16),(int)Math.floor(z/16));
  var candidates=cells.get(cell);
  if(candidates==null){
   searches++;
   double cx=cell.x*16.0+8,cy=cell.y*16.0+8,cz=cell.z*16.0+8,best=Double.POSITIVE_INFINITY;
   for(var p:players)best=Math.min(best,p.distanceSquared(cx,cy,cz));
   double limit=Math.sqrt(best)+DIAMETER;limit=limit*limit+1e-6;
   var found=new ArrayList<Point<T>>();
   // Triangle inequality: nearest-center + twice the cube radius includes everyone who could win in this cube.
   for(var p:players)if(p.distanceSquared(cx,cy,cz)<=limit)found.add(p);
   candidates=List.copyOf(found);
   if(cells.size()>=MAX_CELLS)cells.clear();
   cells.put(cell,candidates);
  }
  T nearest=null;double best=Double.POSITIVE_INFINITY;
  for(var p:candidates){double distance=p.distanceSquared(x,y,z);if(distance<best){best=distance;nearest=p.value;}}
  return nearest;
 }
 int searches(){return searches;}
 int cachedCells(){return cells.size();}
}
