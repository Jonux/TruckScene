package truckscene;

import java.awt.Color;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;

public class DoubleBar {

	private String bar1;
	private String bar2;
	
	private float bar1process;
	private float bar2process;
	private Color backgroundColor = new Color(105, 105, 105);
	private Color barColor = new Color(50, 50, 255);
	private Color textColor = new Color(255, 255, 255);
	
	private PGraphics pg;
	private PApplet applet;
	
	// variables for bar animation
	private int timeStarted;
	private int timeToTarget;
	private float orginalBar1Pos;
	private float orginalBar2Pos;
	
	public DoubleBar(PApplet applet, String bar1, String bar2, Color barColor, int sizeX, int sizeY, float bar2process, float bar1process){
		this.bar1 = bar1;
		this.bar2 = bar2;
		this.barColor = barColor;
		this.pg = applet.createGraphics(sizeX, sizeY);
		this.applet = applet;
		this.bar1process = bar1process;
		this.bar2process = bar2process;	
		this.timeToTarget = 0;
		this.timeStarted = applet.millis();
	}
	
	public int getWidth() {
		return this.pg.width;
	}
	
	public int getHeight() {
		return this.pg.height;
	}
	
	public void drawBar(PGraphics g, int x, int y, int sx, int sy, float percent) {
		g.fill(255);
		g.rect(x,y, sx, sy);
		g.fill(barColor.getRed(), barColor.getGreen(), barColor.getBlue());
		g.rect(x,y, (int)(sx*percent), sy);
	}
	
	public void draw(int x, int y){
		pg.beginDraw();
		pg.fill(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue());
		
		// background
		pg.fill(155,155,155,255);
		pg.rect(120, (int)(pg.height*0.1), pg.width - 120, (int)(pg.height*0.25));
		pg.rect(120, (int)(pg.height*0.4), pg.width - 120, (int)(pg.height*0.25));
		
		
		// draw bars: Black magic, don't touch to animation logic
		double bar1Size = bar1process;
		double bar2Size = bar2process;
		if (isBarInProgress()) {
			double process = (double)(applet.millis() - timeStarted) / (double)timeToTarget;  // between  0 - 1
			bar1Size =  bar1process + (orginalBar1Pos - bar1process) * (1.0 - process);
			bar2Size =  bar2process + (orginalBar2Pos - bar2process) * (1.0 - process);		// between -1 - 1
			//System.out.println("Process: " + bar1Size + " " + bar2Size + " " + process);
		}
		
		pg.fill(50,50,200,255);
		int wholeSize = pg.width-120;
		int halfSize = (int)(wholeSize/2.0);
		//System.out.println("Process: " + bar1Size + " " + bar2Size + " " + halfSize);
		
		if (bar1Size < 0) {
			pg.fill(120+(int)(100*(-bar1Size)), 0, 0);
		} else {
			pg.fill(0, 120+(int)(100*(bar1Size)), 0);
		}
	
		pg.rect(60 + wholeSize/2, (int)(pg.height*0.13), (int)(halfSize*bar1Size), 24);
		
		if (bar2Size < 0) {
			pg.fill(120+(int)(100*(-bar2Size)), 0, 0);
		} else {
			pg.fill(0, 120+(int)(100*(bar2Size)), 0);
		}
		
		pg.rect(60 + wholeSize/2, (int)(pg.height*0.43), (int)(halfSize*bar2Size), 24);
		
		// middle bar
		pg.fill(0,0,0,255);
		pg.rect(pg.width/2 - 2, (int)(pg.height*0.1), 4, (int)(pg.height*0.56));
		
		// left side texts
		pg.textSize(14);
		pg.fill(255,255,255,255);
		pg.text(bar1, 10, (int)(pg.height*0.25));
		pg.text(bar2, 10, (int)(pg.height*0.55));
/*
		int d = (int)(bar1process * 100.0 + 0.5) - (int)(bar2process * 100.0 + 0.5);
		pg.text(String.format("%c%d%%", (d < 0) ? ' ' : '+', d), pg.width-45, 64);
*/
		pg.endDraw();
		
		applet.image(pg, x, y);
	}
	
	public boolean isBarInProgress() {
		return applet.millis() - timeStarted < timeToTarget;
		//return orginalBar1Pos == bar1process && bar2process == orginalBar2Pos;
	}
	
	public float getBar1Process(){
		return bar1process;
	}
	
	public float getBar2Process(){
		return bar2process;
	}
	
	public void setBar12Progress(float barprocess2, float barprocess1, int timeToTarget) {
		if (isBarInProgress()) return;
		orginalBar1Pos = this.bar1process;
		orginalBar2Pos = this.bar2process;
		this.bar1process = barprocess1; 
		this.bar2process = barprocess2;
		this.timeStarted = applet.millis();
		this.timeToTarget = timeToTarget;
		System.out.println("Process1: " + barprocess1 + " orginal: " + orginalBar1Pos + "  time " + timeToTarget);
		System.out.println("Process2: " + barprocess2 + " orginal: " + orginalBar2Pos + "  time " + timeToTarget);
		
	}
}
