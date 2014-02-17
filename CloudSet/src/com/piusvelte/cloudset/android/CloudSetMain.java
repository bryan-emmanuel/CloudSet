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
import java.util.Locale;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.SimpleDevice;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

public class CloudSetMain extends FragmentActivity implements
		ActionBar.TabListener, AccountsFragment.AccountsListener,
		DevicesListener, LoaderManager.LoaderCallbacks<List<SimpleDevice>> {

	private static final String TAG = "CloudSetMain";

	public static final String ACTION_GCM_REGISTERED = "com.piusvelte.cloudset.android.action.GCM_REGISTERED";
	public static final String ACTION_GCM_UNREGISTERED = "com.piusvelte.cloudset.android.action.GCM_UNREGISTERED";
	public static final String ACTION_GCM_ERROR = "com.piusvelte.cloudset.android.action.GCM_ERROR";

	public static final String EXTRA_DEVICE_REGISTRATION = "com.piusvelte.cloudset.android.extra.DEVICE_REGISTRATION";

	public static final String ARGUMENT_ISSUBSCRIPTIONS = "issubscriptions";

	private static final int FRAGMENT_ACCOUNT = 0;
	private static final int FRAGMENT_SUBSCRIPTIONS = 1;
	private static final int FRAGMENT_SUBSCRIBERS = 2;

	protected static final String PREFERENCE_ACCOUNT_NAME = "account_name";
	protected static final String PREFERENCE_DEVICE_ID = "device_id";
	protected static final int INVALID_DEVICE_ID = -1;

	private SectionsPagerAdapter sectionsPagerAdapter;
	private ViewPager viewPager;
	private String account;
	private Long deviceId;
	private List<SimpleDevice> devices;
	// loader 0 if for registration, degregistration, and loading devices
	// loaders 1+ are for created for deregistering additional devices
	private ArrayList<Integer> loaderIds;
	private static final String EXTRA_LOADER_IDS = "loader_ids";
	private static final String EXTRA_DEREGISTER_ID = "deregister_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GooglePlayServicesUtil
				.getOpenSourceSoftwareLicenseInfo(getApplicationContext());
		setContentView(R.layout.activity_main);
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		sectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());
		viewPager = (ViewPager) findViewById(R.id.pager);
		viewPager.setAdapter(sectionsPagerAdapter);
		viewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab()
					.setText(sectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}

		SharedPreferences sp = getSharedPreferences(
				getString(R.string.app_name), MODE_PRIVATE);
		account = sp.getString(PREFERENCE_ACCOUNT_NAME, null);
		deviceId = sp.getLong(PREFERENCE_DEVICE_ID, INVALID_DEVICE_ID);
		setCurrentTab();

		// create the loader for registration, deregistration, and loading
		// devices
		LoaderManager loaderManager = getSupportLoaderManager();
		loaderManager.initLoader(0, null, this);

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(EXTRA_LOADER_IDS)) {
			loaderIds = savedInstanceState
					.getIntegerArrayList(EXTRA_LOADER_IDS);
		} else {
			loaderIds = new ArrayList<Integer>();
		}

		for (int i = 0, s = loaderIds.size(); i < s; i++) {
			// reconnect to additional loaders for deregistering additional
			// devices
			loaderManager.initLoader(loaderIds.get(i), null, this);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putIntegerArrayList(EXTRA_LOADER_IDS, loaderIds);
	}

	private int getNextLoaderId() {
		int id = 1;

		while (loaderIds.contains(id)) {
			id++;
		}

		loaderIds.add(id);

		return id;
	}

	private void setCurrentTab() {
		if (hasAccount()) {
			viewPager.setCurrentItem(FRAGMENT_SUBSCRIPTIONS);
		} else {
			viewPager.setCurrentItem(FRAGMENT_ACCOUNT);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private String getFragmentTag(int position) {
		return "android:switcher:" + R.id.pager + ":" + position;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		if (itemId == R.id.action_refresh) {
			loadDevices(false);
			return true;
		} else if (item.getItemId() == R.id.action_about) {
			(new AlertDialog.Builder(this))
					.setTitle(R.string.about_title)
					.setMessage(R.string.about_message)
					.setPositiveButton(android.R.string.ok,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									arg0.cancel();
								}

							}).setCancelable(true).show();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private boolean isCurrentTabDevices() {
		return viewPager.getCurrentItem() == FRAGMENT_SUBSCRIPTIONS
				|| viewPager.getCurrentItem() == FRAGMENT_SUBSCRIBERS;
	}

	private DevicesListListener getDevicesListener() {
		if (isCurrentTabDevices()) {
			Fragment f = getSupportFragmentManager().findFragmentByTag(
					getFragmentTag(viewPager.getCurrentItem()));

			if (f instanceof DevicesListListener) {
				return (DevicesListListener) f;
			}
		}

		return null;
	}

	private void setLoadedDevices() {
		if (isCurrentTabDevices()) {
			DevicesListListener l = getDevicesListener();

			if (l != null) {
				l.onDevicesLoaded(devices);
			}
		}
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		viewPager.setCurrentItem(tab.getPosition());

		if (isCurrentTabDevices()) {
			if (hasAccount()) {
				loadDevices(true);
			} else {
				DevicesListListener l = getDevicesListener();

				if (l != null) {
					l.onDevicesLoadMessage(getString(R.string.no_account));
				}
			}
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// NO-OP
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// NO-OP
	}

	@Override
	public void loadDevices(boolean useCache) {
		if (hasAccount()) {
			if (useCache && devices != null) {
				setLoadedDevices();
			} else {
				Loader<List<SimpleDevice>> loader = getSupportLoaderManager()
						.initLoader(0, null, this);
				if (loader != null) {
					loader.forceLoad();
				}

				DevicesListListener l = getDevicesListener();
				if (l != null) {
					l.onDevicesLoadMessage(getString(R.string.loading_devices));
				}
			}
		} else {
			// set to new empty List for the adapter
			devices = null;
			DevicesListListener l = getDevicesListener();

			if (l != null) {
				l.onDevicesLoadMessage(getString(R.string.no_account));
			}
		}
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			if (position == FRAGMENT_ACCOUNT) {
				return new AccountsFragment();
			} else if (position == FRAGMENT_SUBSCRIPTIONS) {
				Fragment fragment = new DevicesFragment();
				Bundle b = new Bundle();
				b.putBoolean(ARGUMENT_ISSUBSCRIPTIONS, true);
				fragment.setArguments(b);
				return fragment;
			} else if (position == FRAGMENT_SUBSCRIBERS) {
				Fragment fragment = new DevicesFragment();
				Bundle b = new Bundle();
				b.putBoolean(ARGUMENT_ISSUBSCRIPTIONS, false);
				fragment.setArguments(b);
				return fragment;
			}

			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();

			switch (position) {
			case FRAGMENT_ACCOUNT:
				return getString(R.string.title_accounts).toUpperCase(l);
			case FRAGMENT_SUBSCRIPTIONS:
				return getString(R.string.title_subscriptions).toUpperCase(l);
			case FRAGMENT_SUBSCRIBERS:
				return getString(R.string.title_subscribers).toUpperCase(l);
			}

			return null;
		}
	}

	@Override
	public String getAccount() {
		return account;
	}

	@Override
	public void setAccount(String account) {
		this.account = account;
		if (account != null) {
			// store the account
			getSharedPreferences(getString(R.string.app_name),
					Context.MODE_PRIVATE).edit()
					.putString(PREFERENCE_ACCOUNT_NAME, account).commit();
			setCurrentTab();
			// add the device
			getSupportLoaderManager().initLoader(getNextLoaderId(), null, this);

			DevicesListListener l = getDevicesListener();
			if (l != null) {
				l.onDevicesLoadMessage(getString(R.string.loading_devices));
			}

			// register with GCM, this is an asynchronous operation
			GCMIntentService.register(getApplicationContext());
		} else {
			devices = null;
			removeDevice(deviceId);
			account = null;
			deviceId = null;
			getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
					.edit().putString(PREFERENCE_ACCOUNT_NAME, account)
					.putLong(PREFERENCE_DEVICE_ID, deviceId).commit();
			GCMIntentService.unregister(getApplicationContext());
		}
	}

	@Override
	public boolean hasAccount() {
		return account != null;
	}

	@Override
	public Long getDeviceId() {
		return deviceId;
	}

	@Override
	public Long getDeviceId(int which) {
		if (devices != null && which < devices.size()) {
			return devices.get(which).getId();
		}

		return null;
	}

	@Override
	public Loader<List<SimpleDevice>> onCreateLoader(int which, Bundle args) {
		if (which > 0) {
			if (args != null && args.containsKey(EXTRA_DEREGISTER_ID)) {
				// create a loader for deregistering an additional device
				return new DevicesLoader(this, account,
						args.getLong(EXTRA_DEREGISTER_ID), devices);
			} else {
				return new DevicesLoader(this, account);
			}
		} else if (deviceId != null) {
			// create loader 0 for loading devices
			return new DevicesLoader(this, account, deviceId);
		} else {
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<List<SimpleDevice>> loader,
			List<SimpleDevice> devices) {
		if (loader.getId() > 0) {
			getSupportLoaderManager().destroyLoader(loader.getId());
			loaderIds.remove((Integer) loader.getId());
		}

		this.devices = devices;

		if (devices != null) {
			// either the initial load, or an updated list after deregistering
			// an
			// additional device
			setLoadedDevices();
		} else {
			// network error
			DevicesListListener l = getDevicesListener();

			if (l != null) {
				l.onDevicesLoadMessage(getString(R.string.connection_error));
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<List<SimpleDevice>> arg0) {
		// NO-OP
	}

	@Override
	public void removeDevice(Long id) {
		if (id != null) {
			Bundle extras = new Bundle();
			extras.putLong(EXTRA_DEREGISTER_ID, id);
			getSupportLoaderManager().initLoader(getNextLoaderId(), extras,
					this);
		}
	}

	@Override
	public void confirmRemoval(Long id) {
		new ConfirmDialog().setDeviceId(id).show(getSupportFragmentManager(),
				"confirm:deregister");
	}

	@Override
	public void doSignIn() {
		viewPager.setCurrentItem(FRAGMENT_ACCOUNT);
	}

}
