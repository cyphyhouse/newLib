package edu.illinois.mitra.cyphyhouse.objects;

/**
 * Created by SC on 11/11/16.
 */
public class Point {
    public int x,y;
    public Point(int x, int y){
        set(x, y);
    }
    public Point(Point src){
        set(src.x, src.y);
    }

    public final boolean equals(int x, int y){
        return (this.x==x && this.y==y);
    }

    public void set(int x, int y){
        this.x = x;
        this.y = y;
    }
}
