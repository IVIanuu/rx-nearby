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

/**
 * State
 */
public final class State {

    private boolean initialized;
    private boolean host;
    private boolean advertising;
    private boolean discovering;

    State(boolean initialized, boolean host, boolean advertising, boolean discovering) {
        this.initialized = initialized;
        this.host = host;
        this.advertising = advertising;
        this.discovering = discovering;
    }

    /**
     * Returns whether this device is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Returns whether this device is host
     */
    public boolean isHost() {
        return host;
    }

    void setHost(boolean host) {
        this.host = host;
    }

    /**
     * Returns whether this device is advertising
     */
    public boolean isAdvertising() {
        return advertising;
    }

    void setAdvertising(boolean advertising) {
        this.advertising = advertising;
    }

    /**
     * Returns whether this device is discovering
     */
    public boolean isDiscovering() {
        return discovering;
    }

    void setDiscovering(boolean discovering) {
        this.discovering = discovering;
    }
}
