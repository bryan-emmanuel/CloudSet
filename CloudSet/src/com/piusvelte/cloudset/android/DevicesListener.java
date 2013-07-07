package com.piusvelte.cloudset.android;

import java.util.List;

import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

public interface DevicesListener {

	public String getRegistration();
	public List<SimpleDevice> getDevices();
	public String getDeviceId(int which);

}
