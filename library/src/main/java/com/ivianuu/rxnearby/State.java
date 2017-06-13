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
 * @author Manuel Wrage (IVIanuu)
 */
public class State {

    private boolean initialized;
    private boolean host;
    private boolean advertising;
    private boolean discovering;

    public State(boolean initialized, boolean host, boolean advertising, boolean discovering) {
        this.initialized = initialized;
        this.host = host;
        this.advertising = advertising;
        this.discovering = discovering;
    }

    public boolean isInitialized() {
        return initialized;
    }

    void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isHost() {
        return host;
    }

    void setHost(boolean host) {
        this.host = host;
    }

    public boolean isAdvertising() {
        return advertising;
    }

    void setAdvertising(boolean advertising) {
        this.advertising = advertising;
    }

    public boolean isDiscovering() {
        return discovering;
    }

    void setDiscovering(boolean discovering) {
        this.discovering = discovering;
    }
}
