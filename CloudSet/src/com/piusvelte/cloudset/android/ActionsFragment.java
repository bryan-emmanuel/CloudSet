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

import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleAction;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

public class ActionsFragment extends ListFragment implements
		LoaderManager.LoaderCallbacks<List<SimpleAction>> {

	private static final String TAG = "ActionsFragment";
	// load the actions for the subscription to this device
	private ActionsAdapter adapter;
	// subscriptions, filtered on the publisherId
	private List<SimpleAction> publications;
	private ArrayList<String> actions = new ArrayList<String>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.actions_fragment, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	ActionsListener callback;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			callback = (ActionsListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement ActionsListener");
		}
	}

	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		// block rapid checking/unchecking
		CheckBox actionCheckBox = (CheckBox) view.findViewById(R.id.enabled);
		if (actionCheckBox.isEnabled()) {
			actionCheckBox.setEnabled(false);
			updateDevice(ActionsIntentService.ACTIONS[position],
					actionCheckBox.isChecked());
		} else {
			Toast.makeText(getActivity().getApplicationContext(),
					getString(R.string.updating_sync), Toast.LENGTH_SHORT)
					.show();
		}
	}

	public interface ActionsListener {

		public String getSubscriberId();

		public String getPublisherId();

	}

	private static final String EXTRA_ACTION = "action";
	private static final String EXTRA_REMOVE = "remove";

	private void updateDevice(String action, boolean add) {
		Bundle extras = new Bundle();
		extras.putString(EXTRA_ACTION, action);
		extras.putBoolean(EXTRA_REMOVE, add);
		getLoaderManager().initLoader(getNextLoaderId(), extras, this);
	}

	private int getNextLoaderId() {
		int id = 1;

		while (loaderIds.contains(id)) {
			id++;
		}

		loaderIds.add(id);

		return id;
	}

	// the first loader is for all actions, the rest are for updating devices
	private ArrayList<Integer> loaderIds;
	private static final String EXTRA_LOADER_IDS = "loader_ids";

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putIntegerArrayList(EXTRA_LOADER_IDS, loaderIds);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new ActionsAdapter(getActivity(), actions);
		setListAdapter(adapter);
		LoaderManager loaderManager = getLoaderManager();
		// attach the first loader for populating the publications
		loaderManager.initLoader(0, null, this);

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(EXTRA_LOADER_IDS)) {
			loaderIds = savedInstanceState
					.getIntegerArrayList(EXTRA_LOADER_IDS);
		} else {
			loaderIds = new ArrayList<Integer>();
		}

		// attach additional tasks for updating devices
		for (int i = 0, s = loaderIds.size(); i < s; i++) {
			loaderManager.initLoader(loaderIds.get(i), null, this);
		}
	}

	@Override
	public Loader<List<SimpleAction>> onCreateLoader(int arg0, Bundle args) {
		if (arg0 > 0) {
			if ((args != null) && args.containsKey(EXTRA_ACTION)
					&& args.containsKey(EXTRA_REMOVE)) {
				return new ActionsLoader(getActivity(),
						callback.getSubscriberId(), callback.getPublisherId(),
						publications, args.getString(EXTRA_ACTION),
						args.getBoolean(EXTRA_REMOVE));
			} else {
				return null;
			}
		} else {
			return new ActionsLoader(getActivity(), callback.getSubscriberId(),
					callback.getPublisherId());
		}
	}

	@Override
	public void onLoadFinished(Loader<List<SimpleAction>> loader,
			List<SimpleAction> publications) {
		if (loader.getId() > 0) {
			getLoaderManager().destroyLoader(loader.getId());
			loaderIds.remove((Integer) loader.getId());
		}

		this.publications = publications;

		// reload the adapter for the first loader
		if (loader.getId() == 0) {
			if (publications != null) {
				adapter.clear();

				for (String action : ActionsIntentService.ACTIONS) {
					adapter.add(action);
				}

				adapter.notifyDataSetChanged(publications);
			} else {
				Toast.makeText(getActivity(),
						getString(R.string.action_load_error),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			adapter.notifyDataSetChanged(publications,
					((ActionsLoader) loader).getActionToEnable());
		}
	}

	@Override
	public void onLoaderReset(Loader<List<SimpleAction>> arg0) {
		// NO-OP
	}

}
