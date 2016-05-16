/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package truckscene;

import processing.core.PApplet;

/**
 *
 * @author Laatikko
 */
public class CountDownClock {
    
     private int secoundsToCount;
     private int startTime;
     private PApplet applet;
     private Point position;
     
     public CountDownClock(PApplet applet, int secoundsToCount, Point position){
        this.secoundsToCount = secoundsToCount;
        this.applet = applet;
        this.startTime = applet.millis();
        this.position = position;
        
     }
     
     public boolean draw() {
         if (applet.millis() > startTime + secoundsToCount * 1000) {
             return false;
         }
         
         int timeLeft = ((startTime + secoundsToCount * 1000 - applet.millis())) / 1000;
         applet.textSize(92);
         applet.text(timeLeft, position.x, position.y);
         return true;
         
     }
}
