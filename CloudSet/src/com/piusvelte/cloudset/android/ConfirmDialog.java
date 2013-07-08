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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ConfirmDialog extends DialogFragment {
	
	private String deviceId;
	
	public ConfirmDialog setDeviceId(String id) {
		this.deviceId = id;
		return this;
	}
	
	@Override
	public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof DevicesListener)) {
            throw new ClassCastException(activity.toString() + " must implement ConfirmDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	if (savedInstanceState != null) {
    		if (savedInstanceState.containsKey(EXTRA_DEVICE_ID)) {
    			deviceId = savedInstanceState.getString(EXTRA_DEVICE_ID);
    		}
    	}
        return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.title_deregister)
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				DevicesListener listener;
				try {
					listener = (DevicesListener) getActivity();
					listener.deregisterDevice(deviceId);
				} catch (ClassCastException e) {
					throw new ClassCastException(getActivity().toString()
							+ " must implement DevicesListener");
				}
			}
		})
		.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// do nothing
			}
		})
        .create();
    }
    
    private static final String EXTRA_DEVICE_ID = "device_id";

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putString(EXTRA_DEVICE_ID, deviceId);
	}

}
