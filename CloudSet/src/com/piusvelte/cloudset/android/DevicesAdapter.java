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
import java.util.List;

import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DevicesAdapter extends ArrayAdapter<SimpleDevice> {

	public DevicesAdapter(Context context, List<SimpleDevice> devices) {
		super(context, R.layout.device_item, devices);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row;
		if (convertView == null) {
			row = (View) (LayoutInflater.from(parent.getContext())).inflate(R.layout.device_item, null);
		} else {
			row = (View) convertView;
		}
		
		SimpleDevice simpleDevice = this.getItem(position);
		
		TextView device = (TextView) row.findViewById(R.id.device);
		try {
			device.setText(URLDecoder.decode(simpleDevice.getModel(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			device.setText(simpleDevice.getModel());
		}
		
		return row;
	}

}
