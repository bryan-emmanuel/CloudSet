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

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;
import com.piusvelte.cloudset.gwt.server.actionEndpoint.ActionEndpoint;
import com.piusvelte.cloudset.gwt.server.actionEndpoint.model.Action;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.PowerManager;

public class ActionsIntentService extends IntentService {

	private static PowerManager.WakeLock sWakeLock;
	private static final Object LOCK = ActionsIntentService.class;

	public ActionsIntentService() {
		super("ActionsIntentService");
	}

	static void serviceIntent(Context context, Intent intent) {
		synchronized(LOCK) {
			if (sWakeLock == null) {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CloudSet");
			}
		}
		sWakeLock.acquire();
		intent.setClassName(context, ActionsIntentService.class.getName());
		context.startService(intent);
	}

	private ActionEndpoint endpoint;

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		if (action != null) {
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				if ((state == BluetoothAdapter.STATE_ON) || (state == BluetoothAdapter.STATE_OFF)) {
					sendAction(action, Integer.toString(state));
				} else {
					sWakeLock.release();
				}
			} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				if ((state == WifiManager.WIFI_STATE_ENABLED) || (state == WifiManager.WIFI_STATE_DISABLED)) {
					sendAction(action, Integer.toString(state));
				} else {
					sWakeLock.release();
				}
			} else if (action.equals(ActionReceiver.VOLUME_CHANGED_ACTION)) {
				int type = intent.getIntExtra(ActionReceiver.EXTRA_VOLUME_STREAM_TYPE, -1);
				int value = intent.getIntExtra(ActionReceiver.EXTRA_VOLUME_STREAM_VALUE, -1);
				if ((type > -1) && (value > -1)) {
					sendAction(action, Integer.toString(type) + ";" + Integer.toString(value));
				} else {
					sWakeLock.release();
				}
			} else {
				sWakeLock.release();
			}
		} else {
			sWakeLock.release();
		}
	}
	
	private void sendAction(String action, String value) {
		String accountName = null;
		String registration = null;
		SharedPreferences sp = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
		accountName = sp.getString(getString(R.string.preference_account_name), null);
		registration = sp.getString(getString(R.string.preference_gcm_registration), null);

		if ((accountName != null) && (registration != null)) {

			GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(getApplicationContext(), "server:client_id:" + getString(R.string.client_id));
			credential.setSelectedAccountName(accountName);

			ActionEndpoint.Builder endpointBuilder = new ActionEndpoint.Builder(
					AndroidHttp.newCompatibleTransport(),
					new JacksonFactory(),
					credential);
			endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();

			(new AsyncTask<String, Void, Void>() {

				@Override
				protected Void doInBackground(String... params) {
					try {
						Action action = new Action();
						action.setName(params[0]);
						action.setValue(params[1]);
						action.setDevice(params[2]);
						endpoint.add(action).execute();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					sWakeLock.release();
				}

			}).execute(action, value, registration);

		} else {
			sWakeLock.release();
		}
	}

}
