/*
 * ao-credit-cards-api - Credit card processing API supporting multiple payment gateways.
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2016, 2019, 2020  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-credit-cards-api.
 *
 * ao-credit-cards-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-credit-cards-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-credit-cards-api.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.creditcards;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Creates instances of <code>MerchantServicesProvider</code>s based on the provided
 * parameters.
 * Will only create once instance of each unique set of parameters.
 *
 * @author  AO Industries, Inc.
 */
public class MerchantServicesProviderFactory {

	private static class ProviderKey {

		final private String providerId;
		final private String className;
		final private String param1;
		final private String param2;
		final private String param3;
		final private String param4;

		private ProviderKey(
			String providerId,
			String className,
			String param1,
			String param2,
			String param3,
			String param4
		) {
			this.providerId = providerId;
			this.className = className;
			this.param1 = param1;
			this.param2 = param2;
			this.param3 = param3;
			this.param4 = param4;
		}

		@Override
		public int hashCode() {
			return
				providerId.hashCode()
				+ className.hashCode()*7
				+ (param1==null ? 0 : (param1.hashCode()*17))
				+ (param2==null ? 0 : (param1.hashCode()*37))
				+ (param3==null ? 0 : (param1.hashCode()*103))
				+ (param4==null ? 0 : (param1.hashCode()*149))
			;
		}

		@Override
		public boolean equals(Object O) {
			if(O==null) return false;
			if(!(O instanceof ProviderKey)) return false;
			ProviderKey other = (ProviderKey)O;
			return
				providerId.equals(other.providerId)
				&& className.equals(other.className)
				&& Objects.equals(param1, other.param1)
				&& Objects.equals(param2, other.param2)
				&& Objects.equals(param3, other.param3)
				&& Objects.equals(param4, other.param4)
			;
		}
	}

	final private static Map<ProviderKey,MerchantServicesProvider> providers = new HashMap<>();

	/**
	 * Gets the provider for the given parameters.<br>
	 * <br>
	 * Only one instance of each unique providerId, classname and all parameters will be created.<br>
	 */
	public static MerchantServicesProvider getMerchantServicesProvider(
		String providerId,
		String className,
		String param1,
		String param2,
		String param3,
		String param4
	) throws ReflectiveOperationException {
		// The key in the map
		ProviderKey processorKey = new ProviderKey(
			providerId,
			className,
			param1,
			param2,
			param3,
			param4
		);

		// Now synchronize access to processors
		synchronized(providers) {
			// Look for existing instance
			MerchantServicesProvider provider = providers.get(processorKey);
			if(provider==null) {
				// Instantiate through reflection
				Class<? extends MerchantServicesProvider> clazz = Class.forName(className).asSubclass(MerchantServicesProvider.class);

				// Try the providerId + 4-parameter constructor
				try {
					Constructor<? extends MerchantServicesProvider> constructor = clazz.getConstructor(String.class, String.class, String.class, String.class, String.class);
					provider = constructor.newInstance(providerId, param1, param2, param3, param4);
				} catch(InvocationTargetException e) {
					throw e;
				} catch(ReflectiveOperationException ignored) {
					// Fall through to next param set
				}

				if(provider==null) {
					// Try the providerId + 3-parameter constructor
					try {
						Constructor<? extends MerchantServicesProvider> constructor = clazz.getConstructor(String.class, String.class, String.class, String.class);
						provider = constructor.newInstance(providerId, param1, param2, param3);
					} catch(InvocationTargetException e) {
						throw e;
					} catch(ReflectiveOperationException ignored) {
						// Fall through to next param set
					}
				}

				if(provider==null) {
					// Try the providerId + 2-parameter constructor
					try {
						Constructor<? extends MerchantServicesProvider> constructor = clazz.getConstructor(String.class, String.class, String.class);
						provider = constructor.newInstance(providerId, param1, param2);
					} catch(InvocationTargetException e) {
						throw e;
					} catch(ReflectiveOperationException ignored) {
						// Fall through to next param set
					}
				}

				if(provider==null) {
					// Try the providerId + 1-parameter constructor
					try {
						Constructor<? extends MerchantServicesProvider> constructor = clazz.getConstructor(String.class, String.class);
						provider = constructor.newInstance(providerId, param1);
					} catch(InvocationTargetException e) {
						throw e;
					} catch(ReflectiveOperationException ignored) {
						// Fall through to next param set
					}
				}

				if(provider==null) {
					// Try the providerId constructor, if fails allow exception to go out of this method
					Constructor<? extends MerchantServicesProvider> constructor = clazz.getConstructor(String.class);
					provider = constructor.newInstance(providerId);
				}

				// Create and add to cache
				providers.put(processorKey, provider);
			}
			return provider;
		}
	}

	private MerchantServicesProviderFactory() {
		// Make no instances
	}
}
