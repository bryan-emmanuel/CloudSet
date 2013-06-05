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
import java.net.URLEncoder;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.Deviceendpoint;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.Device;

/**
 * This class is started up as a service of the Android application. It listens
 * for Google Cloud Messaging (GCM) messages directed to this device.
 * 
 * When the device is successfully registered for GCM, a message is sent to the
 * App Engine backend via Cloud Endpoints, indicating that it wants to receive
 * broadcast messages from the it.
 * 
 * Before registering for GCM, you have to create a project in Google's Cloud
 * Console (https://code.google.com/apis/console). In this project, you'll have
 * to enable the "Google Cloud Messaging for Android" Service.
 * 
 * Once you have set up a project and enabled GCM, you'll have to set the
 * PROJECT_NUMBER field to the project number mentioned in the "Overview" page.
 * 
 * See the documentation at
 * http://developers.google.com/eclipse/docs/cloud_endpoints for more
 * information.
 */
public class GCMIntentService extends GCMBaseIntentService {

	private Deviceendpoint endpoint;

	protected static final String PROJECT_NUMBER = "205428532443";

	/**
	 * Register the device for GCM.
	 * 
	 * @param mContext
	 *            the activity's context.
	 */
	public static void register(Context mContext) {
		GCMRegistrar.checkDevice(mContext);
		GCMRegistrar.checkManifest(mContext);
		GCMRegistrar.register(mContext, PROJECT_NUMBER);
	}

	/**
	 * Unregister the device from the GCM service.
	 * 
	 * @param mContext
	 *            the activity's context.
	 */
	public static void unregister(Context mContext) {
		GCMRegistrar.unregister(mContext);
	}

	public GCMIntentService() {
		super(PROJECT_NUMBER);
	}

	private Deviceendpoint getEndpoint(Context context) {
		if (endpoint == null) {
			String accountName = null;
			SharedPreferences sp = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
			if (sp.contains(context.getString(R.string.preference_account_name))) {
				accountName = sp.getString(context.getString(R.string.preference_account_name), null);
			}

			GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(context,
					"server:client_id:" + context.getString(R.string.client_id));
			credential.setSelectedAccountName(accountName);

			Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
					AndroidHttp.newCompatibleTransport(),
					new JacksonFactory(),
					credential);
			endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
		}
		return endpoint;
	}

	/**
	 * Called on registration error. This is called in the context of a Service
	 * - no dialog or UI.
	 * 
	 * @param context
	 *            the Context
	 * @param errorId
	 *            an error message
	 */
	@Override
	public void onError(Context context, String errorId) {
		sendGCMIntent(context, CloudSetMain.ACTION_GCM_ERROR, null);
	}

	/**
	 * Called when a cloud message has been received.
	 */
	@Override
	public void onMessage(Context context, Intent intent) {
		// intent contains the actions extras, "action", and "value"
		String action = intent.getStringExtra("action");
		if (action != null) {
			Log.d(TAG, "GCM action: " + action);
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
				int state = Integer.parseInt(intent.getStringExtra(BluetoothAdapter.EXTRA_STATE));
				if (bt != null) {
					if ((BluetoothAdapter.STATE_ON == state) && !bt.isEnabled()) {
						bt.enable();
					} else if ((BluetoothAdapter.STATE_OFF == state) && bt.isEnabled()) {
						bt.disable();
					}
				} else {
					Log.d(TAG, "Bluetooth not supported on this device");
				}
			} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				WifiManager wf = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				if (wf != null) {
					int state = Integer.parseInt(intent.getStringExtra(WifiManager.EXTRA_WIFI_STATE));
					if ((WifiManager.WIFI_STATE_ENABLED == state) && !wf.isWifiEnabled()) {
						wf.setWifiEnabled(true);
					} else if ((WifiManager.WIFI_STATE_DISABLED == state) && wf.isWifiEnabled()) {
						wf.setWifiEnabled(false);
					}
				} else {
					Log.d(TAG, "WiFi not supported on this device");
				}
			} else if (action.equals(ActionsIntentService.VOLUME_CHANGED_ACTION)) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				if (audioManager != null) {
					int streamType = Integer.parseInt(intent.getStringExtra(ActionsIntentService.EXTRA_VOLUME_STREAM_TYPE));
					int streamValue = Integer.parseInt(intent.getStringExtra(ActionsIntentService.EXTRA_VOLUME_STREAM_VALUE));
					audioManager.setStreamVolume(streamType, streamValue, AudioManager.FLAG_PLAY_SOUND);
				} else {
					Log.d(TAG, "Audio not supported on this device");
				}
			} else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				if (audioManager != null) {
					int mode = Integer.parseInt(intent.getStringExtra(AudioManager.EXTRA_RINGER_MODE));
					if (audioManager.getRingerMode() != mode) {
						audioManager.setRingerMode(mode);
					}
				} else {
					Log.d(TAG, "Audio not supported on this device");
				}
			} else {
				Log.d(TAG, "unsupported action: " + action);
			}
		}
	}

	/**
	 * Called back when a registration token has been received from the Google
	 * Cloud Messaging service.
	 * 
	 * @param context
	 *            the Context
	 */
	@Override
	public void onRegistered(Context context, String registration) {

		try {

			/*
			 * Using cloud endpoints, see if the device has already been
			 * registered with the backend
			 */
			Device subscriber = getEndpoint(context).deviceEndpoint().get(registration)
					.execute();

			if ((subscriber != null) && (registration.equals(subscriber.getId()))) {
				sendGCMIntent(context, CloudSetMain.ACTION_GCM_REGISTERED, registration);
				return;
			}
		} catch (IOException e) {
			// Ignore
		}

		try {
			Device subscriber = getEndpoint(context).deviceEndpoint().add(
					(new Device())
					.setId(registration)
					.setTimestamp(System.currentTimeMillis())
					.setModel(URLEncoder.encode(android.os.Build.MODEL, "UTF-8"))).execute();

			if ((subscriber != null) && registration.equals(subscriber.getId())) {
				sendGCMIntent(context, CloudSetMain.ACTION_GCM_REGISTERED, registration);
			}
		} catch (IOException e) {
			Log.e(GCMIntentService.class.getName(),
					"Exception received when attempting to register with server at "
							+ getEndpoint(context).getRootUrl(), e);

			sendGCMIntent(context, CloudSetMain.ACTION_GCM_ERROR, null);
		}
	}

	/**
	 * Called back when the Google Cloud Messaging service has unregistered the
	 * device.
	 * 
	 * @param context
	 *            the Context
	 */
	@Override
	protected void onUnregistered(Context context, String registrationId) {

		if (registrationId != null && registrationId.length() > 0) {
			try {
				getEndpoint(context).deviceEndpoint().remove(registrationId).execute();
			} catch (IOException e) {
				Log.e(GCMIntentService.class.getName(),
						"Exception received when attempting to unregister with server at "
								+ getEndpoint(context).getRootUrl(), e);
				return;
			}
		}

		sendGCMIntent(context, CloudSetMain.ACTION_GCM_UNREGISTERED, null);

	}

	private void sendGCMIntent(Context context, String action, String registration) {
		startActivity(new Intent(context, CloudSetMain.class)
		.setAction(action)
		.putExtra(CloudSetMain.EXTRA_DEVICE_REGISTRATION, registration)
		.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}
}
