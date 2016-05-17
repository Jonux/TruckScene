package truckscene;

import processing.core.PApplet;
import processing.serial.Serial;
import truckscene.DashboardApplet.WeatherMode;
import truckscene.SceneData.SceneType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	
	/*
	 * Scenario setup
	 * 
	 * ECO -> SLIP (rainy)
	 * SLIP -> ECO (sunny)
	 * ECO -> HILL (winter uphill)
	 * ECO -> WET (rainy)
	 */
	private final SceneData[] scenes = {
			new SceneData(WeatherMode.UNKNOWN, dataFolderPath + "startView.png"),
			new SceneData(0.67f, -0.5f, 0.33f, -0.05f, WeatherMode.ECO, WeatherMode.SLIPPERY, 15.0f, 35.0f, 5000, 8000, 3000, dataFolderPath + "badWeather.mp4"),
			new SceneData(0.0f, 0.8f, 0.7f, 0.8f, WeatherMode.SLIPPERY, WeatherMode.ECO, 10.0f, 25.0f, 5000, 8000, 3000, dataFolderPath + "normal_road.mp4"),
			new SceneData(0.6f, -0.3f, 0.4f, -0.08f, WeatherMode.ECO, WeatherMode.UPHILL, 0.0f, 15.0f, 5000, 8000, 3000, dataFolderPath + "slipperyUphill.mp4"),
			new SceneData(0.75f, -0.47f, 0.41f, -0.02f, WeatherMode.ECO, WeatherMode.WET, 0.0f, 15.0f, 5000, 8000, 3000, dataFolderPath + "WetWeather.mp4"),
			new SceneData(WeatherMode.UNKNOWN, dataFolderPath + "summaryView.png")
	};
	
	// Summary variables
	private double safetyOverallChange = 0;
	private double fuelOverallChange = 0;
	private ArrayList<QuestionStatus> sceneAnswers;
	public enum QuestionStatus {
		UNKNOWN, APPROVED, DENIED
	}
	
	
	public TruckScene() {
		super();
		scenarioIdx = 0;
		scenarios = new ArrayList<Scenario>();
		sceneAnswers = new ArrayList<QuestionStatus>();
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
		initializeScenarios();
		
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

	public void initializeScenarios() {
		scenarios = new ArrayList<Scenario>();
		sceneAnswers = new ArrayList<QuestionStatus>();
		for (SceneData s : scenes) {
			if (s.sceneType == SceneType.IMAGE) {
				scenarios.add(new ImageScenario(this, s.fileName, -1));
			} else if (s.sceneType == SceneType.VIDEO) {
				scenarios.add(new VideoScenario(this, s.fileName, s.videoStartTime, s.videoEndTime));
			}
			
			// Summary answers to the questions
			sceneAnswers.add(QuestionStatus.UNKNOWN);
		}
	}
	
	private void initNextScenario() {
		scenarios.get(scenarioIdx).stop();
		scenarioIdx = (scenarioIdx + 1) % scenarios.size();
		
		println("Setting up scenario idx: " + scenarioIdx);
		Scenario nextScene = scenarios.get(scenarioIdx);
		if (nextScene instanceof VideoScenario) {
			((VideoScenario) nextScene).setup(scenes[scenarioIdx].safetyAtStart, scenes[scenarioIdx].fuelEfficiencyAtStart);
		}
		println("Scene Starting: " + scenarioIdx);
		nextScene.start();
		
		scenarioTimer = millis();
		modeChangeDenied = 0;
		userCommand = UserCommand.UNKNOWN;
		println("Scene timer set: " + scenarioTimer);
		
		// Beginning of each scenario initialize the small screen's Weather mode
		dashboard.setWeatherMode(scenes[scenarioIdx].startWeatherMode);
		println("Dashboard weather " + dashboard.frameRate);
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
			TwinBar b1 = ((VideoScenario) s).getSafetyBar();

			if (serialEvent > 0 || keyEvent > 0
					|| (modeChangeDenied == 0 && dashboard.hasModeActivationStarted() && !dashboard.isWeatherModeChanging())) {
				if (!b1.isBarInProgress() && !(b1.getBar2Process() == scenes[scenarioIdx].safetyWhenApproved
						&& b1.getBar1Process() == scenes[scenarioIdx].fuelEfficiencyWhenApproved)) {
					safetyOverallChange += abs(scenes[scenarioIdx].safetyWhenApproved - b1.getBar2Process());
					fuelOverallChange += abs(scenes[scenarioIdx].fuelEfficiencyWhenApproved - b1.getBar1Process());
					b1.setBar12Progress(scenes[scenarioIdx].safetyWhenApproved, scenes[scenarioIdx].fuelEfficiencyWhenApproved, 1500);
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
		if (scenes[scenarioIdx].sceneType == SceneType.VIDEO) {
			
			// shorter waiting time, when mode is manually selected
			if (dashboard.getModeActivationTimer() > scenes[scenarioIdx].questionAfterTime && 
				sceneAnswers.get(scenarioIdx) != QuestionStatus.UNKNOWN) {
				initNextScenario();
				
			// start mode activation
			} else if (scenarioTimer + scenes[scenarioIdx].questionAfterTime < millis()) {
				dashboard.startModeActivation(scenes[scenarioIdx].nextWeatherMode, scenes[scenarioIdx].questionReactTime);
			}
			
		}
		
		// the last scene
		if (scenarioIdx == scenes.length - 1) {
			// draw new background
			scenarios.get(scenarioIdx).draw();
			
			// Set text on top of bubbles
			textSize(46);
			textAlign(CENTER);
			fill(255);
			text(String.format("%c%.1f%%", (safetyOverallChange >= 0) ? '+' : '-', safetyOverallChange), 1170, 640);
			text(String.format("%c%.1f%%", (fuelOverallChange >= 0) ? '+' : '-', fuelOverallChange * 10), 760, 640);
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
		println(scenarioIdx + ") Key pressed: " + key + "      Command: " + userCommand.name());
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
					println("Mode Selected: " + q[1].charAt(0) + "      Command " + userCommand.name());
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
				sceneAnswers.set(scenarioIdx, QuestionStatus.APPROVED);
				return 1;
			}
			break;
		case DENY: 
			if (dashboard.hasModeActivationStarted() && dashboard.isWeatherModeChanging()) {
				dashboard.completeWeatherModeSelection(false);
				modeChangeDenied = 1;
				sceneAnswers.set(scenarioIdx, QuestionStatus.DENIED);
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
