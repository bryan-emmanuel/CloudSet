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

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import com.piusvelte.cloudset.gwt.server.Publication.Extra;

@Api(name = "publicationendpoint",
namespace = @ApiNamespace(ownerDomain = "piusvelte.com", ownerName = "piusvelte.com", packagePath = "cloudset.gwt.server"),
clientIds = {Ids.WEB_CLIENT_ID, Ids.ANDROID_CLIENT_ID},
audiences = {Ids.ANDROID_AUDIENCE})
public class PublicationEndpoint {

	private static final SubscriberEndpoint endpoint = new SubscriberEndpoint();

	public void publish(User user, Publication publication)
			throws IOException, OAuthRequestException {

		if (user != null) {
			Sender sender = new Sender(Ids.API_KEY);

			EntityManager mgr = getEntityManager();

			List<Subscriber> subscribers;
			try {
				publication.setKey(getPublicationKey(publication.getPublisher(), publication.getAction()));
				mgr.persist(publication);
				subscribers = subscribers(user, publication.getKey());
				if ((subscribers != null) && (subscribers.size() > 0)) {				
					for (Subscriber device : subscribers) {
						doSendViaGcm(user, publication.getAction(), publication.getExtras(), sender, device);
					}
				}
			} catch (OAuthRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				mgr.close();
			}
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	private static Result doSendViaGcm(User user, String action, List<Extra> extras, Sender sender, Subscriber device) throws IOException {
		Message.Builder builder = new Message.Builder();
		builder.addData("action", action);
		if (extras != null) {
			for (Extra extra : extras) {
				builder.addData(extra.getName(), extra.getValue());
			}
		}
		Message msg = builder.build();
		Result result = sender.send(msg, device.getID(), 5);
		if (result.getMessageId() != null) {
			String canonicalRegId = result.getCanonicalRegistrationId();
			if (canonicalRegId != null) {
				try {
					endpoint.remove(user, device.getID());
					device.setID(canonicalRegId);
					endpoint.add(user, device);
				} catch (OAuthRequestException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			String error = result.getErrorCodeName();
			if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
				try {
					endpoint.remove(user, device.getID());
				} catch (OAuthRequestException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private Key getPublicationKey(String publisher, String action) {
		EntityManager mgr = getEntityManager();
		Key key = null;
		try {
			Query query = mgr.createQuery("select from Publication as Publication where action = :action and publisher = :publisher")
					.setParameter("action", action)
					.setParameter("publisher", publisher)
					.setFirstResult(0)
					.setMaxResults(1);
			List<Publication> publications = query.getResultList();
			if ((publications != null) && (publications.size() > 0)) {
				key = publications.get(0).getKey();
			}
		} finally {
			mgr.close();
		}
		return key;
	}

	@SuppressWarnings("unchecked")
	private List<Subscriber> subscribers(
			User user,
			Key publication) throws OAuthRequestException {
		if (user != null) {
			List<Subscriber> subscribers;
			EntityManager mgr = getEntityManager();
			try {
				Query query = mgr.createQuery("select from Subscriber as Subscriber where subscriptions in (:publications)")
						.setParameter("publications", publication);
				subscribers = query.getResultList();
			} finally {
				mgr.close();
			}
			return subscribers;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}
}
