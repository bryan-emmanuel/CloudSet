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
import com.piusvelte.cloudset.gwt.shared.SimpleAction;
import com.piusvelte.cloudset.gwt.shared.SimpleDevice;

public class MainLayoutPanel extends Composite {

	private final WebClientServiceAsync webClientService = GWT.create(WebClientService.class);

	private static MainLayoutPanelUiBinder uiBinder = GWT
			.create(MainLayoutPanelUiBinder.class);

	interface MainLayoutPanelUiBinder extends UiBinder<Widget, MainLayoutPanel> {
	}

	public MainLayoutPanel() {
		initWidget(uiBinder.createAndBindUi(this));

		contentHeaderPanel.add(new Label("Cloud Set synchronizes setting across Android devices. Please sign in to see and manage your registered devices"));

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
						// TODO
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

	int selectedPublisher = 0;
	int selectedSubscriber = 0;

	// Devices
	public void loadPublishers() {
		publishersList.clear();
		publishersList.add(new Label("Loading devices..."));
		webClientService.getDevices(new PublisherCallback());
	}

	public class PublisherClickHandler implements ClickHandler {

		private int index;
		private Long deviceId;

		public PublisherClickHandler(int index, Long deviceId) {
			this.index = index;
			this.deviceId = deviceId;
		}

		@Override
		public void onClick(ClickEvent event) {
			publishersList.getWidget(selectedPublisher).setStyleName("gwt-Button");
			selectedPublisher = index;
			publishersList.getWidget(selectedPublisher).setStyleName("selectedDevice");
			loadSubscribers(deviceId);
		}

	}

	public class PublisherCallback implements AsyncCallback<List<SimpleDevice>> {

		@Override
		public void onFailure(Throwable caught) {
			// NO-OP
		}

		@Override
		public void onSuccess(List<SimpleDevice> result) {
			selectedPublisher = 0;
			publishersList.clear();
			if (result.size() > selectedPublisher) {
				// default to the first device
				SimpleDevice device = result.get(selectedPublisher);
				if (result.size() > 1) {
					loadSubscribers(device.getId());
				} else {
					subscribersList.clear();
					actionsList.clear();
				}
				publishersList.add(new Button(URL.decode(device.getModel()).replace("+", " "),
						new PublisherClickHandler(selectedPublisher, device.getId())));
				publishersList.getWidget(selectedPublisher).setStyleName("selectedDevice");
				for (int i = 1, s = result.size(); i < s; i++) {
					device = result.get(i);
					publishersList.add(new Button(URL.decode(device.getModel()).replace("+", " "),
							new PublisherClickHandler(i, device.getId())));
				}
			} else {
				subscribersList.clear();
				actionsList.clear();
			}
		}

	}

	// Syncing Devices
	public void loadSubscribers(Long deviceId) {
		webClientService.getSubscribers(deviceId, new SubscribersCallback(deviceId));
	}

	public class SubscriberClickHandler implements ClickHandler {

		private int index;
		private Long publisherId;
		private Long subscriberId;

		public SubscriberClickHandler(int index, Long publisherId, Long subscriberId) {
			this.index = index;
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onClick(ClickEvent event) {
			subscribersList.getWidget(selectedSubscriber).setStyleName("gwt-Button");
			selectedSubscriber = index;
			subscribersList.getWidget(selectedSubscriber).setStyleName("selectedDevice");
			loadActions(publisherId, subscriberId);
		}

	}

	public class SubscribersCallback implements AsyncCallback<List<SimpleDevice>> {

		private Long deviceId;

		public SubscribersCallback(Long deviceId) {
			this.deviceId = deviceId;
		}

		@Override
		public void onFailure(Throwable caught) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onSuccess(List<SimpleDevice> result) {
			selectedSubscriber = 0;
			subscribersList.clear();
			if (result.size() > selectedSubscriber) {
				SimpleDevice device = result.get(selectedSubscriber);
				loadActions(deviceId, device.getId());
				subscribersList.add(new Button(URL.decode(device.getModel()).replace("+", " "),
						new SubscriberClickHandler(selectedSubscriber, deviceId, device.getId())));
				subscribersList.getWidget(selectedSubscriber).setStyleName("selectedDevice");
				for (int i = 1, s = result.size(); i < s; i++) {
					device = result.get(i);
					subscribersList.add(new Button(URL.decode(device.getModel()).replace("+", " "),
							new SubscriberClickHandler(i, deviceId, device.getId())));
				}
			} else {
				actionsList.clear();
				subscribersList.add(new Label("You have no other devices to sync. Please register more through the app."));
			}
		}

	}

	// Actions
	public static final String[] ACTIONS = new String[]{"android.net.wifi.WIFI_STATE_CHANGED",
		"android.bluetooth.adapter.action.STATE_CHANGED",
		"android.media.VOLUME_CHANGED_ACTION",
	"android.media.RINGER_MODE_CHANGED"};
	public static final String[] ACTION_NAMES = new String[]{"Wi-Fi", "Bluetooth", "Volume", "Ringer"};

	public void loadActions(Long publisherId, Long subscriberId) {
		webClientService.getSubscriptions(subscriberId, publisherId, new ActionsCallback(publisherId, subscriberId));
	}

	public class ActionValueChangeHandler implements ValueChangeHandler<Boolean> {

		private Long publisherId;
		private Long subscriberId;
		private String action;
		private Long publicationId;

		public ActionValueChangeHandler(Long publisherId, Long subscriberId, String action, Long publicationId) {
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

	public class ActionsCallback implements AsyncCallback<List<SimpleAction>> {

		private Long publisherId;
		private Long subscriberId;

		public ActionsCallback(Long publisherId, Long subscriberId) {
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onFailure(Throwable caught) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onSuccess(List<SimpleAction> simpleActions) {
			actionsList.clear();
			for (int i = 0; i < ACTIONS.length; i++) {
				boolean isSubscribed = false;
				Long publicationId = null;
				for (SimpleAction simpleAction : simpleActions) {
					if (simpleAction.getName().equals(ACTIONS[i])) {
						isSubscribed = true;
						publicationId = simpleAction.getId();
						break;
					}
				}
				CheckBox actionCheckBox = new CheckBox(ACTION_NAMES[i]);
				actionCheckBox.setValue(isSubscribed);
				actionCheckBox.addValueChangeHandler(new ActionValueChangeHandler(publisherId, subscriberId, ACTIONS[i], publicationId));
				actionsList.add(actionCheckBox);
			}
		}

	}

	public class SubscribeCallback implements AsyncCallback<SimpleAction> {

		private Long publisherId;
		private Long subscriberId;

		public SubscribeCallback(Long publisherId, Long subscriberId) {
			this.publisherId = publisherId;
			this.subscriberId = subscriberId;
		}

		@Override
		public void onFailure(Throwable caught) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onSuccess(SimpleAction result) {
			loadActions(publisherId, subscriberId);
		}

	}

	public class UnsubscribeCallback implements AsyncCallback<Void> {

		private Long publisherId;
		private Long subscriberId;

		public UnsubscribeCallback(Long publisherId, Long subscriberId) {
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
