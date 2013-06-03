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
package com.piusvelte.cloudset.gwt.server;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.google.appengine.api.datastore.Key;

@Entity
public class Subscriber {
	
	// Devices publish actions, and subscribe to the actions of other devices

	/*
	 * The Google Cloud Messaging registration token for the device. This token
	 * indicates that the device is able to receive messages sent via GCM.
	 */
	@Id
	private String id;
	
	private long timestamp;
	
	// user's account
	private String account;
	
	// nickname for the device
	private String nickname;
	
	// android.os.Build.MODEL
	private String model;
	
	// subscriptions
	@OneToMany(fetch = FetchType.EAGER)
	private List<Key> subscriptions;

	public List<Key> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(List<Key> subscriptions) {
		this.subscriptions = subscriptions;
	}
	
	public void addSubscription(Key subscription) {
		if (subscriptions == null) {
			subscriptions = new ArrayList<Key>();
		} else if (subscriptions.contains(subscription)) {
			return;
		}
		subscriptions.add(subscription);
	}
	
	public void removeSubscription(Key subscription) {
		if (subscriptions != null) {
			subscriptions.remove(subscription);
		}
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}
}
