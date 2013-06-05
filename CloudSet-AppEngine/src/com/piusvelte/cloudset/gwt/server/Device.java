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

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Device {

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
	@Basic
	private List<Long> subscriptions = new ArrayList<Long>();

	public List<Long> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(List<Long> subscriptions) {
		this.subscriptions = subscriptions;
	}

	public boolean subscribe(Long publication) {
		if (subscriptions.contains(publication)) {
			return false;
		} else {
			subscriptions.add(publication);
			return true;
		}

	}

	public boolean unsubscribe(Long publication) {
		if (!subscriptions.contains(publication)) {
			return false;
		} else {
			subscriptions.remove(publication);
			return true;
		}
	}

	@Basic
	private List<Long> publications;

	public List<Long> getPublications() {
		return publications;
	}

	public void setPublications(List<Long> publications) {
		this.publications = publications;
	}

	public boolean addPublication(Long publication) {
		if (publications.contains(publication)) {
			return false;
		} else {
			publications.add(publication);
			return true;
		}
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
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
