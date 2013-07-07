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
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ActionsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<SimpleAction>> {

	private static final String TAG = "ActionsFragment";
	// load the actions for the subscription to this device
	private ArrayAdapter<String> adapter;
	// subscriptions, filtered on the publisherId
	private List<SimpleAction> publications;
	private ArrayList<String> actions = new ArrayList<String>();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.actions_fragment, container, false);
		return rootView;
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
			updateDevice(ActionsIntentService.ACTIONS[position], actionCheckBox.isChecked());
		} else {
			Toast.makeText(getActivity().getApplicationContext(), getString(R.string.updating_sync), Toast.LENGTH_SHORT).show();
		}
	}

	public interface ActionsListener {

		public String getSubscriberId();
		public String getPublisherId();

	}

	private boolean isSubscribedTo(String action) {
		if (publications != null) {
			for (SimpleAction publication : publications) {
				if (publication.getName().equals(action)) {
					return true;
				}
			}
		}
		return false;
	}

	private void updateDevice(String action, boolean add) {
		ActionsLoader loader = (ActionsLoader) getLoaderManager().initLoader(++loadersCount, null, this);
		loader.update(action, add);
	}
	
	// the first loader is for all actions, the rest are for updating devices
	private int loadersCount = 1;
	private static final String LOADERS = "loaders";

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt(LOADERS, loadersCount);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new ArrayAdapter<String>(getActivity(), R.layout.action_item, actions) {

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
		setListAdapter(adapter);
		LoaderManager loaderManager = getLoaderManager();
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(LOADERS)) {
				loadersCount = savedInstanceState.getInt(LOADERS);
			}
		}
		for (int i = 0; i < loadersCount; i++) {
			loaderManager.initLoader(loadersCount, null, this);
		}
	}

	@Override
	public Loader<List<SimpleAction>> onCreateLoader(int arg0, Bundle args) {
		return new ActionsLoader(getActivity(), callback.getSubscriberId(), callback.getPublisherId());
	}

	@Override
	public void onLoadFinished(Loader<List<SimpleAction>> loader,
			List<SimpleAction> publications) {
		if (loadersCount > 1) {
			loadersCount--;
		}
		this.publications = publications;
		// reload the adapter for the first loader
		if (loader.getId() == 0) {
			adapter.clear();
			for (String action : ActionsIntentService.ACTIONS) {
				adapter.add(action);
			}
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<List<SimpleAction>> arg0) {
		// TODO Auto-generated method stub
	}

}
