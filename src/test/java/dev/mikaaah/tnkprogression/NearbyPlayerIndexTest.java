package dev.mikaaah.tnkprogression;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NearbyPlayerIndexTest {
 @Test void emptyWorldHasNoCandidate(){assertNull(new NearbyPlayerIndex<String>(List.of()).nearest(0,64,0));}
 @Test void sameCellKeepsDifferentNearestPlayersAcrossTheBoundary(){
  var index=new NearbyPlayerIndex<>(List.of(new NearbyPlayerIndex.Point<>("left",0,64,0),new NearbyPlayerIndex.Point<>("right",16,64,0)));
  assertEquals("left",index.nearest(1,64,0));assertEquals("right",index.nearest(15,64,0));
  assertEquals(1,index.searches(),"same area reuses its shortlist without locking in the first player");
 }
 @Test void cachedSelectionMatchesFullSearchAtRandomPositions(){
  var random=new Random(7291);var points=new ArrayList<NearbyPlayerIndex.Point<Integer>>();
  for(int p=0;p<150;p++)points.add(new NearbyPlayerIndex.Point<>(p,random.nextDouble()*2000-1000,random.nextDouble()*600-300,random.nextDouble()*2000-1000));
  var index=new NearbyPlayerIndex<>(points);
  for(int i=0;i<10000;i++){
   double x=random.nextDouble()*2400-1200,y=random.nextDouble()*700-350,z=random.nextDouble()*2400-1200;
   Integer nearest=null;double best=Double.POSITIVE_INFINITY;
   for(var p:points){double d=p.distanceSquared(x,y,z);if(d<best){best=d;nearest=p.value();}}
   assertEquals(nearest,index.nearest(x,y,z));
  }
 }
 @Test void burstReusesAreaAndCacheMemoryIsBounded(){
  var index=new NearbyPlayerIndex<>(List.of(new NearbyPlayerIndex.Point<>(1,0,64,0),new NearbyPlayerIndex.Point<>(2,200,64,0)));
  for(int i=0;i<31;i++)assertEquals(1,index.nearest(i%16,64,i%16));
  assertEquals(1,index.searches());
  for(int i=0;i<10000;i++)index.nearest(i*16,64,0);
  assertTrue(index.cachedCells()<=2048);
 }
}
