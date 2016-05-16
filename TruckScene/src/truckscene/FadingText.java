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
class FadingText {

    private String text;
    private int x;
    private int y;
    private int size;

    private int timer;
    private int lifetime;

    private enum Status {
        FADING, VISIBLE, BRIGHTENING, HIDDEN, UNKNOWN
    };
    
    private Status textStatus;
    private double currentAlpha;

    FadingText(String str, int px, int py, int size, int lifetime) {
        this.text = str;
        this.x = px;
        this.y = py;
        this.size = size;
        this.lifetime = lifetime;
        //this.timer = millis();
        this.textStatus = Status.BRIGHTENING;
        this.currentAlpha = 0;
    }

    public void setup(PApplet applet) {
        this.timer = applet.millis();
        //this.pg = super.createGraphics(size.x, size.y);
        //this.noLoop();
        //super.setup();
    }

    public void reset(PApplet applet){
        this.timer = applet.millis();
        this.textStatus = Status.BRIGHTENING;
        this.currentAlpha = 0;
    }
    
    public void draw(PApplet applet) {
        if (applet == null) {
            System.err.println("Applet == NULL, FadingText, draw");
            return;
        }

        applet.textSize(size);
        //applet.textAlign(applet.CENTER);

        switch (textStatus) {
            case BRIGHTENING:
                currentAlpha += 10;
                if (currentAlpha >= 255) {
                    textStatus = Status.VISIBLE;
                    timer = applet.millis();
                    System.out.println("Text Status: VISIBLE " + textStatus);
                }
                break;
            case VISIBLE:
                
                if (timer + lifetime < applet.millis()) {
                    textStatus = Status.FADING;
                    System.out.println("Text Status: FADING " + textStatus);
                }
                break;
            case FADING:
                currentAlpha -= 10;
                if (currentAlpha <= 0) {
                    currentAlpha = 0;
                    textStatus = Status.HIDDEN;
                    System.out.println("Text Status: HIDDEN " + textStatus);
                }
                break;
            case HIDDEN:
                break;
            default:
                break;
        }
        
        if (textStatus != Status.HIDDEN) {
            applet.fill(255, 0, 0, (int) currentAlpha);
            applet.text(this.text, this.x, this.y);
        }
        return;
    }

}
