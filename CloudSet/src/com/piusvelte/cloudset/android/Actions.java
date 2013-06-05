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
import com.piusvelte.cloudset.gwt.server.deviceendpoint.Deviceendpoint;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.Action;

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

public class Actions extends ListActivity {

	// load the actions for the subscription to this device

	private static final String TAG = "Actions";
	public static final String EXTRA_PUBLISHER = "com.piusvelte.cloudset.android.EXTRA_PUBLISHER";
	public static final String EXTRA_SUBSCRIBER = "com.piusvelte.cloudset.android.EXTRA_SUBSCRIBER";

	private String publisherId;
	private String subscriberId;
	private Deviceendpoint endpoint = null;
	private ArrayAdapter<String> adapter;
	// subscriptions, filtered on the publisherId
	private List<Action> publications;
	private ArrayList<String> actions = new ArrayList<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.actions);

		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if ((extras != null) && extras.containsKey(EXTRA_PUBLISHER) && extras.containsKey(EXTRA_SUBSCRIBER)) {
				publisherId = extras.getString(EXTRA_PUBLISHER);
				subscriberId = extras.getString(EXTRA_SUBSCRIBER);
			}
		}

		if (publisherId != null) {

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
				public View getView(int position, View convertView, ViewGroup parent) {
					View row;
					if (convertView == null) {
						row = (View) (LayoutInflater.from(parent.getContext().getApplicationContext())).inflate(R.layout.action_item, null);
					} else {
						row = (View) convertView;
					}

					String action = ActionsIntentService.ACTIONS[position];

					TextView tv = (TextView) row.findViewById(R.id.action);
					tv.setText(ActionsIntentService.ACTION_NAMES[position]);

					CheckBox cb = (CheckBox) row.findViewById(R.id.enabled);
					cb.setChecked(isSubscribedTo(action));

					return row;
				}

			};
		}
	}

	private boolean isSubscribedTo(String action) {
		if (publications != null) {
			for (Action publication : publications) {
				if (publication.getName().equals(action)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if ((publisherId == null) || (endpoint == null)) {
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
					publications = endpoint.deviceEndpoint().subscriptions(subscriberId, publisherId).execute().getItems();
					if (publications == null) {
						publications = new ArrayList<Action>();
					}
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
		(new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground(String... params) {
				try {
					if (Boolean.parseBoolean(params[1])) {
						Log.d(TAG, "unsubscribe: " + params[0]);
						for (int i = 0, s = publications.size(); i < s; i++) {
							Action publication = publications.get(i);
							if (publication.getName().equals(params[0])) {
								Log.d(TAG, "publicationId: " + publication.getId());
								endpoint.deviceEndpoint().unsubscribe(subscriberId, publication.getId()).execute();
								publications.remove(i);
							}
						}
					} else {
						Log.d(TAG, "subscribe: " + params[0]);
						Action publication = endpoint.deviceEndpoint().subscribe(subscriberId, publisherId, params[0]).execute();
						publications.add(publication);
					}
				} catch (IOException e) {
					Log.e(TAG, e.toString());
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Log.d(TAG, "reload subscriptions");
				adapter.notifyDataSetChanged();
			}

		}).execute(action, Boolean.toString(add));
	}

}
