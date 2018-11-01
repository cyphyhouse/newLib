package testmain;

import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;

import java.util.Random;
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
         Random ran = new Random();

         if (this.destination.x > i.x) {
             //x = i.x + xdist + ran.nextInt((600) + 1)-300;
             x = i.x + xdist;
         }
         else {
             //x = i.x - xdist + ran.nextInt((600) + 1)-300;
             x = i.x - xdist;
         }

        if (this.destination.y > i.y) {
            //y = i.y + ydist + ran.nextInt((600) + 1)-300;
            y = i.y + ydist;
        }
        else {
            //y = i.y - ydist + ran.nextInt((600) + 1)-300;
            y = i.y - ydist;
        }

        if (this.destination.z > i.z) {
            z = i.z + zdist;
        }
        else {
            z = i.z - zdist;
        }

        return new ItemPosition("pathPoint",x,y,z);
    }


    public Stack<ItemPosition> getPath() {
        int pl = this.pathLength;
        Stack<ItemPosition> path = new Stack<ItemPosition>();
        Stack<ItemPosition> temp = new Stack<ItemPosition>();

        temp.push(this.start);
        int xdist = Math.abs(this.start.x - this.destination.x);
        int ydist = Math.abs(this.start.y - this.destination.y);
        int zdist = Math.abs(this.start.z - this.destination.z);
        //System.out.println("xdist: " + xdist + " ydist: " + ydist + " zdis: " + zdist);

        //int xincrement = xdist/pathLength;
        //int yincrement = ydist/pathLength;
        int xincrement = 0;
        int yincrement = 0;
        int zincrement = zdist/pathLength;
        //System.out.println("xinc: " + xincrement + " yinc: " + yincrement + " zinc: " + zincrement);
        
        ItemPosition i = this.start;
        while (pl >= 1) {
            i = getPointAtDistance(i,xincrement,yincrement,zincrement);
            if (zincrement > 0) {
                temp.push(i);
            }
            pl = pl - 1;
        }

        temp.push(destination);
        while (!temp.empty()){
            path.push(temp.pop());
        }
        return path;
    }


}
