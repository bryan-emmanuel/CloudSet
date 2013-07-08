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

public class ActionsAdapter extends ArrayAdapter<String> {
	
	private static final String TAG = "ActionsArrayAdapter";

	private List<SimpleAction> publications;
	private String actionToEnable = null;
	
	public ActionsAdapter(Context context, List<String> objects) {
		super(context, R.layout.action_item, objects);
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
			row = (View) (LayoutInflater.from(parent.getContext())).inflate(R.layout.action_item, null);
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
		if (publications != null) {
			for (SimpleAction publication : publications) {
				if (publication.getName().equals(action)) {
					return true;
				}
			}
		}
		return false;
	}

}
