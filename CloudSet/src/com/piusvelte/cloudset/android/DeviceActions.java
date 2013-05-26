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
import java.util.List;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;
import com.piusvelte.cloudset.gwt.server.deviceEndpoint.DeviceEndpoint;
import com.piusvelte.cloudset.gwt.server.deviceEndpoint.model.Device;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceActions extends ListActivity {

	public static final String EXTRA_DEVICE_REGISTRATION = "com.piusvelte.cloudset.android.EXTRA_DEVICE_REGISTRATION";

	private String registration = null;
	private DeviceEndpoint endpoint = null;
	private ArrayList<String> actions = new ArrayList<String>();
	private ArrayAdapter<String> adapter;
	private Device device;

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

			GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(getApplicationContext(), "server:client_id:" + getString(R.string.client_id));
			credential.setSelectedAccountName(accountName);
			
			DeviceEndpoint.Builder endpointBuilder = new DeviceEndpoint.Builder(AndroidHttp.newCompatibleTransport(), new JacksonFactory(), credential);
			
			endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();

			adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.action_item, actions) {

				@Override
				public View getView(int position, View convertView, ViewGroup parent){
					View row;
					if (convertView == null) {
						row = (View) (LayoutInflater.from(parent.getContext().getApplicationContext())).inflate(R.layout.action_item, null);
					} else {
						row = (View) convertView;
					}
					
					String action = actions.get(position);
					
					TextView tv = (TextView) row.findViewById(R.id.action);
					tv.setText(ActionsIntentService.ACTION_NAMES[position]);
					
					CheckBox cb = (CheckBox) row.findViewById(R.id.enabled);
					cb.setEnabled(device.getActions().contains(action));
					
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
		
		//TODO, enable/disable actions
		String action = this.actions.get(position);
		List<String> deviceActions = device.getActions();
		
		CheckBox cb = (CheckBox) view.findViewById(R.id.enabled);
		if (cb.isChecked()) {
			deviceActions.add(action);
		} else {
			deviceActions.remove(action);
		}
		
		device.setActions(deviceActions);
		
		updateDevice();
		
	}

	private void loadActions() {
		(new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground(String... params) {
				try {
					device = endpoint.get(registration).execute();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	
	private void updateDevice() {
		(new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground(String... params) {
				try {
					endpoint.update(device).execute();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				adapter.notifyDataSetChanged();
			}

		}).execute();
	}

}
