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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.piusvelte.cloudset.gwt.shared.SimpleAction;
import com.piusvelte.cloudset.gwt.shared.SimpleDevice;

public interface WebClientServiceAsync {

	void getUserNickname(
			AsyncCallback<String> callback);

	void getAuthenticationURL(
			String url,
			AsyncCallback<String> callback);

	void getDevices(
			AsyncCallback<List<SimpleDevice>> callback);

	void getSubscribers(
			Long deviceId,
			AsyncCallback<List<SimpleDevice>> callback);

	void subscribe(
			Long subscriberId,
			Long publisherId,
			String action,
			AsyncCallback<SimpleAction> callback);

	void unsubscribe(
			Long subscriberId,
			Long publicationId,
			AsyncCallback<Void> callback);

	void getSubscriptions(
			Long subscriberId,
			Long publisherId,
			AsyncCallback<List<SimpleAction>> callback);

}
