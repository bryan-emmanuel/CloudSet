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
import java.util.List;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.Deviceendpoint;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class DevicesLoader extends AsyncTaskLoader<List<SimpleDevice>> {

	private static final String TAG = "DevicesLoader";
	private String registrationId;
	private Deviceendpoint deviceendpoint;
	private String deregisterId;

	public DevicesLoader(Context context, String account, String registrationId) {
		super(context);
		this.registrationId = registrationId;
		Context globalContext = getContext();
		GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(globalContext.getApplicationContext(), "server:client_id:" + globalContext.getString(R.string.android_audience));
		credential.setSelectedAccountName(account);
		Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
				AndroidHttp.newCompatibleTransport(),
				new JacksonFactory(),
				credential)
		.setApplicationName(globalContext.getString(R.string.app_name));
		deviceendpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
	}

	public DevicesLoader(Context context, String account, String registrationId, List<SimpleDevice> devices, String deregisterId) {
		this(context, account, registrationId);
		this.devices = devices;
		this.deregisterId = deregisterId;
	}


	private List<SimpleDevice> devices = null;

	@Override
	public List<SimpleDevice> loadInBackground() {
		if (deregisterId != null) {
			try {
				deviceendpoint.deviceEndpoint().remove(deregisterId).execute();
				for (int i = 0, l = devices.size(); i < l; i++) {
					if (devices.get(i).getId().equals(deregisterId)) {
						devices.remove(i);
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return devices;
		} else if (registrationId != null) {
			try {
				return deviceendpoint.deviceEndpoint().subscribers(registrationId).execute().getItems();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		} else {
			return null;
		}
	}

	@Override
	public void deliverResult(List<SimpleDevice> devices) {
		deregisterId = null;
		this.devices = devices;
		if (isStarted()) {
			super.deliverResult(devices);
		}
	}

	@Override
	protected void onStartLoading() {
		if ((deregisterId != null) || takeContentChanged() || (devices == null)) {
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
