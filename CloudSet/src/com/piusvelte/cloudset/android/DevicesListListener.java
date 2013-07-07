package com.piusvelte.cloudset.android;

import java.util.List;

import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

public interface DevicesListListener {
	
	public void onDevicesLoaded(List<SimpleDevice> devices);

}
