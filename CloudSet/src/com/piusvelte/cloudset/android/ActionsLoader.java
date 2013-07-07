package com.piusvelte.cloudset.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.Deviceendpoint;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleAction;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

public class ActionsLoader extends AsyncTaskLoader<List<SimpleAction>> {

	private static final String TAG = "ActionsLoader";
	private Deviceendpoint endpoint;
	private String subscriberId;
	private String publisherId;
	private List<SimpleAction> actions;

	public ActionsLoader(Context context, String subscriberId, String publisherId) {
		super(context);

		this.subscriberId = subscriberId;
		this.publisherId = publisherId;

		Context globalContext = getContext();

		SharedPreferences sp = globalContext.getSharedPreferences(globalContext.getString(R.string.app_name), Context.MODE_PRIVATE);
		String accountName = sp.getString(globalContext.getString(R.string.preference_account_name), null);

		GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(globalContext,
				"server:client_id:" + globalContext.getString(R.string.client_id));
		credential.setSelectedAccountName(accountName);

		Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
				AndroidHttp.newCompatibleTransport(),
				new JacksonFactory(),
				credential)
		.setApplicationName(globalContext.getString(R.string.app_name));

		endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
	}

	private String action = null;
	private boolean add = true;

	public void update(String action, boolean add) {
		this.action = action;
		this.add = add;
		forceLoad();
	}

	@Override
	public List<SimpleAction> loadInBackground() {
		if (action != null) {
			try {
				if (add) {
					for (int i = 0, s = actions.size(); i < s; i++) {
						SimpleAction publication = actions.get(i);
						if (publication.getName().equals(action)) {
							Log.d(TAG, "publicationId: " + publication.getId());
							endpoint.deviceEndpoint().unsubscribe(subscriberId, publication.getId()).execute();
							actions.remove(i);
						}
					}
				} else {
					SimpleAction publication = endpoint.deviceEndpoint().subscribe(subscriberId, publisherId, action).execute();
					actions.add(publication);
				}
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
			return actions;
		} else {
			List<SimpleAction> actions = null;
			try {
				actions = endpoint.deviceEndpoint().subscriptions(subscriberId, publisherId).execute().getItems();
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
			if (actions != null) {
				return actions;
			} else {
				return new ArrayList<SimpleAction>();
			}
		}
	}

	@Override
	public void deliverResult(List<SimpleAction> actions) {
		// clear the update
		action = null;
		this.actions = actions;
		if (isStarted()) {
			super.deliverResult(actions);
		}
	}

	@Override
	protected void onStartLoading() {
		// onStart, check if updating
		if (action != null) {
			// update a device
			forceLoad();
		} else if (actions != null) {
			deliverResult(actions);
		} else if (takeContentChanged() || (actions == null)) {
			forceLoad();
		}
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		actions = null;
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

}
