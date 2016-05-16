package truckscene;

import java.awt.Color;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;

public class TwinBar {

	private String bar1;
	private String bar2;
	private String bar1Percent;
	private String topic;
	
	private float bar1process;
	private float bar2process;
	private Color backgroundColor = new Color(105, 105, 105);
	private Color barColor = new Color(50, 50, 255);
	private Color textColor = new Color(255, 255, 255);
	
	private PGraphics pg;
	private PApplet applet;
	
	public TwinBar(PApplet applet, String bar1, String bar2, String bar1Percent, String topic, Color barColor, int sizeX, int sizeY, float bar2process, float bar1process) {
		this.bar1 = bar1;
		this.bar2 = bar2;
		this.bar1Percent = bar1Percent;
		this.barColor = barColor;
		this.topic = topic;
		this.pg = applet.createGraphics(sizeX, sizeY);
		this.applet = applet;
		this.bar1process = bar1process;
		this.bar2process = bar2process;
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
		pg.rect(0,0, pg.width, pg.height);
		drawBar(pg, 50, 50, pg.width - 2*50, 20, bar1process);
		drawBar(pg, 50, 80, pg.width - 2*50, 20, bar2process);
		
		pg.textSize(24);
		pg.fill(255,255,255,255);
		pg.textAlign(PConstants.CENTER);
		pg.text(topic, pg.width/2, 30);
		
		pg.textSize(14);
		pg.textAlign(PConstants.LEFT);
		pg.text(bar1, 12, 64);
		int d = (int)(bar1process * 100.0 + 0.5) - (int)(bar2process * 100.0 + 0.5);
		pg.text(String.format("%c%d%%", (d < 0) ? ' ' : '+', d), pg.width-45, 64);
		pg.text(bar2, 12, 94);
		
		pg.endDraw();
		
		applet.image(pg, x, y);
	}
	
	public void setBar12Progress(float barprocess) {
		this.bar1process = barprocess; 
		this.bar2process = barprocess; 
	}
	
	public void setBar1Progress(float bar1process) {
		this.bar1process = bar1process; 
	}
	
	public void setBar2Progress(float bar2process) {
		this.bar2process = bar2process; 
	}
}
