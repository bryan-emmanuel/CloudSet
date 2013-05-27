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
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityManager;
import javax.persistence.Query;

@Api(name = "deviceendpoint",
namespace = @ApiNamespace(ownerDomain = "piusvelte.com", ownerName = "piusvelte.com", packagePath = "cloudset.gwt.server"),
clientIds = {Ids.WEB_CLIENT_ID, Ids.ANDROID_CLIENT_ID},
audiences = {Ids.ANDROID_AUDIENCE})
public class DeviceEndpoint {
	
	@SuppressWarnings({ "unchecked" })
	public List<Device> list(
			User user,
			@Nullable @Named("setting") String action,
			@Nullable @Named("limit") Integer limit) throws OAuthRequestException {
		if (user != null) {
			List<Device> devices;
			EntityManager mgr = getEntityManager();
			try {
				Query query;
				if (action != null) {
					query = mgr.createQuery("select from Device as Device where nickname = :nickname and actions in (:actions)")
							.setParameter("nickname", user.getNickname())
							.setParameter("actions", action);
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
	
	public Device get(User user, @Named("id") String id) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			Device device = null;
			try {
				device = mgr.find(Device.class, id);
			} finally {
				mgr.close();
			}
			return device;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}
	
	public Device add(User user, Device device) throws OAuthRequestException {
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
	
	public Device addAction(User user, @Named("id") String id, @Named("action") String action) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			Device device = null;
			try {
				device = mgr.find(Device.class, id);
				device.addAction(action);
				mgr.persist(device);
			} finally {
				mgr.close();
			}
			return device;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}
	
	public Device removeAction(User user, @Named("id") String id, @Named("action") String action) throws OAuthRequestException {
		if (user != null) {
			EntityManager mgr = getEntityManager();
			Device device = null;
			try {
				device = mgr.find(Device.class, id);
				device.removeAction(action);
				mgr.persist(device);
			} finally {
				mgr.close();
			}
			return device;
		} else {
			throw new OAuthRequestException("Invalid user.");
		}
	}
	
	public Device update(User user, Device device) throws OAuthRequestException {
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
	
	public Device remove(User user, @Named("id") String id) throws OAuthRequestException {
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
