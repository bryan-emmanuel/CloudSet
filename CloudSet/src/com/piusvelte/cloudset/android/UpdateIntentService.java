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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class UpdateIntentService extends IntentService {

	private static final String TAG = "UpdateIntentService";
	private static final String CURRENT_SDK = "current_sdk";
	private static final int NO_SDK = 0;

	public UpdateIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
			SharedPreferences sp = getSharedPreferences(
					getString(R.string.app_name), Context.MODE_PRIVATE);

			if (hasSystemUpdated(action, sp) || hasPackageUpdated(action, sp)) {
				GCMIntentService.register(getApplicationContext());
			}
		}
	}

	private boolean hasPackageUpdated(String action, SharedPreferences sp) {
		return Intent.ACTION_PACKAGE_REPLACED.equals(action)
				&& sp.getString(getString(R.string.preference_account_name),
						null) != null;
	}

	private boolean hasSystemUpdated(String action, SharedPreferences sp) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)
				&& Build.VERSION.SDK_INT != sp.getInt(CURRENT_SDK, NO_SDK)) {
			sp.edit().putInt(CURRENT_SDK, Build.VERSION.SDK_INT).commit();
			return true;
		}

		return false;
	}
}
