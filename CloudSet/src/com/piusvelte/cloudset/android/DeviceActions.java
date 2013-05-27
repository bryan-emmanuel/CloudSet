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
import java.util.ArrayList;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.Deviceendpoint;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.Device;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceActions extends ListActivity {

	private static final String TAG = "DeviceActions";
	public static final String EXTRA_DEVICE_REGISTRATION = "com.piusvelte.cloudset.android.EXTRA_DEVICE_REGISTRATION";

	private String registration = null;
	private Deviceendpoint endpoint = null;
	private ArrayAdapter<String> adapter;
	private Device device;
	private ArrayList<String> actions = new ArrayList<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.deviceactions);

		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if ((extras != null) && extras.containsKey(EXTRA_DEVICE_REGISTRATION)) {
				registration = extras.getString(EXTRA_DEVICE_REGISTRATION);
			}
		}

		if (registration != null) {

			SharedPreferences sp = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
			String accountName = sp.getString(getString(R.string.preference_account_name), null);

			GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(this,
					"server:client_id:" + getString(R.string.client_id));
			credential.setSelectedAccountName(accountName);

			Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
					AndroidHttp.newCompatibleTransport(),
					new JacksonFactory(),
					credential);

			endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();

			adapter = new ArrayAdapter<String>(this, R.layout.action_item, actions) {

				@Override
				public View getView(int position, View convertView, ViewGroup parent){
					View row;
					if (convertView == null) {
						row = (View) (LayoutInflater.from(parent.getContext().getApplicationContext())).inflate(R.layout.action_item, null);
					} else {
						row = (View) convertView;
					}

					String action = ActionsIntentService.ACTIONS[position];
					
					Log.d(TAG, "action: " + action);

					TextView tv = (TextView) row.findViewById(R.id.action);
					tv.setText(ActionsIntentService.ACTION_NAMES[position]);

					if ((device != null) && (device.getActions() != null)) {
						
						CheckBox cb = (CheckBox) row.findViewById(R.id.enabled);
						cb.setChecked(device.getActions().contains(action));
						
					}

					return row;
				}

			};
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if ((registration == null) || (endpoint == null)) {
			finish();
		} else {
			setListAdapter(adapter);
			loadActions();
		}
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);

		CheckBox cb = (CheckBox) view.findViewById(R.id.enabled);

		updateDevice(ActionsIntentService.ACTIONS[position], cb.isChecked());

	}

	private void loadActions() {
		(new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground(String... params) {
				try {
					device = endpoint.deviceEndpoint().get(registration).execute();
				} catch (IOException e) {
					Log.e(TAG, e.toString());
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				adapter.clear();
				for (String action : ActionsIntentService.ACTIONS) {
					adapter.add(action);
				}
				adapter.notifyDataSetChanged();
			}

		}).execute();
	}

	private void updateDevice(String action, boolean add) {
		if (device != null) {
			(new AsyncTask<String, Void, Void>() {

				@Override
				protected Void doInBackground(String... params) {
					try {
						if (Boolean.parseBoolean(params[1])) {
							device = endpoint.deviceEndpoint().removeAction(device.getDeviceRegistrationID(), params[0]).execute();
						} else {
							device = endpoint.deviceEndpoint().addAction(device.getDeviceRegistrationID(), params[0]).execute();
						}
					} catch (IOException e) {
						Log.e(TAG, e.toString());
					}
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					adapter.notifyDataSetChanged();
				}

			}).execute(action, Boolean.toString(add));
		}
	}

}
