/*
  Copyright (c) 2012, 2015, Credit Suisse (Anatole Tresch), Werner Keil and others by the @author tag.

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy of
  the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations under
  the License.
 */
package org.javamoney.moneta.spi.loader.urlconnection;

import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

class LoadDataService {

	private static final Logger LOG = Logger.getLogger(LoadDataService.class.getName());

	private final URLConnectionLoaderListener listener;

   LoadDataService(URLConnectionLoaderListener listener) {
		this.listener = listener;
	}

	public boolean execute(String resourceId,
			Map<String, LoadableURLResource> resources) {

		LoadableURLResource load = resources.get(resourceId);
		if (Objects.nonNull(load)) {
			try {
				if (load.load()) {
					LOG.log(Level.FINE, "Read data from: " + load.getRemoteResources());
					listener.trigger(resourceId, load);
					LOG.log(Level.FINE, "New data successfully loaded from: " + load.getRemoteResources());
					return true;
				}
			} catch (Exception e) {
				LOG.log(Level.WARNING, "Failed to read/load resource (checking fallback): " + resourceId,
						e);
			}
			try {
				if (load.loadFallback()) {
					LOG.log(Level.WARNING, "Read fallback data from: " + load.getFallbackResource());
					listener.trigger(resourceId, load);
					LOG.log(Level.WARNING, "Loaded fallback data from: " + load.getFallbackResource());
					return true;
				}
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Failed to read/load fallback resource: " + resourceId,
						e);
			}
		} else {
			throw new IllegalArgumentException("No such resource: "
					+ resourceId);
		}
		return false;
	}

	@Override
	public String toString() {
		return LoadDataService.class.getName() + '{' + " listener: " + listener + '}';
	}

}
