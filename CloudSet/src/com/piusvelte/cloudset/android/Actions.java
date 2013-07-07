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

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

public class Actions extends FragmentActivity implements ActionsFragment.ActionsListener {

	private static final String TAG = "Actions";
	public static final String EXTRA_PUBLISHER = "com.piusvelte.cloudset.android.EXTRA_PUBLISHER";
	public static final String EXTRA_SUBSCRIBER = "com.piusvelte.cloudset.android.EXTRA_SUBSCRIBER";

	private String publisherId;
	private String subscriberId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.actions);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if ((extras != null) && extras.containsKey(EXTRA_PUBLISHER) && extras.containsKey(EXTRA_SUBSCRIBER)) {
				publisherId = extras.getString(EXTRA_PUBLISHER);
				subscriberId = extras.getString(EXTRA_SUBSCRIBER);
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (publisherId == null) {
			finish();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public String getSubscriberId() {
		return subscriberId;
	}

	@Override
	public String getPublisherId() {
		return publisherId;
	}

}
