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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;

public class AccountsFragment extends ListFragment {

	ArrayAdapter<String> adapter;
	Button deregister;
	TextView empty;

	public AccountsFragment() {
	}

	AccountsListener callback;

	public interface AccountsListener {

		public String getAccount();

		public void setAccount(String account);

		public boolean hasRegistration();

	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        	callback = (AccountsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AccountsListener");
        }
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
		callback.setAccount(adapter.getItem(position));
		empty.setText(R.string.registering);
		adapter.clear();
		adapter.notifyDataSetChanged();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.accounts, container, false);
		deregister = (Button) rootView.findViewById(R.id.deregister);
		empty = (TextView) rootView.findViewById(android.R.id.empty);
		deregister.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				GCMIntentService.unregister(getActivity().getApplicationContext());
			}

		});
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
		setListAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		View v = getView();

		if (callback.hasRegistration()) {
			v.findViewById(android.R.id.list).setVisibility(View.GONE);
			v.findViewById(android.R.id.empty).setVisibility(View.GONE);
			TextView tv = (TextView) v.findViewById(R.id.account);
			tv.setText(callback.getAccount());
			tv.setVisibility(View.VISIBLE);
			v.findViewById(R.id.deregister).setVisibility(View.VISIBLE);
		} else {
			v.findViewById(android.R.id.list).setVisibility(View.VISIBLE);
			v.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
			TextView tv = (TextView) v.findViewById(R.id.account);
			tv.setText("");
			tv.setVisibility(View.GONE);
			v.findViewById(R.id.deregister).setVisibility(View.GONE);
			getAccountNames();
		}
	}

}