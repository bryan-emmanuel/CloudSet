/*
 * Mosaic - Location Based Messaging
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

import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.piusvelte.cloudset.gwt.client.WebClientService;
import com.piusvelte.cloudset.gwt.shared.Action;
import com.piusvelte.cloudset.gwt.shared.Device;

@SuppressWarnings("serial")
public class WebClientServiceImpl extends RemoteServiceServlet implements
WebClientService {
	
	final static public UserService userService = UserServiceFactory.getUserService();

	@Override
	public String getUserNickname() throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			return userService.getCurrentUser().getNickname();
		} else {
			throw new IllegalArgumentException("not logged in");
		}
	}
	
	@Override
	public String getAuthenticationURL(String url) throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			return userService.createLogoutURL(url);
		} else {
			return userService.createLoginURL(url);
		}
	}
	
	private List<Device> makeSerializable(List<Device> streamingResults) {
		List<Device> devices = new ArrayList<Device>();
		for (Device device : streamingResults) {
			ArrayList<Long> publications = new ArrayList<Long>();
			publications.addAll(device.getPublications());
			device.setPublications(publications);
			ArrayList<Long> subscriptions = new ArrayList<Long>();
			subscriptions.addAll(device.getSubscriptions());
			device.setSubscriptions(subscriptions);
			devices.add(device);
		}
		return devices;
	}

	// load all devices for the user
	@Override
	public List<Device> getDevices() throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			try {
				return makeSerializable(new DeviceEndpoint().list(userService.getCurrentUser()));
			} catch (OAuthRequestException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("error getting devices");
			}
		} else {
			throw new IllegalArgumentException("not logged in");
		}
	}
	
	// load all devices synced to from a selected device
	@Override
	public List<Device> getSubscribers(String deviceId)
			throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			try {
				return makeSerializable(new DeviceEndpoint().subscribers(userService.getCurrentUser(), deviceId));
			} catch (OAuthRequestException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("error getting devices");
			}
		} else {
			throw new IllegalArgumentException("not logged in");
		}
	}

	@Override
	public Action subscribe(String subscriberId, String publisherId,
			String action) throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			try {
				return new DeviceEndpoint().subscribe(userService.getCurrentUser(), subscriberId, publisherId, action);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("error getting devices");
			}
		} else {
			throw new IllegalArgumentException("not logged in");
		}
	}

	@Override
	public void unsubscribe(String subscriberId, Long publicationId)
			throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			try {
				new DeviceEndpoint().unsubscribe(userService.getCurrentUser(), subscriberId, publicationId);
			} catch (OAuthRequestException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("error getting devices");
			}
		} else {
			throw new IllegalArgumentException("not logged in");
		}
	}
	
}
