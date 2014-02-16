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

import java.util.ArrayList;
import java.util.List;

import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class DevicesFragment extends ListFragment implements
		DevicesListListener, OnItemClickListener {

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

	private DevicesAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		isSubscriptions = getArguments().getBoolean(
				CloudSetMain.ARGUMENT_ISSUBSCRIPTIONS);
		final View rootView = inflater.inflate(R.layout.devices, container,
				false);
		empty = (TextView) rootView.findViewById(android.R.id.empty);

		if (callback != null && callback.hasAccount()) {
			empty.setText(R.string.loading_devices);
			empty.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (callback != null) {
						callback.loadDevices(false);
					}
				}

			});
		} else {
			empty.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (callback != null) {
						callback.doSignIn();
					}
				}

			});
		}

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new DevicesAdapter(getActivity(),
				new ArrayList<SimpleDevice>());
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (callback != null) {
			callback.loadDevices(true);
		}
	}

	@Override
	public void onDevicesLoaded(List<SimpleDevice> devices) {
		if (adapter != null) {
			if (devices != null) {
				adapter.clear();
				adapter.addAll(devices);
				adapter.notifyDataSetChanged();

				if (adapter.isEmpty()) {
					empty.setText(R.string.no_devices);
				} else {
					empty.setText(R.string.loading_devices);
				}
			} else {
				empty.setText(R.string.connection_error);
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (callback != null) {
			if (isSubscriptions) {
				startActivity(new Intent(getActivity(), Actions.class)
						.putExtra(Actions.EXTRA_PUBLISHER,
								callback.getDeviceId(position)).putExtra(
								Actions.EXTRA_SUBSCRIBER,
								callback.getRegistration()));
			} else {
				startActivity(new Intent(getActivity(), Actions.class)
						.putExtra(Actions.EXTRA_PUBLISHER,
								callback.getRegistration()).putExtra(
								Actions.EXTRA_SUBSCRIBER,
								callback.getDeviceId(position)));
			}
		}
	}

	@Override
	public void onDevicesLoadMessage(String message) {
		adapter.clear();
		adapter.notifyDataSetChanged();
		empty.setText(message);
	}

}