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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DevicesFragment extends ListFragment implements DevicesListListener {

	private static final String TAG = "DevicesFragment";

	TextView empty;

	boolean isSubscriptions;

	public DevicesFragment() {
	}

	DevicesListener callback;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			callback = (DevicesListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement DevicesListener");
		}
	}

	private ArrayAdapter<String> adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		isSubscriptions = getArguments().getBoolean(CloudSetMain.ARGUMENT_ISSUBSCRIPTIONS);
		final View rootView = inflater.inflate(isSubscriptions ? R.layout.devices : R.layout.devices, container, false);
		empty = (TextView) rootView.findViewById(android.R.id.empty);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
		setListAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (callback != null) {
			onDevicesLoaded(callback.getDevices());
		}
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		if (callback != null) {
			String publisher;
			String subscriber;
			if (isSubscriptions) {
				publisher = callback.getDeviceId(position);
				subscriber = callback.getRegistration();
			} else {
				publisher = callback.getRegistration();
				subscriber = callback.getDeviceId(position);
			}
			startActivity(new Intent(getActivity().getApplicationContext(), Actions.class)
			.putExtra(Actions.EXTRA_PUBLISHER, publisher)
			.putExtra(Actions.EXTRA_SUBSCRIBER, subscriber));
		}
	}

	@Override
	public void onDevicesLoaded(List<SimpleDevice> devices) {
		if (adapter != null) {
			if (devices != null) {
				adapter.clear();
				for (SimpleDevice device : devices) {
					try {
						adapter.add(URLDecoder.decode(device.getModel(), "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (adapter.isEmpty()) {
					empty.setText(R.string.no_devices);
				} else {
					empty.setText(R.string.loading_devices);
				}
				adapter.notifyDataSetChanged();
			}
		}
	}

}