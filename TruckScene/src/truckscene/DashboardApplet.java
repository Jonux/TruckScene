package truckscene;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PShape;

class DashboardApplet extends PApplet {
	
	final private int screenSizeX = 640;
	final private int screenSizeY = 480;
	
	private ArrayList<PShape> images;
	private ArrayList<PShape> inactiveImages;
	
	private String dataFolderPath; // = "data/";
	private String[] weatherFiles = {"eco.svg", "slippery.svg", "uphill.svg", "wet.svg", "eco.svg"};
	private String[] weatherFilesI = {"eco_.svg", "slippery_.svg", "uphill_.svg", "wet_.svg", "eco_.svg"};
	private String[] activatingMsg = {"ACTIVATING\nECO MODE", "ACTIVATING\nSLIPPERY MODE", "ACTIVATING\nUPHILL MODE", "ACTIVATING\nWET MODE", "DISABLING\nWEATHER MODE"};
	
	private int modeActivationTimer;
	private int timeToNextMode;
	private WeatherMode nextWeatherMode;
	
	private boolean modeActivationStarted;
	private PFont textFont;
	
	private WeatherMode weatherMode;
	public enum WeatherMode {
		ECO(0), SLIPPERY(1), UPHILL(2), WET(3), UNKNOWN(4);
		
		private int value;
	    private WeatherMode(int value){
	        this.value = value;
	    }
	}
	
	public DashboardApplet(String dataFolderPath){
		super();
		images = new ArrayList<PShape>();
		inactiveImages = new ArrayList<PShape>();
		this.dataFolderPath = dataFolderPath;
		this.weatherMode = WeatherMode.UNKNOWN;
		this.nextWeatherMode = WeatherMode.UNKNOWN;
		this.modeActivationStarted = false;
	}
	
	public void settings() {
		size(screenSizeX, screenSizeY);
	}
	
	public void setup() {
		frameRate(60);
		surface.setResizable(true);
		
		// Load weather icons
		for (String s : weatherFiles){
			images.add(loadShape(dataFolderPath + "" + s));
		}
		// Load inactive weather icons
		for (String s : weatherFilesI){
			inactiveImages.add(loadShape(dataFolderPath + "" + s));
		}
		
		this.textFont = createFont("Arial Bold", 36);
		textFont(textFont);
		modeActivationStarted = false;
	}

	private void drawBar(int x, int y, int sx, int sy, double percent) {
		fill(255);
		rect(x,y, sx, sy);
		fill(50,50,200);
		rect(x,y, (int)(sx*percent), sy);
		fill(255);
	}
	
	public void draw() {
		background(0);

		// this.height = (int)(9.0 / 16.0 * (float)this.width);
		// println(this.height);
		
        // Mode is changing
        if (isWeatherModeChanging()) {
	        if (modeActivationTimer + timeToNextMode > millis()) {
	            textSize((int)(this.width/20.0));
	            textAlign(CENTER);
	            fill(255,255,255);
	            
	        	text(activatingMsg[nextWeatherMode.value], (int)(this.width*0.25), (int)(this.height*0.3));
	    		shape(images.get(nextWeatherMode.value),  (int)(this.width*0.50),  (int)(this.height*0.1), (int)(this.width*0.5), (int)(this.height*0.5));
	    		
	    		double progress = ((double)(millis() - modeActivationTimer)) / timeToNextMode;
	    		// println(modeActivationTimer + " " + timeToNextMode + " " + millis() + " " + progress);
	    		drawBar((int)(this.width*0.1), (int)(this.height*0.59), (int)(this.width*0.8), (int)(this.height*0.05), progress);
	        } else {
	        	weatherMode = nextWeatherMode;
	        }
        }
        
        fill(50,50,50);
        rect(0, (int)(this.height*0.7), (int)(this.width), (int)(this.height*0.02));
        for (int i=0; i < weatherFiles.length; i++){
        	if (weatherMode.value == i && weatherMode != WeatherMode.UNKNOWN) {
        		shape(images.get(i),  (int)(this.width*0.25)*i,  (int)(this.height*0.75), (int)(this.width*0.25), (int)(this.height*0.25));
        	} else {
        		shape(inactiveImages.get(i),  (int)(this.width*0.25)*i,  (int)(this.height*0.75), (int)(this.width*0.25), (int)(this.height*0.25));
        	}
        }

		redraw();
	}
	
	/*
	 * Starts mode activation
	 */
	public void startModeActivation(WeatherMode nextMode, int timeToNextMode) {
		startModeActivation(nextMode, timeToNextMode, false);
	}
	
	public void startModeActivation(WeatherMode nextMode, int timeToNextMode, boolean override){
		if (!modeActivationStarted || override) { 
			this.modeActivationTimer = millis();
			this.timeToNextMode = timeToNextMode;
			this.nextWeatherMode = nextMode;
			this.modeActivationStarted = true;
		}
	}
	
	public boolean hasModeActivationStarted() {
		return modeActivationStarted;
	}
	
	public boolean isWeatherModeChanging() {
		return weatherMode != nextWeatherMode;
	}
	
	public void completeWeatherModeSelection(boolean result) {
		if (result) {
			weatherMode = nextWeatherMode;
		} else {
			nextWeatherMode = weatherMode;
		}
	}
	
	public void setWeatherMode(WeatherMode w){
		weatherMode = w;
		nextWeatherMode = w;
		this.modeActivationStarted = false;
	}
	
	public WeatherMode getWeatherMode(){
		return weatherMode;
	}
	
	/*
	 * Returns timer in milliseconds from mode activation, else -1 
	 */
	public int getModeActivationTimer(){
		if (!modeActivationStarted) return -1;
		return millis() - modeActivationTimer;
	}
}