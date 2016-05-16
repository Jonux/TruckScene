package truckscene;

import java.awt.Frame;

import processing.core.PApplet;
import processing.serial.Serial;
import processing.core.*;
import processing.video.*;
import truckscene.DashboardApplet.WeatherMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import com.sun.xml.internal.messaging.saaj.util.transform.EfficientStreamingTransformer;


public class TruckScene extends PApplet {

	private DashboardApplet dashboard;
	
	private Serial myPort;
	final private int serialPortIdx = 0;
	final private String dataFolderPath = "data/"; 
	
    private ArrayList<Scenario> scenarios;
    private int scenarioIdx;
    private char activeMode = 'a';

    private int scenarioTimer;
    private int sceneStepCount = 0;
    private int modeChangeDenied = 0;
    
    // Scenario setup
    // ECO -> SLIP (rainy)
    // SLIP -> ECO (sunny)
    // ECO -> HILL (winter uphil)
    // ECO -> WET  (rainy)
    private float[] fuelEffAtStart = {0.0f, 0.67f, 0.0f,  0.6f, 0.75f, 0.0f};
    private float[] safetyAtStart = {0.0f,  -0.5f, 0.8f, -0.3f, -0.47f, 0.0f};

    private float[] fuelChangesWhenApproved = {0.0f, 0.33f, 0.7f, 0.4f, 0.41f, 0.0f};
    private float[] safetyChangesWhenApproved = {0.0f, -0.06f, 0.8f, -0.04f, -0.02f, 0.0f};
    
    private WeatherMode[] startWeatherModes = { WeatherMode.UKNOWN, WeatherMode.ECO, WeatherMode.SLIPPERY, WeatherMode.ECO, WeatherMode.ECO, WeatherMode.UKNOWN};
    
    // Summary variables
    private double safetyOverallChange = 0;
    private double fuelOverallChange = 0;
    
    public TruckScene() {
        super();
        scenarioIdx = 0;
    }
    
	public void settings() {
		size(1920, 1024);
		fullScreen(2);
	}

	private void initScenarios(){
        int i = 0;
        for (Scenario s : scenarios) {
        	s.setup();
        	//s.stop();
        	if (s instanceof VideoScenario) {
        		((VideoScenario)s).setBarsStartSizes(safetyAtStart[i], fuelEffAtStart[i]);
        	}
        	i++;
        }
        //scenarios.get(0).setup();
	}
	
	public void setup() {
		// Main display
		super.setup();
		frameRate(30);
	
		// Setup scenarios
        scenarios = new ArrayList<Scenario>(Arrays.asList(
        	new ImageScenario(this, dataFolderPath + "startView.png", -1),
            new VideoScenario(this, dataFolderPath + "badWeather.mp4", 15.0f, 35.0f),
            new VideoScenario(this, dataFolderPath + "normal_road.mp4", 10.0f, 25.0f),
            new VideoScenario(this, dataFolderPath + "slipperyUphill.mp4",0.0f, 15.0f),
            new VideoScenario(this, dataFolderPath + "WetWeather.mp4",0.0f, 15.0f),
            new ImageScenario(this, dataFolderPath + "summaryView.png", -1)
        ));
        
        // Load all videos
        initScenarios();

		// Serial port communication setup
		System.out.println("Available Serial Ports\nCurrent Serial Port Idx: " + serialPortIdx);
		printArray(Serial.list());
		try {
			myPort = new Serial(this, Serial.list()[serialPortIdx], 9600);
		} catch (Exception e){
			System.out.println("Error opening serial port: Port busy");
		}
		
		// Setup dashboard display
		dashboard = new DashboardApplet(dataFolderPath);
		PApplet.runSketch(new String[]{dashboard.getClass().getName()}, dashboard);
		
		dashboard.setWeatherMode(startWeatherModes[0]);
		// dashboard.startModeActivation(WeatherMode.SLIPPERY, 8000);
	}

	// Serial format:
	// <6-bits for switch state><tab delimiter><char for gear>
	// where the chars are p,r,n,d,s,- and + for park, reverse, neutral, drive, smart-auto and plus/minus
	// return -1 if denied, 0 nothing happened, 1 approved
	public int readSerial() {
		activeMode = 'a';
		try {
			while (myPort.available() > 0) {
				// int inByte = myPort.read();
				String line = myPort.readStringUntil('\n');
				if (line == null) continue;
				println("Serial Input: " + line);
				
				String[] q = splitTokens(line);
				if (q != null && q.length > 1 && q[1] != null && q[1].length() > 0) {
					// println("Mode Selected: " + q[1].charAt(0) + " whole line: " + line);
					activeMode = q[1].toLowerCase().charAt(0);
				}
			}
		} catch (Exception e) {}
		

		return handleUserEvent();
	}
	
	
	private void initNextScenario() {
		scenarios.get(scenarioIdx).stop();
    	scenarioIdx = (scenarioIdx + 1) % scenarios.size();
		scenarios.get(scenarioIdx).start();
		scenarioTimer = millis();
		sceneStepCount = 0;
		modeChangeDenied = 0;
		activeMode = 'a';
		
		// Beginning of each scenario initialize the small screen's Weather mode
		dashboard.setWeatherMode(startWeatherModes[scenarioIdx]);
		
        Scenario s = scenarios.get(scenarioIdx);
    	if (s instanceof VideoScenario) {
    		DoubleBar b1 = ((VideoScenario)s).getSafetyBar();
    		//TwinBar b2 = ((VideoScenario)s).getFuelEfficiencyBar();
    		
    		b1.setBar12Progress(safetyAtStart[scenarioIdx], fuelEffAtStart[scenarioIdx], 0);
    		//b2.setBar1Progress(0.5f + fuelEffAtStart[scenarioIdx]);
    	}
	}
	
	private int handleUserEvent() {
		// nothing happened
		if (activeMode == 'a') return 0;
		
		// Start
		if (scenarioIdx == 0) {
			if (activeMode == 's') {
				initNextScenario();
			}
			return 0;
		// end
		} else if (scenarioIdx == scenarios.size() - 1) {
			if (activeMode == 'd') {
				println("Restarting Scenario");
				initNextScenario();
			}
			return 0;
			
		// restart
		} else if (activeMode == 'r' && scenarioIdx != 0) {
			scenarios.get(scenarioIdx).stop();
			scenarioIdx = scenarios.size() - 1;
			println("Restarting Scenario");
			initNextScenario();
		}

		// Automatic changing
		// Yes (+)
		if (activeMode == '+' && sceneStepCount > 0 && dashboard.isWeatherModeChanging()) {
			dashboard.completeWeatherModeSelection(true);
			modeChangeDenied = 0;
			return 1;
			
		// No (-)
		} else if (activeMode == '-' && sceneStepCount > 0 && dashboard.isWeatherModeChanging()) {
			dashboard.completeWeatherModeSelection(false);
			modeChangeDenied = 1;
			return -1;
		}
		return 0;
	}
	

	public void draw() {
		background(0);
        boolean running = scenarios.get(scenarioIdx).draw();
        
        // Move forward on scenarios
        if (!running) {
        	initNextScenario();
        }
        
        // Build interactions to scenarios
        switch(scenarioIdx)
        {
	        case 0: 
	        	break;
	        case 1:
	        	if (scenarioTimer + 5000 < millis() && sceneStepCount == 0) {
	        		dashboard.startModeActivation(WeatherMode.SLIPPERY, 8000);
	        		sceneStepCount = 1;
	        	}
	        	break;
	        case 2:
	        	if (scenarioTimer + 5000 < millis() && sceneStepCount == 0) {
	        		dashboard.startModeActivation(WeatherMode.ECO, 8000);
	        		sceneStepCount = 1;
	        	}
	        	break;
	        case 3:
	        	if (scenarioTimer + 5000 < millis() && sceneStepCount == 0) {
	        		dashboard.startModeActivation(WeatherMode.UPHILL, 8000);
	        		sceneStepCount = 1;
	        	}
	        	break;
	        case 4:
	        	if (scenarioTimer + 8000 < millis() && sceneStepCount == 0) {
	        		dashboard.startModeActivation(WeatherMode.WET, 8000);
	        		sceneStepCount = 1;
	        	}
	        	break;
	        case 5:
	        	
	        	textSize(46);
	        	textAlign(CENTER);
	        	fill(255);
	        	text(String.format("%c%.1f%%", (safetyOverallChange>0)?'+':'-', safetyOverallChange), 1170, 640);
	        	text(String.format("%c%.1f%%", (fuelOverallChange>0)?'+':'-', fuelOverallChange*10), 760, 640);
	        	break;
	        default:
        	
        }
        
        int event2 = handleUserEvent();
        
        // read user inputs
        int event1 = readSerial();

        // Update bar sizes
        Scenario s = scenarios.get(scenarioIdx);
    	if (s instanceof VideoScenario) {
    		DoubleBar b1 = ((VideoScenario)s).getSafetyBar();
    		//TwinBar b2 = ((VideoScenario)s).getFuelEfficiencyBar();
    	
	        if (event1 > 0 || event2 > 0 || (modeChangeDenied == 0 && sceneStepCount > 0 && !dashboard.isWeatherModeChanging())) {
	        	if (!b1.isBarInProgress() && !(b1.getBar2Process() == safetyChangesWhenApproved[scenarioIdx] &&  b1.getBar1Process() == fuelChangesWhenApproved[scenarioIdx]) ){
	        		safetyOverallChange += abs(safetyChangesWhenApproved[scenarioIdx] - b1.getBar2Process());
	        		fuelOverallChange += abs(fuelChangesWhenApproved[scenarioIdx] - b1.getBar1Process());
	        		b1.setBar12Progress(safetyChangesWhenApproved[scenarioIdx], fuelChangesWhenApproved[scenarioIdx], 2000);
	        	}
	    		
	    	//	b2.setBar1Progress(0.5f + fuelChangesWhenApproved[scenarioIdx]);
	        }
    	}
	}
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { truckscene.TruckScene.class.getName() });
	}
	
	
	/**
	 * USER Interface
	 * keyboard (yes , no , start)
	 * DEBUG purposes
	 */
	@Override
	public void keyReleased() {
		println("key pressed " + scenarioIdx + " w: " + dashboard.isWeatherModeChanging() + " key: " + key);
		
		activeMode = 'a';
		
		// Start
		if (scenarioIdx == 0) {
			if (key == 's') {
				activeMode = 's';
			}
			return;
		} else if (scenarioIdx == scenarios.size() - 1) {
			if (key == 'd') {
				activeMode = 'd';
			}
			return;
		} else if (key == 'r' && scenarioIdx != 0) {
			activeMode = 'r';
			return;
		}
		/*
		// Manual changing, TODO later
		if (!dashboard.isWeatherModeChanging()) {
			if (key == 'e')  dashboard.setWeatherMode(WeatherMode.ECO);
			if (key == 'r')  dashboard.setWeatherMode(WeatherMode.SLIPPERY);
			if (key == 't')  dashboard.setWeatherMode(WeatherMode.UPHILL);
			if (key == 'y')  dashboard.setWeatherMode(WeatherMode.WET);
			if (key == 'u')  dashboard.setWeatherMode(WeatherMode.UKNOWN);
			return;
		}
		*/
		
		// Automatic changing
		// Yes (+)
		if (key == 'y') {
			activeMode = '+';
		// No (-)
		} else if (key == 'n') {
			activeMode = '-';
		}
	}
	
}




