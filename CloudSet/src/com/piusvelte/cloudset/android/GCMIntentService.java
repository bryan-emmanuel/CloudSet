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
import java.util.List;

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
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleAction;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

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

	protected static final String PROJECT_NUMBER = "271877614630";

	/**
	 * Register the device for GCM.
	 *
	 * @param mContext
	 *            the activity's context.
	 */
	public static void register(Context context) {
		GCMRegistrar.checkDevice(context);
		GCMRegistrar.checkManifest(context);
		GCMRegistrar.register(context, PROJECT_NUMBER);
	}

	/**
	 * Unregister the device from the GCM service.
	 *
	 * @param mContext
	 *            the activity's context.
	 */
	public static void unregister(Context context) {
		GCMRegistrar.unregister(context);
	}

	public GCMIntentService() {
		super(PROJECT_NUMBER);
	}

	private Deviceendpoint getEndpoint(Context context) {
		if (endpoint == null) {
			String accountName = null;
			SharedPreferences sp = context.getSharedPreferences(
					context.getString(R.string.app_name), MODE_PRIVATE);

			if (sp.contains(context.getString(R.string.preference_account_name))) {
				accountName = sp.getString(
						context.getString(R.string.preference_account_name),
						null);
			}

			GoogleAccountCredential credential = GoogleAccountCredential
					.usingAudience(
							context,
							"server:client_id:"
									+ context
											.getString(R.string.android_audience));
			credential.setSelectedAccountName(accountName);

			Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
					AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
					credential)
					.setApplicationName(getString(R.string.app_name));
			endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder)
					.build();
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
		// NO-OP
	}

	/**
	 * Called when a cloud message has been received.
	 */
	@Override
	public void onMessage(Context context, Intent intent) {
		// intent contains the actions extras, "action", and "value"
		String action = intent.getStringExtra("action");

		if (action != null) {
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
				int state = Integer.parseInt(intent
						.getStringExtra(BluetoothAdapter.EXTRA_STATE));

				if (bt != null) {
					if ((BluetoothAdapter.STATE_ON == state) && !bt.isEnabled()) {
						bt.enable();
					} else if ((BluetoothAdapter.STATE_OFF == state)
							&& bt.isEnabled()) {
						bt.disable();
					}
				} else {
					Log.d(TAG, "Bluetooth not supported on this device");
				}
			} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				WifiManager wf = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				if (wf != null) {
					int state = Integer.parseInt(intent
							.getStringExtra(WifiManager.EXTRA_WIFI_STATE));

					if ((WifiManager.WIFI_STATE_ENABLED == state)
							&& !wf.isWifiEnabled()) {
						wf.setWifiEnabled(true);
					} else if ((WifiManager.WIFI_STATE_DISABLED == state)
							&& wf.isWifiEnabled()) {
						wf.setWifiEnabled(false);
					}
				} else {
					Log.d(TAG, "WiFi not supported on this device");
				}
			} else if (action
					.equals(ActionsIntentService.VOLUME_CHANGED_ACTION)) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

				if (audioManager != null) {
					int streamType = Integer
							.parseInt(intent
									.getStringExtra(ActionsIntentService.EXTRA_VOLUME_STREAM_TYPE));
					int streamValue = Integer
							.parseInt(intent
									.getStringExtra(ActionsIntentService.EXTRA_VOLUME_STREAM_VALUE));
					audioManager.setStreamVolume(streamType, streamValue,
							AudioManager.FLAG_SHOW_UI);
				} else {
					Log.d(TAG, "Audio not supported on this device");
				}
			} else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
				AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

				if (audioManager != null) {
					int mode = Integer.parseInt(intent
							.getStringExtra(AudioManager.EXTRA_RINGER_MODE));
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
	public void onRegistered(Context context, String newRegistration) {
		String oldRegistration = getRegistration();
		if (oldRegistration == null) {
			if (isRegisteredWithServer(context, newRegistration)) {
				storeRegistration(newRegistration);
			} else {
				registerWithServer(context, newRegistration);
			}
		} else if (!oldRegistration.equals(newRegistration)) {
			registerWithServer(context, newRegistration);
			migrateDevice(context, oldRegistration, newRegistration);
		}

	}

	private Device createDevice(String registration)
			throws UnsupportedEncodingException {
		return new Device().setId(registration)
				.setTimestamp(System.currentTimeMillis())
				.setModel(URLEncoder.encode(android.os.Build.MODEL, "UTF-8"));
	}

	private boolean isRegisteredWithServer(Context context, String registration) {
		try {
			Device subscriber = getEndpoint(context).deviceEndpoint()
					.get(registration).execute();

			return subscriber != null
					&& registration.equals(subscriber.getId());
		} catch (IOException e) {
			// Ignore
		}

		return false;
	}

	private void registerWithServer(Context context, String registration) {
		try {
			Device registeredDevice = getEndpoint(context).deviceEndpoint()
					.add(createDevice(registration)).execute();

			if (registeredDevice != null
					&& registration.equals(registeredDevice.getId())) {
				storeRegistration(registration);
			}
		} catch (IOException e) {
			Log.e(GCMIntentService.class.getName(),
					"Exception received when attempting to register with server at "
							+ getEndpoint(context).getRootUrl(), e);
		}
	}

	private void migrateDevice(Context context, String oldRegistration, String newRegistration) {
		try {
			Device oldDevice = getEndpoint(context).deviceEndpoint()
					.get(oldRegistration).execute();

			if (oldDevice != null) {
				List<SimpleDevice> subscribers = getEndpoint(context)
						.deviceEndpoint().subscribers(oldRegistration)
						.execute().getItems();

				if (subscribers != null) {
					for (SimpleDevice subscriber : subscribers) {
						migrateSubscriber(context, oldRegistration,
								newRegistration, subscriber.getId());
						migratePublisher(context, oldRegistration,
								newRegistration, subscriber.getId());
					}
				}

				getEndpoint(context).deviceEndpoint()
						.remove(oldRegistration).execute();
			}
		} catch (IOException e) {
			// Ignore
		}
	}

	private void migrateSubscriber(Context context, String oldSubscriber,
			String newSubscriber, String publisher) {
		try {
			List<SimpleAction> subscriptions = getEndpoint(context)
					.deviceEndpoint().subscriptions(oldSubscriber, publisher)
					.execute().getItems();

			if (subscriptions != null) {
				for (SimpleAction subscription : subscriptions) {
					getEndpoint(context)
							.deviceEndpoint()
							.subscribe(newSubscriber, publisher,
									subscription.getName()).execute();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void migratePublisher(Context context, String oldPublisher,
			String newPublisher, String subscriber) {
		try {
			List<SimpleAction> subscriptions = getEndpoint(context)
					.deviceEndpoint().subscriptions(subscriber, oldPublisher)
					.execute().getItems();

			if (subscriptions != null) {
				for (SimpleAction subscription : subscriptions) {
					getEndpoint(context)
							.deviceEndpoint()
							.subscribe(subscriber, newPublisher,
									subscription.getName()).execute();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
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
				getEndpoint(context).deviceEndpoint().remove(registrationId)
						.execute();
			} catch (IOException e) {
				Log.e(GCMIntentService.class.getName(),
						"Exception received when attempting to unregister with server at "
								+ getEndpoint(context).getRootUrl(), e);
				return;
			}
		}

		endpoint = null;
	}

	private String getRegistration() {
		return getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
				.getString(getString(R.string.preference_gcm_registration),
						null);
	}

	private void storeRegistration(String registration) {
		getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
				.edit()
				.putString(getString(R.string.preference_gcm_registration),
						registration).commit();
	}
}
