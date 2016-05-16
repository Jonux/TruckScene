/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package truckscene;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 *
 * @author Laatikko
 */
public class GraphCurve {
    
    private double[] points;
    private int timer;
    private int startTimer;
    private int curveTime;
    
    private Point position;
    private Point size;

    private int curveWidth;
    private PGraphics pg;
    
    private PApplet applet;
    private String topic;
    private int topicBarSize;
    
    /*
        @curveTime milliseconds
        @points values between 0 - 100%
        @curveWidth pixels
    */
    public GraphCurve(PApplet applet, Point pos, Point size, int curveTime, double[] points, int curveWidth, String topic){
        this.applet = applet;
        this.position = pos;
        this.size = size;
        this.curveTime = curveTime;
        this.curveWidth = curveWidth;
        this.timer = applet.millis();
        this.points = points;
        this.startTimer = applet.millis();
        this.topic = topic;
        this.topicBarSize = 60;
        // this.pg = applet.createGraphics(size.x, size.y);
    }
    
    public void setProprties() {
        this.pg.background(100);
        this.pg.stroke(255);
    }
    

    public void setup() {
       // this.pg = applet.createGraphics(size.x, size.y);
    }
    
    public boolean draw() {
        if (pg == null) {
            pg = applet.createGraphics(size.x, size.y + topicBarSize);
        }

        // Curve finished
        if (applet.millis() > startTimer + curveTime) {
            applet.image(pg, position.x, position.y);
            return false;
        }

        float timeElapsed = curveTime - (startTimer + curveTime - applet.millis());
        float currentPosX = (timeElapsed / curveTime) * size.x;

        pg.beginDraw();

        // Topic background
        pg.stroke(0, 0, 0);
        pg.strokeWeight(4);
        pg.fill(50,50,50, 10);
        pg.rect(0, 0, size.x, topicBarSize);
        
        // Topic text
        pg.textSize(36);
        pg.fill(255,255,255);
        pg.text(topic, 30, (int)(topicBarSize*0.8));
        
        // Curve background
        pg.stroke(0, 0, 0);
        pg.fill(140,140,140, 5);
        pg.rect(0,topicBarSize,size.x,size.y);
        
        // Graph curve, Todo Optimize
        pg.fill(0, 255, 0);
        pg.stroke(0, 255, 0);
        pg.ellipseMode(pg.CENTER);
        for (int i=0; i<points.length-3; i++) {
            
            double px1 = i * size.x / points.length;
            double py1 = topicBarSize + size.y - size.y * points[i];
            double px2 = (i+1) * size.x / points.length;
            double py2 = topicBarSize + size.y - size.y * points[i+1];
            double px3 = (i+2) * size.x / points.length;
            double py3 = topicBarSize + size.y - size.y * points[i+2];
            double px4 = (i+3) * size.x / points.length;
            double py4 = topicBarSize + size.y - size.y * points[i+3];
            
            //pg.curve((int)px1, (int)py1,(int)px2, (int)py2, (int)px3, (int)py3,(int)px4, (int)py4);

            for (float t=0; t<1.0; t+=0.03) {
                float x1 = pg.curvePoint((int)px1, (int)px2,(int)px3, (int)px4, t);
                float y1 = pg.curvePoint((int)py1, (int)py2,(int)py3, (int)py4, t);
                if (x1 < currentPosX) {
                    pg.ellipse((int)x1, (int)y1, curveWidth, curveWidth);
                } else {
                    break;
                }
            }
        }
        pg.endDraw();

        applet.image(pg, position.x, position.y);
        return true;
    }
}
