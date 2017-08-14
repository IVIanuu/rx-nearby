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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.ivianuu.rxnearby.Endpoint.Status.STATUS_CONNECTED;
import static com.ivianuu.rxnearby.Endpoint.Status.STATUS_DISCONNECTED;
import static com.ivianuu.rxnearby.Endpoint.Status.STATUS_FOUND;
import static com.ivianuu.rxnearby.Endpoint.Status.STATUS_LOST;
import static com.ivianuu.rxnearby.Endpoint.Status.STATUS_REQUESTED_BY_ME;
import static com.ivianuu.rxnearby.Endpoint.Status.STATUS_REQUESTING_ME;
import static com.ivianuu.rxnearby.Endpoint.Status.STATUS_REQUEST_DENIED_BY_ME;
import static com.ivianuu.rxnearby.Endpoint.Status.STATUS_REQUEST_DENIED_BY_THEM;

/**
 * Endpoint
 */
public class Endpoint {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_CONNECTED, STATUS_REQUESTED_BY_ME, STATUS_REQUESTING_ME,
            STATUS_REQUEST_DENIED_BY_ME, STATUS_REQUEST_DENIED_BY_THEM, STATUS_FOUND,
            STATUS_DISCONNECTED, STATUS_LOST})
    public @interface Status {
        int STATUS_CONNECTED = 0;
        int STATUS_REQUESTED_BY_ME = 1;
        int STATUS_REQUESTING_ME = 2;
        int STATUS_REQUEST_DENIED_BY_ME = 3;
        int STATUS_REQUEST_DENIED_BY_THEM = 4;
        int STATUS_FOUND = 5;
        int STATUS_DISCONNECTED = 6;
        int STATUS_LOST = 7;
    }

    @Status
    private int status;
    
    private String endpointId;
    private String endpointName;

    Endpoint(@Status int status, @NonNull String endpointId, @NonNull String endpointName) {
        this.status = status;
        this.endpointId = endpointId;
        this.endpointName = endpointName;
    }

    /**
     * Returns the status of this endpoint
     */
    @Status
    public int getStatus() {
        return status;
    }

    /**
     * Sets the status of this endpoint
     */
    void setStatus(@Status int status) {
        this.status = status;
    }

    /**
     * Returns the endpoint id
     */
    @NonNull
    public String getEndpointId() {
        return endpointId;
    }

    /**
     * Returns the endpoint name
     */
    @NonNull
    public String getEndpointName() {
        return endpointName;
    }
}
