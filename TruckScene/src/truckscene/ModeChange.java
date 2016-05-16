package truckscene;

import truckscene.DashboardApplet.WeatherMode;

public class ModeChange {
	public int timeToChange; // from the beginning of the video (ms)
	public WeatherMode weatherMode;
	public int changingTime; // user reaction time (ms)
	
	public double fuelEfficiencyChangeAfterApproval;
	public double safetyChangeAfterApproval;
	
	public ModeChange(WeatherMode weatherMode, int timeToChange, int changingTime, double fuelEfficiencyChangeAfterApproval, double safetyChangeAfterApproval ){
		this.timeToChange = timeToChange;
		this.weatherMode = weatherMode;
		this.changingTime = changingTime;
		this.fuelEfficiencyChangeAfterApproval = fuelEfficiencyChangeAfterApproval;
		this.safetyChangeAfterApproval = safetyChangeAfterApproval;
	}
}
