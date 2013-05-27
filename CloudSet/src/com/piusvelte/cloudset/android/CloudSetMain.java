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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Locale;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.Deviceendpoint;
import com.piusvelte.cloudset.gwt.server.deviceendpoint.model.Device;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class CloudSetMain extends FragmentActivity implements
ActionBar.TabListener {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	public static final String ACTION_GCM_REGISTERED = "com.piusvelte.cloudset.android.action.GCM_REGISTERED";
	public static final String ACTION_GCM_UNREGISTERED = "com.piusvelte.cloudset.android.action.GCM_UNREGISTERED";
	public static final String ACTION_GCM_ERROR = "com.piusvelte.cloudset.android.action.GCM_ERROR";

	private static final int FRAGMENT_ACCOUNT = 0;
	private static final int FRAGMENT_DEVICES = 1;

	private static String accountName = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(getApplicationContext());
		
		setContentView(R.layout.activity_main);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public void onResume() {
		super.onResume();
		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();
			if (action != null) {
				if (action.equals(ACTION_GCM_ERROR)) {
					Toast.makeText(getApplicationContext(), "Error occurred during device registration", Toast.LENGTH_SHORT).show();
				} else if (action.equals(ACTION_GCM_REGISTERED)) {
				} else if (action.equals(ACTION_GCM_UNREGISTERED)) {	
				}
			}
		} else {
			// make sure that the receiver is registered
			sendBroadcast(new Intent(Intent.ACTION_PACKAGE_REPLACED).addCategory(getPackageName()));
		}

		// check if the account is setup
		SharedPreferences sp = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
		if ((sp.getString(getString(R.string.preference_account_name), null) != null)
				&& (sp.getString(getString(R.string.preference_gcm_registration), null) != null)) {
			accountName = sp.getString(getString(R.string.preference_account_name), null);
		}
		
		if (accountName == null) {
			mViewPager.setCurrentItem(FRAGMENT_ACCOUNT);
		} else {
			mViewPager.setCurrentItem(FRAGMENT_DEVICES);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case FRAGMENT_ACCOUNT:
				return new AccountsFragment();
			case FRAGMENT_DEVICES:
				return new DevicesFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case FRAGMENT_ACCOUNT:
				return getString(R.string.title_accounts).toUpperCase(l);
			case FRAGMENT_DEVICES:
				return getString(R.string.title_devices).toUpperCase(l);
			}
			return null;
		}
	}

	public static class AccountsFragment extends ListFragment {

		ArrayAdapter<String> adapter;

		public AccountsFragment() {

		}

		private void getAccountNames() {
			adapter.clear();
			AccountManager accountManager = AccountManager.get(getActivity().getApplicationContext());
			Account[] accounts = accountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
			String[] names = new String[accounts.length];
			for (int i = 0; i < names.length; i++) {
				adapter.add(accounts[i].name);
			}
			adapter.notifyDataSetChanged();
		}

		@Override
		public void onListItemClick(ListView list, View view, int position, long id) {
			super.onListItemClick(list, view, position, id);
			accountName = adapter.getItem(position);

			// store the account
			getActivity().getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
			.edit()
			.putString(getString(R.string.preference_account_name), accountName)
			.commit();

			// register with GCM, this is an asynchronous operation
			GCMIntentService.register(getActivity().getApplicationContext());

			// move to the devices tab
			((ViewPager) getActivity().findViewById(R.id.pager)).setCurrentItem(FRAGMENT_DEVICES);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View rootView = inflater.inflate(R.layout.accounts, container, false);
			adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
			return rootView;
		}

		@Override
		public void onResume() {
			super.onResume();
			setListAdapter(adapter);
			getAccountNames();
		}

	}

	public static class DevicesFragment extends ListFragment {

		GoogleAccountCredential credential = null;

		ArrayAdapter<String> adapter;
		ArrayList<Device> devices = new ArrayList<Device>();

		public DevicesFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View rootView = inflater.inflate(R.layout.devices, container, false);
			adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
			return rootView;
		}

		@Override
		public void onResume() {
			super.onResume();
			setListAdapter(adapter);
			loadDevices();
		}

		@Override
		public void onListItemClick(ListView list, View view, int position, long id) {
			super.onListItemClick(list, view, position, id);
			startActivity(new Intent(getActivity().getApplicationContext(), DeviceActions.class).putExtra(DeviceActions.EXTRA_DEVICE_REGISTRATION, devices.get(position).getDeviceRegistrationID()));
		}

		public void loadDevices() {
			if (accountName != null) {

				credential = GoogleAccountCredential.usingAudience(getActivity().getApplicationContext(), "server:client_id:" + getActivity().getString(R.string.client_id));
				credential.setSelectedAccountName(accountName);

				(new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {

						Deviceendpoint.Builder endpointBuilder = new Deviceendpoint.Builder(
								AndroidHttp.newCompatibleTransport(),
								new JacksonFactory(),
								credential);
						Deviceendpoint endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();

						try {
							devices.clear();
							devices.addAll(endpoint.deviceEndpoint().list().execute().getItems());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						adapter.clear();
						for (Device device : devices) {
							try {
								adapter.add(URLDecoder.decode(device.getModel(), "UTF-8"));
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						adapter.notifyDataSetChanged();
					}

				}).execute();

			}
		}

	}

}
