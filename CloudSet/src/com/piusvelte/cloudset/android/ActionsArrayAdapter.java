package com.piusvelte.cloudset.android;

import java.util.List;

import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleAction;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class ActionsArrayAdapter extends ArrayAdapter<String> {
	
	private static final String TAG = "ActionsArrayAdapter";

	private List<SimpleAction> publications;
	private String actionToEnable = null;
	
	public ActionsArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
		super(context, textViewResourceId, objects);
	}
	
	public void notifyDataSetChanged(List<SimpleAction> publications) {
		this.publications = publications;
		this.notifyDataSetChanged();
	}
	
	public void notifyDataSetChanged(List<SimpleAction> publications, String actionToEnable) {
		this.publications = publications;
		this.actionToEnable = actionToEnable;
		this.notifyDataSetChanged();
	}

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
		
		if (action.equals(actionToEnable)) {
			cb.setEnabled(true);
		}

		return row;
	}

	private boolean isSubscribedTo(String action) {
		Log.d(TAG, "isSubscribedTo, action: " + action);
		if (publications != null) {
			for (SimpleAction publication : publications) {
				Log.d(TAG, "publication: " + publication.getName());
				if (publication.getName().equals(action)) {
					return true;
				}
			}
		}
		return false;
	}

}
