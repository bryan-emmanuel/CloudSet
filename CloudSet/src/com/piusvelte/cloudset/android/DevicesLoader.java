/*
 * CloudSet - Android devices settings synchronization
 * Copyright (C) 2013 Bryan Emmanuel
 *
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.cloudset.android;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.Deviceendpoint;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.Device;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class DevicesLoader extends AsyncTaskLoader<List<SimpleDevice>> {

	public enum Action {
		ADD_DEVICE, GET_SUBSCRIBERS, REMOVE_DEVICE, NONE;
	}

	private static final String TAG = "DevicesLoader";
	private Long deviceId;
	private Deviceendpoint deviceendpoint;
	private Action action;
	private List<SimpleDevice> devices = null;

	public DevicesLoader(Context context, String account, Long deviceId) {
		super(context);
		this.deviceId = deviceId;
		action = Action.GET_SUBSCRIBERS;
		initEndpoint(account);
	}

	public DevicesLoader(Context context, String account) {
		this(context, account, null);
		action = Action.ADD_DEVICE;
	}

	public DevicesLoader(Context context, String account, Long deviceId,
			List<SimpleDevice> devices) {
		this(context, account, deviceId);
		this.devices = devices;
		action = Action.REMOVE_DEVICE;
	}

	private void initEndpoint(String account) {
		Context globalContext = getContext();
		GoogleAccountCredential credential = GoogleAccountCredential
				.usingAudience(
						globalContext.getApplicationContext(),
						"server:client_id:"
								+ globalContext
										.getString(R.string.android_audience));
		credential.setSelectedAccountName(account);
		Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
				AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
				credential).setApplicationName(globalContext
				.getString(R.string.app_name));
		deviceendpoint = CloudEndpointUtils.updateBuilder(endpointBuilder)
				.build();
	}

	@Override
	public List<SimpleDevice> loadInBackground() {
		if (action == Action.ADD_DEVICE) {
			addDevice();
			action = Action.NONE;
			return getSubscribers();
		} else if (action == Action.GET_SUBSCRIBERS && deviceId != null) {
			return getSubscribers();
		} else if (action == Action.REMOVE_DEVICE && deviceId != null) {
			try {
				deviceendpoint.deviceEndpoint().remove(deviceId).execute();

				if (devices != null) {
					for (int i = 0, l = devices.size(); i < l; i++) {
						if (devices.get(i).getId().equals(deviceId)) {
							devices.remove(i);
							break;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			action = Action.NONE;
			return devices;
		}

		return null;
	}

	private Device createDevice() throws UnsupportedEncodingException {
		return new Device().setTimestamp(System.currentTimeMillis()).setModel(
				URLEncoder.encode(android.os.Build.MODEL, "UTF-8"));
	}

	private void addDevice() {
		try {
			Device device = deviceendpoint.deviceEndpoint().add(createDevice())
					.execute();

			if (device != null) {
				deviceId = device.getId();
				getContext()
						.getSharedPreferences(
								getContext().getString(R.string.app_name),
								Context.MODE_PRIVATE).edit()
						.putLong(CloudSetMain.PREFERENCE_DEVICE_ID, deviceId)
						.commit();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<SimpleDevice> getSubscribers() {
		List<SimpleDevice> devices = null;
		try {
			devices = deviceendpoint.deviceEndpoint().subscribers(deviceId)
					.execute().getItems();

			if (devices == null) {
				devices = new ArrayList<SimpleDevice>();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return devices;
	}

	@Override
	public void deliverResult(List<SimpleDevice> devices) {
		this.devices = devices;

		if (isStarted()) {
			super.deliverResult(devices);
		}
	}

	@Override
	protected void onStartLoading() {
		if ((action != Action.NONE && devices == null) || takeContentChanged()) {
			forceLoad();
		} else if (devices != null) {
			deliverResult(devices);
		}
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		devices = null;
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

}
