/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package truckscene;

import java.awt.Color;
import processing.core.*;
import processing.video.*;

/**
 *
 * @author Jonux
 */
public class VideoScenario implements Scenario {
    
    private PApplet applet;
    private Movie videoClip;
    private String videoClipName;

    private DoubleBar safetyBarSet;
    private PGraphics barSetBackground;
    
    private int startTime = 0;
    private float videoStartPos;
    private float videoEndPos;
    
    public VideoScenario(PApplet applet, String videoClipName, float videoStartPos, float videoEndPos) {
        this.applet = applet;
        this.videoClipName = videoClipName;

        this.safetyBarSet = new DoubleBar(applet, "Fuel Efficiency", "Safety", new Color(0,0,200), (int)(applet.width*0.7), 135, 0.0f, 0.0f);
        this.barSetBackground = applet.createGraphics(applet.width, 145);
        this.startTime = applet.millis();
        
        this.videoEndPos = videoEndPos;
        this.videoStartPos = videoStartPos;
    }
    
    
    public DoubleBar getSafetyBar() {
    	return safetyBarSet;
    }
    
    public void setBarsStartSizes(float safetyProgress, float fuelEffProgress) {
    	safetyBarSet.setBar12Progress(safetyProgress, fuelEffProgress, 0);
    }

    public void start() {
    	if (videoClip == null) {
    		System.err.println(this.videoClip + " NULL");
    		return;
    	}
    	
        videoClip.speed(1);
    	videoClip.play();
    	videoClip.jump(videoStartPos);
        startTime = applet.millis();
    }
    
    public void stop(){
    	videoClip.stop(); // stop
    }
    

    public void setup(float safetyProgress, float fuelEffProgress) {
        videoClip = new Movie(applet, videoClipName);
        videoClip.frameRate(30);

        if (videoClip != null) {
        	videoClip.speed(1);
        	videoClip.loop();
	        System.out.println(this.videoClip + "   " + videoStartPos + " pos: " + videoClip.time() + "   duration: "+ this.videoClip.duration());
        } else {
        	System.err.println("Unable to load video: " + this.videoClip);
        }

        barSetBackground.beginDraw();
        barSetBackground.background(40,40,40);
        barSetBackground.endDraw();
        
        setBarsStartSizes(safetyProgress, fuelEffProgress);
        
    }
    
    public boolean draw() {
        if (videoClip != null && videoClip.available()) {
            videoClip.read();
        }
        
        applet.fill(0,0,0);
        applet.image(videoClip, (applet.width - videoClip.width)/2, barSetBackground.height); //, applet.width, applet.height - barSetBackground.height);
        applet.image(barSetBackground, 0, 0);

        this.safetyBarSet.draw(applet.width/2 - safetyBarSet.getWidth()/2, 5);

        if (videoClip.time() >= videoEndPos) {
        	return false;
        }

        return true;
    }
    
}
