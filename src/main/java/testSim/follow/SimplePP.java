package testSim.follow;

import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;

import java.util.Stack;

public class SimplePP {
    public int pathLength;
    public ItemPosition destination;
    public ItemPosition start;

    public SimplePP(ItemPosition start, ItemPosition destination, int pathLength){
        this.start = start;
        this.destination = destination;
        this.pathLength = pathLength;
    }

    public ItemPosition getPointAtDistance(ItemPosition i, int xdist,int ydist, int zdist) {
         int x = 0;
         int y = 0;
         int z = 0;
         if (this.destination.x > i.x) {
             x = i.x + xdist;
         }
         else {
             x = i.x - xdist;
         }

        if (this.destination.x > i.x) {
            y = i.x + xdist;
        }
        else {
            y = i.x - xdist;
        }

        if (this.destination.x > i.x) {
            z = i.x + xdist;
        }
        else {
            z = i.x - xdist;
        }

        return new ItemPosition("pathPoint",x,y,z);
    }


    public Stack<ItemPosition> getPath() {
        int pl = this.pathLength;
        Stack<ItemPosition> path = new Stack<ItemPosition>();
        path.push(this.start);
        int xdist = Math.abs(this.start.x - this.destination.x);
        int ydist = Math.abs(this.start.y - this.destination.y);
        int zdist = Math.abs(this.start.z - this.destination.z);
        int xincrement = xdist/pathLength;
        int yincrement = ydist/pathLength;
        int zincrement = zdist/pathLength;
        ItemPosition i = this.start;
        while (pl >= 1) {
            i = getPointAtDistance(i,xincrement,yincrement,zincrement);
            path.push(i);
            pathLength = pl - 1;
        }
        path.push(destination);
        return path;
    }


}
