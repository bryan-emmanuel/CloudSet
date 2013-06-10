package com.piusvelte.cloudset.gwt.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.piusvelte.cloudset.gwt.shared.Action;
import com.piusvelte.cloudset.gwt.shared.Device;

public class MainLayoutPanel extends Composite {

	private final WebClientServiceAsync webClientService = GWT.create(WebClientService.class);

	private static MainLayoutPanelUiBinder uiBinder = GWT
			.create(MainLayoutPanelUiBinder.class);

	interface MainLayoutPanelUiBinder extends UiBinder<Widget, MainLayoutPanel> {
	}

	public MainLayoutPanel() {
		initWidget(uiBinder.createAndBindUi(this));
		
		contentHeaderPanel.add(new Label("Cloud Set synchronizes setting across Android devices. Please sign in to see and manager your registered devices"));

		webClientService.getUserNickname(
				new AsyncCallback<String>() {

					@Override
					public void onFailure(Throwable caught) {
						signin.setText("Sign in");
					}

					@Override
					public void onSuccess(String result) {
						signin.setText(result);
						contentHeaderPanel.clear();
						contentHeaderPanel.add(new Label("Android devices registered through the app appear below. Sync selected settings from a device on the left to the device on the right."));
						loadPublishers();
					}

				});
	}

	@UiField
	Button signin;

	@UiHandler("signin")
	void onClick(ClickEvent e) {
		webClientService.getAuthenticationURL(
				Window.Location.getHref(),
				new AsyncCallback<String>() {

					@Override
					public void onFailure(Throwable caught) {
						//TODO
					}

					@Override
					public void onSuccess(String result) {
						Window.Location.replace(result);
					}

				});
	}
	
	@UiField
	FlowPanel contentHeaderPanel;

	@UiField
	FlowPanel publishersList;

	@UiField
	FlowPanel subscribersList;

	@UiField
	FlowPanel actionsList;

	// Devices
	public void loadPublishers() {
		publishersList.clear();
		publishersList.add(new Label("Loading devices..."));
		webClientService.getDevices(new PublisherCallback());
	}

	public class PublisherClickHandler implements ClickHandler {

		private String deviceId;

		public PublisherClickHandler(String deviceId) {
			this.deviceId = deviceId;
		}

		@Override
		public void onClick(ClickEvent event) {
			loadSubscribers(deviceId);
		}

	}

	public class PublisherCallback implements AsyncCallback<List<Device>> {

		@Override
		public void onFailure(Throwable caught) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onSuccess(List<Device> result) {
			publishersList.clear();
			for (Device device : result) {
				publishersList.add(new Button(URL.decode(device.getModel()),
						new PublisherClickHandler(device.getId())));
			}
			// load the first device
			if (result.size() > 0) {
				loadSubscribers(result.get(0).getId());
			}
		}

	}

	// Syncing Devices
	public void loadSubscribers(String deviceId) {
		subscribersList.clear();
		subscribersList.add(new Label("Loading devices..."));
		webClientService.getSubscribers(deviceId, new SubscribersCallback(deviceId));
	}

	public class SubscriberClickHandler implements ClickHandler {

		private String publisherId;
		private String subscriberId;

		public SubscriberClickHandler(String publisherId, String subscriberId) {
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onClick(ClickEvent event) {
			loadActions(publisherId, subscriberId);
		}

	}

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
			subscribersList.clear();
			if (result.size() > 0) {
				for (Device device : result) {
					subscribersList.add(new Button(URL.decode(device.getModel()),
							new SubscriberClickHandler(deviceId, device.getId())));
				}
				loadActions(deviceId, result.get(0).getId());
			} else {
				actionsList.clear();
				//TODO add message to register another device
			}
		}

	}

	// Actions
	public static final String[] ACTIONS = new String[]{"android.net.wifi.WIFI_STATE_CHANGED",
		"android.bluetooth.adapter.action.STATE_CHANGED",
		"android.media.VOLUME_CHANGED_ACTION",
	"android.media.RINGER_MODE_CHANGED"};
	public static final String[] ACTION_NAMES = new String[]{"Wi-Fi", "Bluetooth", "Volume", "Ringer"};

	public void loadActions(String publisherId, String subscriberId) {
		actionsList.clear();
		actionsList.add(new Label("Loading actions..."));
		webClientService.getSubscriptions(subscriberId, publisherId, new ActionsCallback(publisherId, subscriberId));
	}

	public class ActionValueChangeHandler implements ValueChangeHandler<Boolean> {

		private String publisherId;
		private String subscriberId;
		private String action;
		private Long publicationId;

		public ActionValueChangeHandler(String publisherId, String subscriberId, String action, Long publicationId) {
			this.action = action;
			this.publicationId = publicationId;
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onValueChange(ValueChangeEvent<Boolean> event) {
			if (event.getValue()) {
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
			actionsList.clear();
			for (int i = 0; i < ACTIONS.length; i++) {
				boolean isSubscribed = false;
				Long publicationId = null;
				for (Action action : result) {
					if (action.getName().equals(ACTIONS[i])) {
						isSubscribed = true;
						publicationId = action.getId();
						break;
					}
				}
				CheckBox actionCheckBox = new CheckBox(ACTION_NAMES[i]);
				actionCheckBox.setValue(isSubscribed);
				actionCheckBox.addValueChangeHandler(new ActionValueChangeHandler(publisherId, subscriberId, ACTION_NAMES[i], publicationId));
				actionsList.add(actionCheckBox);
			}
		}

	}

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
}
