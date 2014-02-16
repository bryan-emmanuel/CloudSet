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
	private List<SimpleAction> publications = null;
	private String action = null;
	private String actionToEnable = null;
	private boolean remove = false;

	public ActionsLoader(Context context, String subscriberId, String publisherId) {
		super(context);
		this.subscriberId = subscriberId;
		this.publisherId = publisherId;
		Context globalContext = getContext();
		SharedPreferences sp = globalContext.getSharedPreferences(globalContext.getString(R.string.app_name), Context.MODE_PRIVATE);
		String accountName = sp.getString(globalContext.getString(R.string.preference_account_name), null);
		GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(globalContext,
				"server:client_id:" + globalContext.getString(R.string.android_audience));
		credential.setSelectedAccountName(accountName);
		Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
				AndroidHttp.newCompatibleTransport(),
				new JacksonFactory(),
				credential)
		.setApplicationName(globalContext.getString(R.string.app_name));
		endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
	}

	public ActionsLoader(Context context, String subscriberId, String publisherId, List<SimpleAction> publications, String action, boolean remove) {
		this(context, subscriberId, publisherId);
		this.publications = publications;
		this.action = action;
		this.remove = remove;
	}

	public String getActionToEnable() {
		return actionToEnable;
	}

	@Override
	public List<SimpleAction> loadInBackground() {
		if (action != null) {
			try {
				if (remove) {
					for (int i = 0, s = publications.size(); i < s; i++) {
						SimpleAction publication = publications.get(i);
						if (publication.getName().equals(action)) {
							endpoint.deviceEndpoint().unsubscribe(subscriberId, publication.getId()).execute();
							publications.remove(i);
							break;
						}
					}
				} else {
					SimpleAction publication = endpoint.deviceEndpoint().subscribe(subscriberId, publisherId, action).execute();
					publications.add(publication);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return publications;
		} else {
			try {
				List<SimpleAction> publications = endpoint.deviceEndpoint().subscriptions(subscriberId, publisherId).execute().getItems();
				if (publications != null) {
					return publications;
				} else {
					return new ArrayList<SimpleAction>();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	@Override
	public void deliverResult(List<SimpleAction> publications) {
		// store the action for the checkbox to enable, then clear it here
		actionToEnable = action;
		action = null;
		this.publications = publications;
		if (isStarted()) {
			super.deliverResult(publications);
		}
	}

	@Override
	protected void onStartLoading() {
		// onStart, check if updating
		if ((action != null) || takeContentChanged() || (publications == null)) {
			// update a device
			forceLoad();
		} else if (publications != null) {
			deliverResult(publications);
		}
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		publications = null;
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

}
