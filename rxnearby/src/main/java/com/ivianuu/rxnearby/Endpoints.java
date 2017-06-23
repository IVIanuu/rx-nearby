/*
 * Copyright 2017 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.rxnearby;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Manuel Wrage (IVIanuu)
 */
public class Endpoints {

    private List<Endpoint> endpoints = new ArrayList<>();

    /**
     * Adds or updates the endpoint
     */
    void addEndpoint(@NonNull Endpoint endpoint) {
        if (containsEndpoint(endpoint)) {
            setEndpointStatus(endpoint.getEndpointId(), endpoint.getStatus());
        } else {
            endpoints.add(endpoint);
        }
    }

    /**
     * Removes the endpoint
     */
    void removeEndpoint(@NonNull Endpoint endpoint) {
        endpoints.remove(endpoint);
    }

    /**
     * Removes the endpoint with the id
     */
    void removeEndpointById(@NonNull String id) {
        int position = getEndpointPosition(id);
        if (position != -1) {
            endpoints.remove(position);
        }
    }

    /**
     * Removes all endpoints with this status
     */
    void removeEndpointByStatus(@Endpoint.Status int status) {
        for (int i = endpoints.size() - 1; i >= 0; i--) {
            if (endpoints.get(i).getStatus() == status) {
                endpoints.remove(i);
            }
        }
    }

    /**
     * Returns the endpoint with this id
     */
    @Nullable
    public Endpoint getEndpointById(@NonNull String id) {
        for (Endpoint endpoint : endpoints) {
            if (endpoint.getEndpointId().equals(id)) {
                return endpoint;
            }
        }

        return null;
    }

    /**
     * Returns the endpoint at the position
     */
    @NonNull
    public Endpoint getEndpointByPosition(int pos) {
        if (pos < 0 || pos >= endpoints.size()) {
            throw new IllegalArgumentException("" + pos + " is out of range");
        }

        return endpoints.get(pos);
    }

    /**
     * Returns the endpoint count
     */
    public int getEndpointCount() {
        return endpoints.size();
    }

    /**
     * Returns the position of the endpoint
     */
    public int getEndpointPosition(@NonNull String id) {
        for (int i = 0; i < getEndpointCount(); i++) {
            if (endpoints.get(i).getEndpointId().equals(id)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns all endpoints
     */
    @NonNull
    public List<Endpoint> getEndpoints() {
        return new ArrayList<>(endpoints); // return copy
    }

    /**
     * Returns all endpoints with the provided status
     */
    @NonNull
    public List<Endpoint> getEndpointsByStatus(@Endpoint.Status int status) {
        List<Endpoint> statusEndpoints = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.getStatus() == status) {
                statusEndpoints.add(endpoint);
            }
        }

        return statusEndpoints;
    }

    /**
     * Sets the status of the endpoint
     */
    void setEndpointStatus(@NonNull String id, @Endpoint.Status int status) {
        Endpoint endpoint = getEndpointById(id);
        if (endpoint != null) {
            endpoint.setStatus(status);
        }
    }

    /**
     * Clears all endpoints
     */
    void clear() {
        endpoints.clear();
    }

    /**
     * Checks if the endpoint is in the endpoints list
     */
    public boolean containsEndpoint(@NonNull Endpoint endpoint) {
        for (Endpoint e : endpoints) {
            if (e.getEndpointId().equals(endpoint.getEndpointId())) {
                return true;
            }
        }

        return false;
    }
}
