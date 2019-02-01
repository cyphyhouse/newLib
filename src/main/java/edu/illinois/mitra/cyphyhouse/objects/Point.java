package edu.illinois.mitra.cyphyhouse.objects;

/**
 * Created by SC on 11/11/16.
 */
public class Point {
    public double x,y;
    public Point(double x, double y){
        set(x, y);
    }
    public Point(Point src){
        set(src.x, src.y);
    }

    public final boolean equals(double x, double y){
        return (this.x==x && this.y==y);
    }

    public void set(double x, double y){
        this.x = x;
        this.y = y;
    }
}
