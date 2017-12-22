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

package com.ivianuu.rxnearby

import android.support.annotation.IntDef
import com.google.android.gms.nearby.connection.Payload
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Represents a connection event
 */
data class ConnectionEvent(val type: EventType,
                           val endpoint: Endpoint) {

    enum class EventType {
        REQUEST_ACCEPTED, REQUEST_REJECTED
    }
}

/**
 * Endpoint
 */
data class Endpoint(val status: Status,
                    val id: String,
                    val name: String) {

    enum class Status {
        STATUS_CONNECTED,
        STATUS_REQUESTED_BY_ME,
        STATUS_REQUESTING_ME,
        STATUS_REQUEST_DENIED_BY_ME,
        STATUS_REQUEST_DENIED_BY_THEM,
        STATUS_FOUND,
        STATUS_DISCONNECTED,
        STATUS_LOST
    }
}

/**
 * Message class
 */
data class Message(
        val endpoint: Endpoint,
        val payload: Payload,
        val timestamp: Long)

/**
 * State
 */
data class State(val initialized: Boolean,
                 val host: Boolean,
                 val advertising: Boolean,
                 val discovering: Boolean)