"""Reject crossing links and links passing through unrelated node frames."""
import itertools,math

def inspect_links(skills,definitions,edges):
    positions={key:(node['x'],node['y']) for key,node in skills.items()}
    def cross(a,b,c):return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
    crossings=[];overlaps=[];obstructions=[]
    for (a,b),(c,d) in itertools.combinations(edges,2):
        if len({a,b,c,d})<4:continue
        u,v,w,z=positions[a],positions[b],positions[c],positions[d]
        if cross(u,v,w)*cross(u,v,z)<0 and cross(w,z,u)*cross(w,z,v)<0:
            crossings.append([[a,b],[c,d]])
        elif cross(u,v,w)==0 and cross(u,v,z)==0:
            axis=0 if abs(u[0]-v[0])>=abs(u[1]-v[1]) else 1
            first=sorted((u[axis],v[axis]));second=sorted((w[axis],z[axis]))
            if min(first[1],second[1])>max(first[0],second[0]):overlaps.append([[a,b],[c,d]])
    for a,b in edges:
        ax,ay=positions[a];bx,by=positions[b];vx,vy=bx-ax,by-ay;length=vx*vx+vy*vy
        for key,(x,y) in positions.items():
            if key in (a,b):continue
            t=((x-ax)*vx+(y-ay)*vy)/length
            if not 0<t<1:continue
            distance=math.hypot(x-ax-t*vx,y-ay-t*vy)
            radius=13*definitions[key]['size']
            if distance<radius+1:
                obstructions.append({'edge':[a,b],'node':key,'distance':round(distance,2),'frame_radius':radius})
    return {'crossing_links':crossings,'collinear_overlaps':overlaps,'unrelated_node_obstructions':obstructions}
