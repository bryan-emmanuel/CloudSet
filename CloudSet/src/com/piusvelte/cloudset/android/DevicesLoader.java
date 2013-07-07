package com.piusvelte.cloudset.android;

import java.io.IOException;
import java.util.ArrayList;
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

	public DevicesLoader(Context context, String account, String registrationId) {
		super(context);
		this.registrationId = registrationId;
		Context globalContext = getContext();
		GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(globalContext.getApplicationContext(), "server:client_id:" + globalContext.getString(R.string.client_id));
		credential.setSelectedAccountName(account);
		Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
				AndroidHttp.newCompatibleTransport(),
				new JacksonFactory(),
				credential)
		.setApplicationName(globalContext.getString(R.string.app_name));
		deviceendpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
	}
	
	private List<SimpleDevice> devices;

	@Override
	public List<SimpleDevice> loadInBackground() {
		List<SimpleDevice> devices = null;
		try {
			devices = deviceendpoint.deviceEndpoint().subscribers(registrationId).execute().getItems();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (devices == null) {
			// set to new empty List for the adapter
			devices = new ArrayList<SimpleDevice>();
		}
		return devices;
	}

    @Override
    public void deliverResult(List<SimpleDevice> devices) {
    	this.devices = devices;
    	if (isStarted()) {
    		super.deliverResult(devices);
    	}
    }

	@Override
	protected void onStartLoading() {
		if (devices != null) {
			deliverResult(devices);
		}
        if (takeContentChanged() || (devices == null)) {
            forceLoad();
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
