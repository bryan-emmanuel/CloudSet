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
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityManager;
import javax.persistence.Query;

@Api(name = "deviceEndpoint",
namespace = @ApiNamespace(ownerDomain = "piusvelte.com", ownerName = "piusvelte.com", packagePath = "cloudset.gwt.server"),
clientIds = {Ids.WEB_CLIENT_ID, Ids.ANDROID_CLIENT_ID},
audiences = {Ids.ANDROID_AUDIENCE})
public class DeviceEndpoint {

	/**
	 * This method lists all the entities inserted in datastore.
	 * It uses HTTP GET method and paging support.
	 *
	 * @return A CollectionResponse class containing the list of all entities
	 * persisted and a cursor to the next page.
	 * @throws OAuthRequestException 
	 */
	@SuppressWarnings({ "unchecked" })
	@ApiMethod(name = "list",
	httpMethod = "GET",
	path = "device")
	public List<Device> listDevice(
			User user,
			@Nullable @Named("setting") String setting,
			@Nullable @Named("limit") Integer limit) throws OAuthRequestException {
		if (user != null) {
			List<Device> devices;
			EntityManager mgr = getEntityManager();
			try {
				Query query;
				if (setting != null) {
					query = mgr.createQuery("select from Device as Device where nickname = :nickname and settings in (:settings)")
							.setParameter("nickname", user.getNickname())
							.setParameter("settings", setting);
				} else {
					query = mgr.createQuery("select from Device as Device where nickname = :nickname")
							.setParameter("nickname", user.getNickname());
				}
				if (limit != null) {
					query.setFirstResult(0);
					query.setMaxResults(limit);
				}
				devices = query.getResultList();
			} finally {
				mgr.close();
			}
			return devices;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	/**
	 * This method gets the entity having primary key id. It uses HTTP GET method.
	 *
	 * @param id the primary key of the java bean.
	 * @return The entity with primary key id.
	 * @throws OAuthRequestException 
	 */
	@ApiMethod(name = "get",
			httpMethod = "GET",
			path = "device/{id}")
	public Device getDevice(User user, @Named("id") String id) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			Device deviceinfo = null;
			try {
				deviceinfo = mgr.find(Device.class, id);
			} finally {
				mgr.close();
			}
			return deviceinfo;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	/**
	 * This inserts a new entity into App Engine datastore. If the entity already
	 * exists in the datastore, an exception is thrown.
	 * It uses HTTP POST method.
	 *
	 * @param deviceinfo the entity to be inserted.
	 * @return The inserted entity.
	 */
	@ApiMethod(name = "add",
			httpMethod = "PUT",
			path = "device")
	public Device addDevice(User user, Device device) throws OAuthRequestException {
		if (user != null) {
			device.setNickname(user.getNickname());
			EntityManager mgr = getEntityManager();
			try {
				if (containsDevice(device)) {
					throw new EntityExistsException("Object already exists");
				}
				mgr.persist(device);
			} finally {
				mgr.close();
			}
			return device;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	/**
	 * This method is used for updating an existing entity. If the entity does not
	 * exist in the datastore, an exception is thrown.
	 * It uses HTTP PUT method.
	 *
	 * @param device the entity to be updated.
	 * @return The updated entity.
	 */
	@ApiMethod(name = "update",
			httpMethod = "POST",
			path = "device/{id}")
	public Device updateDevice(User user, Device device) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			try {
				if (!containsDevice(device)) {
					throw new EntityNotFoundException("Object does not exist");
				}
				mgr.persist(device);
			} finally {
				mgr.close();
			}
			return device;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	/**
	 * This method removes the entity with primary key id.
	 * It uses HTTP DELETE method.
	 *
	 * @param id the primary key of the entity to be deleted.
	 * @return The deleted entity.
	 */
	@ApiMethod(name = "remove",
			httpMethod = "DELETE",
			path = "device/{id}")
	public Device removeDevice(User user, @Named("id") String id) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			Device device = null;
			try {
				device = mgr.find(Device.class, id);
				mgr.remove(device);
			} finally {
				mgr.close();
			}
			return device;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}

	private boolean containsDevice(Device device) {
		EntityManager mgr = getEntityManager();
		boolean contains = true;
		try {
			Device item = mgr.find(Device.class,
					device.getDeviceRegistrationID());
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
