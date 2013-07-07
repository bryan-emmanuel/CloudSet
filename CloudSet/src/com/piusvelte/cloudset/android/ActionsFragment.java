package com.piusvelte.cloudset.android;

import java.util.ArrayList;
import java.util.List;

import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleAction;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

public class ActionsFragment extends ListFragment implements LoaderManager.LoaderCallbacks<List<SimpleAction>> {

	private static final String TAG = "ActionsFragment";
	// load the actions for the subscription to this device
	private ActionsArrayAdapter adapter;
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
			updateDevice(ActionsIntentService.ACTIONS[position], actionCheckBox.isChecked());
		} else {
			Toast.makeText(getActivity().getApplicationContext(), getString(R.string.updating_sync), Toast.LENGTH_SHORT).show();
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
		getLoaderManager().initLoader(++loadersCount, extras, this);
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
		adapter = new ActionsArrayAdapter(getActivity(), R.layout.action_item, actions);
		setListAdapter(adapter);
		LoaderManager loaderManager = getLoaderManager();
		// attach the first loader for populating the publications
		loaderManager.initLoader(0, null, this);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(LOADERS)) {
				loadersCount = savedInstanceState.getInt(LOADERS);
			}
		}
		// attach additional tasks for updating devices
		for (int i = 1; i < loadersCount; i++) {
			loaderManager.initLoader(loadersCount, null, this);
		}
	}

	@Override
	public Loader<List<SimpleAction>> onCreateLoader(int arg0, Bundle args) {
		if ((loadersCount > 0) && (args != null)) {
			return new ActionsLoader(getActivity(), callback.getSubscriberId(), callback.getPublisherId(), publications, args.getString(EXTRA_ACTION), args.getBoolean(EXTRA_REMOVE));
		} else {
			return new ActionsLoader(getActivity(), callback.getSubscriberId(), callback.getPublisherId());
		}
	}

	@Override
	public void onLoadFinished(Loader<List<SimpleAction>> loader,
			List<SimpleAction> publications) {
		if (loadersCount > 1) {
			getLoaderManager().destroyLoader(--loadersCount);
		}
		Log.d(TAG, "onLoadFinished : " + loadersCount + ", " + loader.getId());
		this.publications = publications;
		// reload the adapter for the first loader
		if (loader.getId() == 0) {
			adapter.clear();
			for (String action : ActionsIntentService.ACTIONS) {
				adapter.add(action);
			}
			adapter.notifyDataSetChanged(this.publications);
		} else {
			Log.d(TAG, "onLoadFinished, done updating, has publications? " + (this.publications != null));
			adapter.notifyDataSetChanged(this.publications, ((ActionsLoader) loader).getActionToEnable());
		}
	}

	@Override
	public void onLoaderReset(Loader<List<SimpleAction>> arg0) {
		// TODO Auto-generated method stub
	}

}
