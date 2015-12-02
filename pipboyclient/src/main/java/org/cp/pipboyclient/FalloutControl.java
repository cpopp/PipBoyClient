package org.cp.pipboyclient;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public abstract class FalloutControl {
	// phone app increments this for this change but so far I haven't noticed
	// that to be necessary
	private final int id = 1;
	
	public static class RadioToggle extends FalloutControl{
		public static enum RadioStation {
			RadioFreedom(28686),
			ClassicalRadio(28691),
			RecruitmentRadioBeacon(28696),
			DiamondCityRadio(28751);
			
			private int stationId;
			private RadioStation(int stationId) {
				this.stationId = stationId;
			}
			
			public int getStationId() {
				return stationId;
			}
		}
		
		private final int type = 12;
		private final List<Integer> args = new ArrayList<>();
		
		public RadioToggle(RadioStation... stations) {
			for(RadioStation station : stations) {
				args.add(station.getStationId());
			}
		}
	}
}
