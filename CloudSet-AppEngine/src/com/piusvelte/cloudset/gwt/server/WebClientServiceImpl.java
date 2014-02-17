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

import java.util.List;

import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.piusvelte.cloudset.gwt.client.WebClientService;
import com.piusvelte.cloudset.gwt.shared.SimpleDevice;
import com.piusvelte.cloudset.gwt.shared.SimpleAction;

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

	// load all devices for the user
	@Override
	public List<SimpleDevice> getDevices() throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			try {
				return new DeviceEndpoint().list(userService.getCurrentUser());
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
	public List<SimpleDevice> getSubscribers(Long deviceId)
			throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			try {
				return new DeviceEndpoint().subscribers(userService.getCurrentUser(), deviceId);
			} catch (OAuthRequestException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("error getting devices");
			}
		} else {
			throw new IllegalArgumentException("not logged in");
		}
	}

	@Override
	public SimpleAction subscribe(Long subscriberId, Long publisherId,
			String actionName) throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			try {
				return new DeviceEndpoint().subscribe(userService.getCurrentUser(), subscriberId, publisherId, actionName);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("error getting devices");
			}
		} else {
			throw new IllegalArgumentException("not logged in");
		}
	}

	@Override
	public void unsubscribe(Long subscriberId, Long publicationId)
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

	@Override
	public List<SimpleAction> getSubscriptions(Long subscriberId, Long publisherId)
			throws IllegalArgumentException {
		if (userService.isUserLoggedIn()) {
			try {
				return new DeviceEndpoint().subscriptions(userService.getCurrentUser(), subscriberId, publisherId);
			} catch (OAuthRequestException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("error getting devices");
			}
		} else {
			throw new IllegalArgumentException("not logged in");
		}
	}

}
