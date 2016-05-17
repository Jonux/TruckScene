package truckscene;

import processing.core.PApplet;
import processing.serial.Serial;
import truckscene.DashboardApplet.WeatherMode;

import java.util.ArrayList;
import java.util.Arrays;

public class TruckScene extends PApplet {

	private DashboardApplet dashboard;

	private Serial myPort;
	final private int serialPortIdx = 0;
	final private String dataFolderPath = "data/";

	private ArrayList<Scenario> scenarios;
	private UserCommand userCommand;
	private int scenarioIdx;
	
	private int scenarioTimer;
	private int modeChangeDenied = 0;

	// Scenario setup
	// ECO -> SLIP (rainy)
	// SLIP -> ECO (sunny)
	// ECO -> HILL (winter uphil)
	// ECO -> WET (rainy)
	private float[] fuelEffAtStart = { 0.0f, 0.67f, 0.0f, 0.6f, 0.75f, 0.0f };
	private float[] safetyAtStart = { 0.0f, -0.5f, 0.8f, -0.3f, -0.47f, 0.0f };

	private float[] fuelChangesWhenApproved = { 0.0f, 0.33f, 0.7f, 0.4f, 0.41f, 0.0f };
	private float[] safetyChangesWhenApproved = { 0.0f, -0.06f, 0.8f, -0.04f, -0.02f, 0.0f };

	private WeatherMode[] startWeatherModes = { 
			WeatherMode.UNKNOWN, WeatherMode.ECO, WeatherMode.SLIPPERY,
			WeatherMode.ECO, WeatherMode.ECO, WeatherMode.UNKNOWN };

	// How many milliseconds to wait before asking the question from the beginning of the scenario
	private int[] sceneQuestionTimes = {0 , 5000, 5000, 5000, 5000, 0};
	
	
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

	public void setup() {
		// Main display
		super.setup();
		frameRate(30);

		// Setup scenarios
		scenarios = new ArrayList<Scenario>(Arrays.asList(
				new ImageScenario(this, dataFolderPath + "startView.png", -1),
				new VideoScenario(this, dataFolderPath + "badWeather.mp4", 15.0f, 35.0f),
				new VideoScenario(this, dataFolderPath + "normal_road.mp4", 10.0f, 25.0f),
				new VideoScenario(this, dataFolderPath + "slipperyUphill.mp4", 0.0f, 15.0f),
				new VideoScenario(this, dataFolderPath + "WetWeather.mp4", 0.0f, 15.0f),
				new ImageScenario(this, dataFolderPath + "summaryView.png", -1)
		));
		
		// Serial port communication setup
		System.out.println("Available Serial Ports\nCurrent Serial Port Idx: " + serialPortIdx);
		printArray(Serial.list());
		try {
			myPort = new Serial(this, Serial.list()[serialPortIdx], 9600);
		} catch (Exception e) {
			System.out.println("Error opening serial port: Port busy");
		}

		// Setup dashboard display
		dashboard = new DashboardApplet(dataFolderPath);
		PApplet.runSketch(new String[] { dashboard.getClass().getName() }, dashboard);
		
		// Magic delay, wait to other thread to get ready. TODO: do this properly
		delay(300);
		
		// Start scenarios
		restartScenarios();
	}

	private void initNextScenario() {
		scenarios.get(scenarioIdx).stop();
		scenarioIdx = (scenarioIdx + 1) % scenarios.size();
		
		println("Setting up scenario idx: " + scenarioIdx);
		Scenario nextScene = scenarios.get(scenarioIdx);
		if (nextScene instanceof VideoScenario) {
			((VideoScenario) nextScene).setup(safetyAtStart[scenarioIdx], fuelEffAtStart[scenarioIdx]);
		}
		println("Scene Starting: " + scenarioIdx);
		nextScene.start();
		
		try {
			scenarioTimer = millis();
		} catch(Exception e){
			scenarioTimer = 0;
		}
		modeChangeDenied = 0;
		userCommand = UserCommand.UNKNOWN;

		// Beginning of each scenario initialize the small screen's Weather mode
		dashboard.setWeatherMode(startWeatherModes[scenarioIdx]);
	}

	private void restartScenarios() {
		scenarios.get(scenarioIdx).stop();
		scenarioIdx = scenarios.size() - 1;
		println("(re)starting scenarios");
		initNextScenario();
	}
	


	/*
	 * Update top bar sizes based on user inputs
	 */
	public void updateBarSizes() {
		// handle keyboard based events
		int keyEvent = handleUserEvent();

		// read user inputs
		int serialEvent = readSerial();

		// Update bar sizes
		Scenario s = scenarios.get(scenarioIdx);
		if (s instanceof VideoScenario) {
			DoubleBar b1 = ((VideoScenario) s).getSafetyBar();

			if (serialEvent > 0 || keyEvent > 0
					|| (modeChangeDenied == 0 && dashboard.hasModeActivationStarted() && !dashboard.isWeatherModeChanging())) {
				if (!b1.isBarInProgress() && !(b1.getBar2Process() == safetyChangesWhenApproved[scenarioIdx]
						&& b1.getBar1Process() == fuelChangesWhenApproved[scenarioIdx])) {
					safetyOverallChange += abs(safetyChangesWhenApproved[scenarioIdx] - b1.getBar2Process());
					fuelOverallChange += abs(fuelChangesWhenApproved[scenarioIdx] - b1.getBar1Process());
					b1.setBar12Progress(safetyChangesWhenApproved[scenarioIdx], fuelChangesWhenApproved[scenarioIdx], 2000);
				}
			}
		}
	}
	
	public void draw() {
		background(0);
		boolean running = scenarios.get(scenarioIdx).draw();

		// Move forward on scenarios
		if (!running) {
			initNextScenario();
		}

		// Build scenarios
		switch (scenarioIdx) {
		case 0:
			break;
		case 1:
			if (scenarioTimer + sceneQuestionTimes[1] < millis()) {
				dashboard.startModeActivation(WeatherMode.SLIPPERY, 8000);
			}
			break;
		case 2:
			if (scenarioTimer + sceneQuestionTimes[2] < millis()) {
				dashboard.startModeActivation(WeatherMode.ECO, 8000);
			}
			break;
		case 3:
			if (scenarioTimer + sceneQuestionTimes[3] < millis()) {
				dashboard.startModeActivation(WeatherMode.UPHILL, 8000);
			}
			break;
		case 4:
			if (scenarioTimer + sceneQuestionTimes[4] < millis()) {
				dashboard.startModeActivation(WeatherMode.WET, 8000);
			}
			break;
		case 5:
			textSize(46);
			textAlign(CENTER);
			fill(255);
			text(String.format("%c%.1f%%", (safetyOverallChange >= 0) ? '+' : '-', safetyOverallChange), 1170, 640);
			text(String.format("%c%.1f%%", (fuelOverallChange >= 0) ? '+' : '-', fuelOverallChange * 10), 760, 640);
			break;
		default:

		}

		// Handle user inputs and update bar sizes
		updateBarSizes();
	}


	
	/**
	 * USER Interface keyboard (yes , no , start) DEBUG purposes
	 */
	@Override
	public void keyReleased() {
		userCommand = UserCommand.fromKeyboard(key);
		println(scenarioIdx + ") Key pressed: " + key + "      Command: " + userCommand.getValue());
	}

	// Serial format:
	// <6-bits for switch state><tab delimiter><char for gear>
	// where the chars are p,r,n,d,s,- and + for park, reverse, neutral, drive,
	// smart-auto and plus/minus
	// return -1 if denied, 0 nothing happened, 1 approved
	public int readSerial() {
		userCommand = UserCommand.UNKNOWN;
		try {
			while (myPort.available() > 0) {
				String line = myPort.readStringUntil('\n');
				if (line == null)
					continue;
				println("Serial Input: " + line);

				String[] q = splitTokens(line);
				if (q != null && q.length > 1 && q[1] != null && q[1].length() > 0) {
					userCommand = UserCommand.fromSerial(q[1].toLowerCase().charAt(0));
					println("Mode Selected: " + q[1].charAt(0) + "      Command " + userCommand.getValue());
				}
			}
		} catch (Exception e) { }

		// Handle user command
		return handleUserEvent();
	}
	
	/**
	 * Handle user interaction
	 * @return 1 if topBarSet needs to be updated, else 0
	 */
	private int handleUserEvent() {

		switch (userCommand) {
		case UNKNOWN: 
			break;
		case START:
			if (scenarioIdx == 0) {
				initNextScenario();
			}
			break;
		case END: 
			if (scenarioIdx == scenarios.size() - 1) {
				restartScenarios();
			}
			break;
		case RESTART: 
			if (scenarioIdx != 0) {
				restartScenarios();
			}
			break;
		case NEXT: 
			if (scenarioIdx != 0) {
				initNextScenario();
			}
			break;
		case APPROVE: 
			if (dashboard.hasModeActivationStarted() && dashboard.isWeatherModeChanging()) {
				dashboard.completeWeatherModeSelection(true);
				modeChangeDenied = 0;
				return 1;
			}
			break;
		case DENY: 
			if (dashboard.hasModeActivationStarted() && dashboard.isWeatherModeChanging()) {
				dashboard.completeWeatherModeSelection(false);
				modeChangeDenied = 1;
				return -1;
			}
			break;
		}
		return 0;
	}
	
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { truckscene.TruckScene.class.getName() });
	}
}
