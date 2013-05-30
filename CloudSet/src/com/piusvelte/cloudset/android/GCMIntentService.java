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
import com.piusvelte.cloudset.gwt.server.subscriberendpoint.Subscriberendpoint;
import com.piusvelte.cloudset.gwt.server.subscriberendpoint.model.Subscriber;

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

	private Subscriberendpoint endpoint;

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

	private Subscriberendpoint getEndpoint(Context context) {
		if (endpoint == null) {
			String accountName = null;
			SharedPreferences sp = context.getSharedPreferences(context.getString(R.string.app_name), MODE_PRIVATE);
			if (sp.contains(context.getString(R.string.preference_account_name))) {
				accountName = sp.getString(context.getString(R.string.preference_account_name), null);
			}

			GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(context,
					"server:client_id:" + context.getString(R.string.client_id));
			credential.setSelectedAccountName(accountName);

			Subscriberendpoint.Builder endpointBuilder = new Subscriberendpoint.Builder(
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
		sendGCMIntent(context, CloudSetMain.ACTION_GCM_ERROR);
	}

	/**
	 * Called when a cloud message has been received.
	 */
	@Override
	public void onMessage(Context context, Intent intent) {
		// intent contains the actions extras, "name", and "value"
		String action = intent.getStringExtra("name");
		if (action != null) {
			String value = intent.getStringExtra("value");
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
				if (bt != null) {
					if (Integer.toString(BluetoothAdapter.STATE_ON).equals(value) && !bt.isEnabled()) {
						bt.enable();
					} else if (Integer.toString(BluetoothAdapter.STATE_OFF).equals(value) && bt.isEnabled()) {
						bt.disable();
					}
				} else {
					Log.d(TAG, "Bluetooth not supported on this device");
				}
			} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				WifiManager wf = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				if (wf != null) {
					if (Integer.toString(WifiManager.WIFI_STATE_ENABLED).equals(value) && !wf.isWifiEnabled()) {
						wf.setWifiEnabled(true);
					} else if (Integer.toString(WifiManager.WIFI_STATE_DISABLED).equals(value) && wf.isWifiEnabled()) {
						wf.setWifiEnabled(false);
					}
				} else {
					Log.d(TAG, "WiFi not supported on this device");
				}
			} else if (action.equals(ActionsIntentService.VOLUME_CHANGED_ACTION)) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				if (audioManager != null) {
					int idx = value.indexOf(";");
					int streamType = Integer.parseInt(value.substring(0, idx++));
					int streamValue = Integer.parseInt(value.substring(idx));
					audioManager.setStreamVolume(streamType, streamValue, AudioManager.FLAG_PLAY_SOUND);
				} else {
					Log.d(TAG, "Audio not supported on this device");
				}
			} else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
				if (audioManager != null) {
					int mode = Integer.parseInt(value);
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
		/*
		 * This is some special exception-handling code that we're using to work around a problem
		 * with the DevAppServer and methods that return null in App Engine 1.7.5.
		 */
		boolean alreadyRegisteredWithEndpointServer = false;

		try {

			/*
			 * Using cloud endpoints, see if the device has already been
			 * registered with the backend
			 */
			Subscriber subscriber = getEndpoint(context).subscriberEndpoint().get(registration)
					.execute();

			if (subscriber != null && registration.equals(subscriber.getId())) {
				alreadyRegisteredWithEndpointServer = true;
				sendGCMIntent(context, CloudSetMain.ACTION_GCM_REGISTERED);
			}
		} catch (IOException e) {
			// Ignore
		}

		try {
			if (!alreadyRegisteredWithEndpointServer) {
				/*
				 * We are not registered as yet. Send an endpoint message
				 * containing the GCM registration id and some of the device's
				 * product information over to the backend. Then, we'll be
				 * registered.
				 */
				Subscriber subscriber = getEndpoint(context).subscriberEndpoint().add(
						(new Subscriber())
						.setId(registration)
						.setTimestamp(System.currentTimeMillis())
						.setModel(URLEncoder.encode(android.os.Build.MODEL, "UTF-8"))).execute();
				if ((subscriber != null) && registration.equals(subscriber.getId())) {
					// registered and stored in the backend, store locally
					context.getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
					.edit()
					.putString(getString(R.string.preference_gcm_registration), registration)
					.commit();

					sendGCMIntent(context, CloudSetMain.ACTION_GCM_REGISTERED);
				}
			}
		} catch (IOException e) {
			Log.e(GCMIntentService.class.getName(),
					"Exception received when attempting to register with server at "
							+ getEndpoint(context).getRootUrl(), e);

			sendGCMIntent(context, CloudSetMain.ACTION_GCM_ERROR);
			return;
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
				getEndpoint(context).subscriberEndpoint().remove(registrationId).execute();
				context.getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
				.edit()
				.putString(getString(R.string.preference_account_name), null)
				.putString(getString(R.string.preference_gcm_registration), null)
				.commit();
			} catch (IOException e) {
				Log.e(GCMIntentService.class.getName(),
						"Exception received when attempting to unregister with server at "
								+ getEndpoint(context).getRootUrl(), e);
				return;
			}
		}

		sendGCMIntent(context, CloudSetMain.ACTION_GCM_UNREGISTERED);

	}

	private void sendGCMIntent(Context context, String action) {
		startActivity(new Intent(context, CloudSetMain.class)
		.setAction(action)
		.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}
}
