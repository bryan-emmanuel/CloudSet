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

@Entity
public class Device {

	/*
	 * The Google Cloud Messaging registration token for the device. This token
	 * indicates that the device is able to receive messages sent via GCM.
	 */
	@Id
	private String deviceRegistrationID;
	
	private long timestamp;
	
	private String nickname;
	
	// android.os.Build.MODEL
	private String model;
	
	// synced settings
	@OneToMany(fetch = FetchType.EAGER)
	private List<String> actions;

	public List<String> getActions() {
		return actions;
	}

	public void setActions(List<String> actions) {
		this.actions = actions;
	}
	
	public void addAction(String action) {
		if (actions == null) {
			List<String> newActions = new ArrayList<String>();
			newActions.add(action);
			actions = newActions;
		} else if (!actions.contains(action)) {
			List<String> newActions = new ArrayList<String>();
			for (String a : actions) {
				newActions.add(a);
			}
			newActions.add(action);
			actions = newActions;
		}
	}
	
	public void removeAction(String action) {
		if ((actions != null) && actions.contains(action)) {
			List<String> newActions = new ArrayList<String>();
			for (String a : actions) {
				if (!a.equals(action)) {
					newActions.add(a);
				}
			}
			if (newActions.isEmpty()) {
				actions = null;
			} else {
				actions = newActions;
			}
		}
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getDeviceRegistrationID() {
		return deviceRegistrationID;
	}

	public void setDeviceRegistrationID(String deviceRegistrationID) {
		this.deviceRegistrationID = deviceRegistrationID;
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
}
