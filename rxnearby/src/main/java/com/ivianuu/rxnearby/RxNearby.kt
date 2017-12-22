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

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.support.annotation.CheckResult
import android.util.Log

import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.ArrayList

/**
 * Rx nearby
 */
class RxNearby(context: Context, private val serviceId: String = context.packageName) {

    private val googleApiClient: GoogleApiClient

    private val endpointsSubject = BehaviorSubject.createDefault(ArrayList<Endpoint>())
    private val messages = PublishSubject.create<Message>()
    private val stateSubject = BehaviorSubject.createDefault(
            State(false, false, false, false))

    private fun log(message: String) {
        Log.d(TAG, message)
    }

    init {
        googleApiClient = GoogleApiClient.Builder(context)
                .addApi(Nearby.CONNECTIONS_API)
                .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                    override fun onConnected(bundle: Bundle?) {
                        log("google api client connected")
                        state.setInitialized(true)
                        stateSubject.onNext(state)
                    }

                    override fun onConnectionSuspended(i: Int) {
                        log("google api client connection suspended")
                        googleApiClient.reconnect()
                    }
                })
                .build()
        googleApiClient.connect()

        log("init")
    }

    // STATE

    /**
     * Emits on every state events
     */
    @CheckResult
    fun state() = stateSubject.hide()

    // ENDPOINTS

    /**
     * Emits when ever endpoints changes
     */
    @CheckResult
    fun endpoints() = endpointsSubject.hide()

    // CLEANING

    /**
     * Cleaning
     */
    fun release() {
        log("release")
        stopAdvertising()
        stopDiscovery()
        stopAllEndpoints()
        if (googleApiClient.isConnected) {
            googleApiClient.disconnect()
        }
        endpoints.clear()
        endpointsSubject.onNext(endpoints)
    }

    /**
     * Starts advertising
     */
    @CheckResult
    @JvmOverloads fun startAdvertising(thisDeviceName: String? = null,
                                       strategy: Strategy? = null): Observable<Endpoint> {
        return Observable.create({ e ->
            advertise(e, thisDeviceName, strategy)
        } as ObservableOnSubscribe<Endpoint>).subscribeOn(Schedulers.io())
    }

    private fun advertise(e: ObservableEmitter<Endpoint>, thisDeviceName: String?, strategy: Strategy?) {
        var strategy = strategy
        if (strategy == null) {
            // default strategy
            strategy = Strategy.P2P_CLUSTER
        }

        // connect api client if not done yet
        if (!googleApiClient.isConnected) {
            googleApiClient.blockingConnect()
        }

        // stop all endpoints
        stopAdvertising()
        stopDiscovery()
        stopAllEndpoints()

        log("start advertising")
        Nearby.Connections.startAdvertising(googleApiClient, thisDeviceName, serviceId,
                object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(s: String, connectionInfo: ConnectionInfo) {
                        val endpoint = Endpoint(
                                Endpoint.Status.STATUS_REQUESTING_ME, s,
                                connectionInfo.endpointName)
                        endpoints.addEndpoint(endpoint)
                        endpointsSubject.onNext(endpoints)
                        log("connection initiated from" + endpoint.name)
                        if (!e.isDisposed) {
                            e.onNext(endpoint)
                        }
                    }

                    override fun onConnectionResult(s: String, connectionResolution: ConnectionResolution) {
                        val endpoint = endpoints.getEndpointById(s)
                        if (endpoint != null) {
                            if (connectionResolution.status.isSuccess) {
                                log("successfully connected " + endpoint.name)
                                endpoint.setStatus(Endpoint.Status.STATUS_CONNECTED)
                            } else {
                                log("failed to connect to " + endpoint.name)
                                if (endpoint.status === Endpoint.Status.STATUS_REQUESTED_BY_ME) {
                                    endpoint.setStatus(
                                            Endpoint.Status.STATUS_REQUEST_DENIED_BY_THEM)
                                } else {
                                    endpoint.setStatus(Endpoint.Status.STATUS_REQUEST_DENIED_BY_ME)
                                }
                            }

                            endpointsSubject.onNext(endpoints)

                            if (!e.isDisposed) {
                                e.onNext(endpoint)
                            }
                        }
                    }

                    override fun onDisconnected(s: String) {
                        val endpoint = endpoints.getEndpointById(s)
                        if (endpoint != null) {
                            log("disconnected from " + endpoint.name)
                            endpoints.removeEndpoint(endpoint)
                            endpointsSubject.onNext(endpoints)
                            endpoint.setStatus(Endpoint.Status.STATUS_DISCONNECTED)
                            e.onNext(endpoint)
                        }
                    }
                }, AdvertisingOptions(strategy))
                .setResultCallback { startAdvertisingResult ->
                    state.setAdvertising(startAdvertisingResult.status.isSuccess)
                    state.setHost(state.isAdvertising())
                    stateSubject.onNext(state)

                    log("start advertising result " + startAdvertisingResult.status.isSuccess)
                    if (!e.isDisposed && !startAdvertisingResult.status.isSuccess) {
                        val statusCode = startAdvertisingResult.status.statusCode
                        e.onError(Throwable("status " + statusCode))
                    }
                }
    }

    /**
     * Stops advertising
     */
    fun stopAdvertising() {
        log("stopping advertising")
        Nearby.Connections.stopAdvertising(googleApiClient)
        endpoints.removeEndpointByStatus(Endpoint.Status.STATUS_CONNECTED)
        endpointsSubject.onNext(endpoints)
        state.setAdvertising(false)
        state.setHost(false)
        stateSubject.onNext(state)
    }

    /**
     * Starts discovery
     */
    @CheckResult
    @JvmOverloads fun startDiscovery(strategy: Strategy? = null): Observable<Endpoint> {
        return Observable.create({ e -> discover(e, strategy) } as ObservableOnSubscribe<Endpoint>)
                .subscribeOn(Schedulers.io())
    }

    private fun discover(e: ObservableEmitter<Endpoint>, strategy: Strategy?) {
        var strategy = strategy
        if (strategy == null) {
            // set default strategy
            strategy = Strategy.P2P_CLUSTER
        }

        // connect api client if not done yet
        if (!googleApiClient.isConnected) {
            googleApiClient.blockingConnect()
        }

        // clean up
        stopAdvertising()
        stopDiscovery()
        stopAllEndpoints()

        log("start discovery")
        Nearby.Connections.startDiscovery(googleApiClient, serviceId,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(s: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
                        val endpoint = Endpoint(Endpoint.Status.STATUS_FOUND, s,
                                discoveredEndpointInfo.endpointName)
                        endpoints.addEndpoint(endpoint)
                        endpointsSubject.onNext(endpoints)
                        log("endpoint found " + endpoint.name)
                        if (!e.isDisposed) {
                            e.onNext(endpoint)
                        }
                    }

                    override fun onEndpointLost(s: String) {
                        val endpoint = endpoints.getEndpointById(s)
                        if (endpoint != null) {
                            log("endpoint lost " + endpoint.name)
                            endpoints.removeEndpoint(endpoint)
                            endpointsSubject.onNext(endpoints)
                            endpoint.setStatus(Endpoint.Status.STATUS_LOST)
                            if (!e.isDisposed) {
                                e.onNext(endpoint)
                            }
                        }
                    }
                }, DiscoveryOptions(strategy)).setResultCallback { status ->
            state.setDiscovering(status.isSuccess)
            stateSubject.onNext(state)
            log("start discovery result " + status.isSuccess)
            if (!e.isDisposed && !status.isSuccess) {
                val statusCode = status.statusCode
                e.onError(Throwable("status " + statusCode))
            }
        }
    }

    /**
     * Stops discovery
     */
    fun stopDiscovery() {
        log("stop discovery")
        Nearby.Connections.stopDiscovery(googleApiClient)
        endpoints.removeEndpointByStatus(Endpoint.Status.STATUS_FOUND)
        endpointsSubject.onNext(endpoints)
        state.setDiscovering(false)
        stateSubject.onNext(state)
    }

    // CONNECTIONS

    /**
     * Accepts the connection to the specified endpoint
     */
    fun acceptConnection(endpoint: Endpoint) {
        log("accepting connection to " + endpoint.name)
        Nearby.Connections.acceptConnection(googleApiClient, endpoint.id,
                object : PayloadCallback() {
                    override fun onPayloadReceived(s: String, payload: Payload) {
                        val endpoint = endpoints.getEndpointById(s)
                        if (endpoint != null) {
                            log("message received from " + endpoint.name)
                            messages.onNext(Message(endpoint, payload))
                        }
                    }

                    override fun onPayloadTransferUpdate(s: String, payloadTransferUpdate: PayloadTransferUpdate) {
                        // ignore
                    }
                })
    }

    /**
     * Rejects the connection to the specified endpoint
     */
    fun rejectConnection(endpoint: Endpoint) {
        log("reject connection to " + endpoint.name)
        endpoint.setStatus(Endpoint.Status.STATUS_REQUEST_DENIED_BY_ME)
        endpointsSubject.onNext(endpoints)
        Nearby.Connections.rejectConnection(googleApiClient, endpoint.id)
    }

    /**
     * Requests the connection to the specified endpoint
     */
    @CheckResult
    @JvmOverloads
    fun requestConnection(endpoint: Endpoint, thisDeviceName: String? = null): Single<ConnectionEvent> {
        return Single.create({ e ->
            makeConnectionRequest(e, endpoint, thisDeviceName)
        } as SingleOnSubscribe<ConnectionEvent>)
                .subscribeOn(Schedulers.io())
    }

    private fun makeConnectionRequest(e: SingleEmitter<ConnectionEvent>, endpoint: Endpoint, thisDeviceName: String?) {
        log("requesting connection to " + endpoint.name)
        Nearby.Connections.requestConnection(
                googleApiClient, thisDeviceName, endpoint.id,
                object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(s: String, connectionInfo: ConnectionInfo) {
                        val initEndpoint = Endpoint(Endpoint.Status.STATUS_REQUEST_DENIED_BY_ME, s,
                                connectionInfo.endpointName)
                        log("auto accepting our initiated connection to " + initEndpoint.name)
                        endpoints.addEndpoint(initEndpoint)
                        endpointsSubject.onNext(endpoints)
                        acceptConnection(
                                initEndpoint) // auto accept -> were requesting the connection
                    }

                    override fun onConnectionResult(s: String, connectionResolution: ConnectionResolution) {
                        val resultEndpoint = endpoints.getEndpointById(s)
                        if (resultEndpoint != null) {
                            if (connectionResolution.status.isSuccess) {
                                resultEndpoint.setStatus(Endpoint.Status.STATUS_CONNECTED)
                                endpointsSubject.onNext(endpoints)
                                log("connected to " + resultEndpoint.name)
                            } else {
                                endpoints.removeEndpoint(resultEndpoint)
                                endpointsSubject.onNext(endpoints)
                            }
                        }
                    }

                    override fun onDisconnected(s: String) {
                        log("disconnected from host")
                        endpoints.removeEndpointById(s)
                        endpointsSubject.onNext(endpoints)
                    }
                })
                .setResultCallback { status ->
                    if (e.isDisposed) {
                        // the emitter is not longer interested
                        return@Nearby.Connections.requestConnection(
                                googleApiClient, thisDeviceName, endpoint.id,
                                new ConnectionLifecycleCallback () {
                                    @Override
                                    public void onConnectionInitiated(String s,
                                            ConnectionInfo connectionInfo) {
                                        Endpoint initEndpoint = new Endpoint(
                                                Endpoint.Status.STATUS_REQUEST_DENIED_BY_ME, s,
                                                connectionInfo.name);
                                        log("auto accepting our initiated connection to " + initEndpoint.name);
                                        endpoints.addEndpoint(initEndpoint);
                                        endpointsSubject.onNext(endpoints);
                                        acceptConnection(
                                                initEndpoint); // auto accept -> were requesting the connection
                                    }

                                    @Override
                                    public void onConnectionResult(String s,
                                            ConnectionResolution connectionResolution) {
                                        Endpoint resultEndpoint = endpoints . getEndpointById s;
                                        if (resultEndpoint != null) {
                                            if (connectionResolution.getStatus().isSuccess()) {
                                                resultEndpoint.setStatus(
                                                        Endpoint.Status.STATUS_CONNECTED);
                                                endpointsSubject.onNext(endpoints);
                                                log("connected to " + resultEndpoint.name);
                                            } else {
                                                endpoints.removeEndpoint(resultEndpoint);
                                                endpointsSubject.onNext(endpoints);
                                            }
                                        }
                                    }

                                    @Override
                                    public void onDisconnected(String s) {
                                        log("disconnected from host");
                                        endpoints.removeEndpointById(s);
                                        endpointsSubject.onNext(endpoints);
                                    }
                                })
                                .setResultCallback
                    }
                    val event: ConnectionEvent

                    if (status.isSuccess) {
                        log("successfully connected to " + endpoint.name)
                        event = ConnectionEvent(ConnectionEvent.EventType.REQUEST_ACCEPTED,
                                endpoint)
                    } else {
                        log("failed to connect to " + endpoint.name + " " + status.statusCode)
                        event = ConnectionEvent(ConnectionEvent.EventType.REQUEST_REJECTED,
                                endpoint)
                    }

                    e.onSuccess(event)
                }
    }

    /**
     * Disconnects from the specified endpoint
     */
    fun disconnectFromEndpoint(endpoint: Endpoint) {
        log("disconnecting from endpoint " + endpoint.name)
        Nearby.Connections.disconnectFromEndpoint(googleApiClient, endpoint.id)
        val newList = getEndpoints()
        newList.remove(endpoint)
        endpointsSubject.onNext(newList)
    }

    /**
     * Stops discovering and advertising and clears all values
     */
    fun stopAllEndpoints() {
        log("stop all endpoints")
        Nearby.Connections.stopAllEndpoints(googleApiClient)
        endpointsSubject.onNext(ArrayList())
    }

    // MESSAGES

    /**
     * Emits when a new message was received
     */
    @CheckResult
    fun messages() = messages.hide()

    /**
     * Sends the payload to all connected devices
     */
    fun sendMessageToAllConnectedEndpoints(payload: Payload) {
        log("sending message to all")
        getEndpoints().withStatus(Endpoint.Status.STATUS_CONNECTED)
                .forEach { sendMessage(Message(it, payload, System.currentTimeMillis())) }
    }

    /**
     * Sends the message
     */
    fun sendMessage(message: Message) {
        log("sending message to " + message.endpoint.name)
        Nearby.Connections.sendPayload(googleApiClient, message.endpoint.id,
                message.payload)
    }

    private fun getEndpoints() = endpointsSubject.value

    private fun getState() = stateSubject.value

    private fun ArrayList<Endpoint>.addOrSet(endpoint: Endpoint) {
        val index = indexById(endpoint.id)
        if (index != -1) {
            set(index, endpoint)
        } else {
            add(endpoint)
        }
    }

    private fun ArrayList<Endpoint>.getById(id: String) = firstOrNull { it.id == id }

    private fun ArrayList<Endpoint>.indexById(id: String) = indexOfFirst { it.id == id }

    private fun ArrayList<Endpoint>.withStatus(status: Endpoint.Status) = filter { it.status == status }

    private fun ArrayList<Endpoint>.removeById(id: String) {
        val index = indexById(id)
        if (index != -1) {
            removeAt(index)
        }
    }

    private fun ArrayList<Endpoint>.removeWithStatus(status: Endpoint.Status) {
        val removed = filter { it.status == status }
        removed.forEach { remove(it) }
    }
}