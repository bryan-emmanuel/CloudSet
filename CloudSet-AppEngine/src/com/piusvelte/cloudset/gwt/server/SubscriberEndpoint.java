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

import com.piusvelte.cloudset.gwt.server.EMF;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityManager;
import javax.persistence.Query;

@Api(name = "subscriberendpoint",
namespace = @ApiNamespace(ownerDomain = "piusvelte.com", ownerName = "piusvelte.com", packagePath = "cloudset.gwt.server"),
clientIds = {Ids.WEB_CLIENT_ID, Ids.ANDROID_CLIENT_ID},
audiences = {Ids.ANDROID_AUDIENCE})
public class SubscriberEndpoint {

	@SuppressWarnings({ "unchecked" })
	public List<Subscriber> subscribers(
			User user,
			@Named("id") String id) throws OAuthRequestException {
		if (user != null) {
			List<Subscriber> subscribers;
			EntityManager mgr = getEntityManager();
			try {
				Query query = mgr.createQuery("select from Subscriber as Subscriber where account = :account and id <> :id")
						.setParameter("account", user.getNickname())
						.setParameter("id", id);
				subscribers = query.getResultList();
			} finally {
				mgr.close();
			}
			return subscribers;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	public Subscriber get(User user, @Named("id") String id) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			Subscriber subscriber = null;
			try {
				subscriber = mgr.find(Subscriber.class, id);
			} finally {
				mgr.close();
			}
			return subscriber;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	public Subscriber add(User user, Subscriber subscriber) throws OAuthRequestException {
		if (user != null) {
			subscriber.setAccount(user.getNickname());
			EntityManager mgr = getEntityManager();
			try {
				if (containsSubscriber(subscriber)) {
					throw new EntityExistsException("Object already exists");
				}
				mgr.persist(subscriber);
			} finally {
				mgr.close();
			}
			return subscriber;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}
	
	public List<Publication> subscriptions(User user, @Named("subscriber") String subscriberId, @Named("publisher") String publisherId) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			List<Publication> publications = new ArrayList<Publication>();
			Subscriber subscriber;
			try {
				subscriber = mgr.find(Subscriber.class, subscriberId);
				List<Key> subscriptions = subscriber.getSubscriptions();
				if (subscriptions != null) {
					// filter on the publisher
					for (Key subscription : subscriptions) {
						Publication publication = mgr.find(Publication.class, subscription);
						if (publication.getPublisher().equals(publisherId)) {
							publications.add(publication);
						}
					}
				}
			} finally {
				mgr.close();
			}
			return publications;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	public List<Publication> subscribe(User user, @Named("subscriber") String subscriberId, @Named("publisher") String publisherId, @Named("action") String action) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			List<Publication> publications = new ArrayList<Publication>();
			Subscriber subscriber = null;
			try {
				subscriber = mgr.find(Subscriber.class, subscriberId);
				subscriber.addSubscription(getPublication(publisherId, action));
				mgr.persist(subscriber);
				List<Key> subscriptions = subscriber.getSubscriptions();
				if (subscriptions != null) {
					// filter on the publisher
					for (Key subscription : subscriptions) {
						Publication publication = mgr.find(Publication.class, subscription);
						if (publication.getPublisher().equals(publisherId)) {
							publications.add(publication);
						}
					}
				}
			} finally {
				mgr.close();
			}
			return publications;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	public List<Publication> unsubscribe(User user, @Named("subscriber") String subscriberId, Key publicationKey) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			List<Publication> publications = new ArrayList<Publication>();
			Subscriber subscriber = null;
			try {
				subscriber = mgr.find(Subscriber.class, subscriberId);
				Publication publication = mgr.find(Publication.class, publicationKey);
				String publisherId = publication.getPublisher();
				subscriber.removeSubscription(publicationKey);
				mgr.persist(subscriber);
				List<Key> subscriptions = subscriber.getSubscriptions();
				if (subscriptions != null) {
					// filter on the publisher
					for (Key subscription : subscriptions) {
						publication = mgr.find(Publication.class, subscription);
						if (publication.getPublisher().equals(publisherId)) {
							publications.add(publication);
						}
					}
				}
			} finally {
				mgr.close();
			}
			return publications;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	public Subscriber update(User user, Subscriber subscriber) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			try {
				if (!containsSubscriber(subscriber)) {
					throw new EntityNotFoundException("Object does not exist");
				}
				mgr.persist(subscriber);
			} finally {
				mgr.close();
			}
			return subscriber;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	@SuppressWarnings("unchecked")
	public Subscriber remove(User user, @Named("id") String id) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			Subscriber subscriber = null;
			try {
				subscriber = mgr.find(Subscriber.class, id);
				// need to clean up everything
				Query query = mgr.createQuery("select from Publication as Publication where publisher = :publisher")
						.setParameter("publisher", subscriber.getID());
				List<Publication> publications = query.getResultList();
				if (publications != null) {
					for (Publication publication : publications) {
						// update subscriptions
						query = mgr.createQuery("select from Subscriber as Subscriber where subscriptions in (:publication)")
								.setParameter("publication", publication.getKey());
						List<Subscriber> subscribers = query.getResultList();
						for (Subscriber s : subscribers) {
							s.removeSubscription(publication.getKey());
							mgr.persist(s);
						}
						mgr.remove(publication);
					}
				}
				mgr.remove(subscriber);
			} finally {
				mgr.close();
			}
			return subscriber;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	@SuppressWarnings("unchecked")
	private Key getPublication(String publisher, String action) {
		EntityManager mgr = getEntityManager();
		Key key = null;
		try {
			Query query = mgr.createQuery("select from Publication as Publication where action = :action and publisher = :publisher")
					.setParameter("action", action)
					.setParameter("publisher", publisher)
					.setFirstResult(0)
					.setMaxResults(1);
			List<Publication> actions = query.getResultList();
			if ((actions != null) && (actions.size() > 0)) {
				key = actions.get(0).getKey();
			} else {
				// first subscriber triggers the new publication
				Publication publication = new Publication();
				publication.setAction(action);
				publication.setPublisher(publisher);
				publication.setTimestamp(System.currentTimeMillis());
				mgr.persist(publication);
				key = getPublication(publisher, action);
			}
		} finally {
			mgr.close();
		}
		return key;
	}

	private boolean containsSubscriber(Subscriber subscriber) {
		EntityManager mgr = getEntityManager();
		boolean contains = true;
		try {
			Subscriber item = mgr.find(Subscriber.class,
					subscriber.getID());
			if (item == null) {
				contains = false;
			}
		} finally {
			mgr.close();
		}
		return contains;
	}

	private static EntityManager getEntityManager() {
		return EMF.get().createEntityManager();
	}

}
