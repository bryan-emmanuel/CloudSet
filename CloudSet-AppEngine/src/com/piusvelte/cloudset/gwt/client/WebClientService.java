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
package com.piusvelte.cloudset.gwt.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.piusvelte.cloudset.gwt.shared.Action;
import com.piusvelte.cloudset.gwt.shared.Device;

@RemoteServiceRelativePath("webclient")
public interface WebClientService extends RemoteService {

	String getUserNickname() throws IllegalArgumentException;

	String getAuthenticationURL(String url) throws IllegalArgumentException;
	
	List<Device> getDevices() throws IllegalArgumentException;

	List<Device> getSubscribers(String deviceId) throws IllegalArgumentException;
	
	Action subscribe(String subscriberId, String publisherId, String action) throws IllegalArgumentException;
	
	void unsubscribe(String subscriberId, Long publicationId) throws IllegalArgumentException;
	
	List<Action> getSubscriptions(String subscriberId, String publisherId) throws IllegalArgumentException;
	
}
