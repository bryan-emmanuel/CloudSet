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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ParagraphElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.piusvelte.cloudset.gwt.shared.Action;
import com.piusvelte.cloudset.gwt.shared.Device;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class CloudSet implements EntryPoint {
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network "
			+ "connection and try again.";

	private final WebClientServiceAsync webClientService = GWT
			.create(WebClientService.class);

	private VerticalPanel devicesPanel = new VerticalPanel();

	public void onModuleLoad() {

		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);

		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");

		final Label textToServerLabel = new Label();

		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		// Add a handler to close the DialogBox
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});

		final Button authButton = new Button("Sign in");
		RootPanel.get("authContainer").add(authButton);
		authButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				webClientService.getAuthenticationURL(
						Window.Location.getHref(),
						new AsyncCallback<String>() {

							@Override
							public void onFailure(Throwable caught) {
								dialogBox
								.setText("Remote Procedure Call - Failure");
								serverResponseLabel
								.addStyleName("serverResponseLabelError");
								serverResponseLabel.setHTML(SERVER_ERROR);
								dialogBox.center();
								closeButton.setFocus(true);
							}

							@Override
							public void onSuccess(String result) {
								Window.Location.replace(result);
							}

						});
			}

		});

		webClientService.getUserNickname(
				new AsyncCallback<String>() {

					@Override
					public void onFailure(Throwable caught) {
						authButton.setText("Sign in");
					}

					@Override
					public void onSuccess(String result) {
						authButton.setText(result);
						loadDevices();
					}

				});

		contentTitle = RootPanel.get("contentTitle").getElement();
		devicesContainer = RootPanel.get("devicesContainer");

		devicesContainer.add(devicesPanel);

		syncPanel.add(syncFromPanel, "Sync From");
		syncPanel.add(syncToPanel, "Sync To");

	}

	private RootPanel devicesContainer;
	
	public void setLoading() {
		devicesPanel.clear();
		devicesPanel.add(new Label("Loading..."));
	}

	private Element contentTitle;

	public void loadDevices() {
		setLoading();
		webClientService.getDevices(new AsyncCallback<List<Device>>() {

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSuccess(List<Device> result) {
				// TODO Auto-generated method stub
				contentTitle.setInnerHTML("Registered Devices");
				devicesPanel.clear();
				for (Device device : result) {
					devicesPanel.add(new Button(URL.decode(device.getModel()),
							new DeviceClickHandler(device.getId())));
				}
			}

		});
	}

	public class DeviceClickHandler implements ClickHandler {

		private String deviceId;

		public DeviceClickHandler(String deviceId) {
			this.deviceId = deviceId;
		}

		@Override
		public void onClick(ClickEvent event) {
			loadSubscribers(deviceId);
		}

	}

	public class SyncClickHandler implements ClickHandler {

		private String publisherId;
		private String subscriberId;

		public SyncClickHandler(String publisherId, String subscriberId) {
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onClick(ClickEvent event) {
			loadActions(publisherId, subscriberId);
		}

	}

	private VerticalPanel actionsPanel = new VerticalPanel();

	public static final String[] ACTIONS = new String[]{"android.net.wifi.WIFI_STATE_CHANGED",
		"android.bluetooth.adapter.action.STATE_CHANGED",
		"android.media.VOLUME_CHANGED_ACTION",
		"android.media.RINGER_MODE_CHANGED"};
	public static final String[] ACTION_NAMES = new String[]{"Wi-Fi", "Bluetooth", "Volume", "Ringer"};
	
	public class SubscribeCallback implements AsyncCallback<Action> {

		private String publisherId;
		private String subscriberId;
		
		public SubscribeCallback(String publisherId, String subscriberId) {
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onFailure(Throwable caught) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSuccess(Action result) {
			loadActions(publisherId, subscriberId);
		}
		
	}
	
	public class UnsubscribeCallback implements AsyncCallback<Void> {

		private String publisherId;
		private String subscriberId;
		
		public UnsubscribeCallback(String publisherId, String subscriberId) {
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onFailure(Throwable caught) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSuccess(Void result) {
			loadActions(publisherId, subscriberId);
		}
		
	}

	public class ActionClickHandler implements ClickHandler {

		private String publisherId;
		private String subscriberId;
		private String action;
		private Long publicationId;

		public ActionClickHandler(String publisherId, String subscriberId, String action, Long publicationId) {
			this.action = action;
			this.publicationId = publicationId;
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onClick(ClickEvent event) {
			boolean checked = ((CheckBox) event.getSource()).isChecked();
			if (checked) {
				webClientService.subscribe(subscriberId, publisherId, action, new SubscribeCallback(publisherId, subscriberId));
			} else {
				webClientService.unsubscribe(subscriberId, publicationId, new UnsubscribeCallback(publisherId, subscriberId));
			}
		}

	}

	public class ActionsCallback implements AsyncCallback<List<Action>> {

		private String publisherId;
		private String subscriberId;

		public ActionsCallback(String publisherId, String subscriberId) {
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onFailure(Throwable caught) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSuccess(List<Action> result) {
			// TODO Auto-generated method stub
			devicesPanel.clear();

			actionsPanel.clear();

			for (int i = 0; i < ACTIONS.length; i++) {
				boolean isSubscribed = false;
				for (Action action : result) {
					if (action.getName().equals(ACTIONS[i])) {
						isSubscribed = true;
						break;
					}
				}
				CheckBox cb = new CheckBox(ACTION_NAMES[i]);
				cb.setChecked(isSubscribed);
				actionsPanel.add(cb);
			}
		}

	}

	public void loadActions(String publisherId, String subscriberId) {
		setLoading();
		webClientService.getSubscriptions(subscriberId, publisherId, new ActionsCallback(publisherId, subscriberId));
	}

	private TabPanel syncPanel = new TabPanel();

	private VerticalPanel syncFromPanel = new VerticalPanel();

	private VerticalPanel syncToPanel = new VerticalPanel();

	public class SubscribersCallback implements AsyncCallback<List<Device>> {

		private String deviceId;

		public SubscribersCallback(String deviceId) {
			this.deviceId = deviceId;
		}

		@Override
		public void onFailure(Throwable caught) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSuccess(List<Device> result) {
			devicesPanel.clear();

			syncFromPanel.clear();
			syncToPanel.clear();

			for (Device device : result) {
				syncFromPanel.add(new Button(URL.decode(device.getModel()),
						new SyncClickHandler(deviceId, device.getId())));
				syncToPanel.add(new Button(URL.decode(device.getModel()),
						new SyncClickHandler(device.getId(), deviceId)));
			}

			devicesPanel.add(syncPanel);
		}

	}

	public void loadSubscribers(String deviceId) {
		setLoading();
		webClientService.getSubscribers(deviceId, new SubscribersCallback(deviceId));
	}
}
