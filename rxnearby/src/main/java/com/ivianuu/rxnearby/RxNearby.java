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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

import static com.ivianuu.rxnearby.Preconditions.checkNotNull;

/**
 * Rx nearby
 */
public final class RxNearby {

    private static final String TAG = "RxNearby";
    private void log(String message) { Log.d(TAG, message); }

    private final GoogleApiClient googleApiClient;
    private final String serviceId;

    private final BehaviorSubject<Endpoints> endpointsSubject = BehaviorSubject.create();
    private final PublishSubject<Message> messages = PublishSubject.create();
    private final BehaviorSubject<State> stateSubject = BehaviorSubject.create();

    private State state;
    private Endpoints endpoints;

    private RxNearby(Context context, String serviceId) {
        this.serviceId = serviceId;

        state = new State(false, false, false, false);
        stateSubject.onNext(state);
        endpoints = new Endpoints();
        endpointsSubject.onNext(endpoints);

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Nearby.CONNECTIONS_API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        log("google api client connected");
                        state.setInitialized(true);
                        stateSubject.onNext(state);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        log("google api client connection suspended");
                        googleApiClient.reconnect();
                    }
                })
                .build();
        googleApiClient.connect();

        log("init");
    }

    /**
     * Returns a new RxNearby instance
     * This will use the package name as the service id
     */
    @NonNull
    public static RxNearby create(@NonNull Context context) {
        return create(context, context.getPackageName());
    }

    /**
     * Returns a new RxNearby instance
     */
    @NonNull
    public static RxNearby create(@NonNull Context context, @NonNull String serviceId) {
        checkNotNull(context, "context == null");
        checkNotNull(serviceId, "serviceId == null");
        return new RxNearby(context, serviceId);
    }

    // STATE

    /**
     * Emits on every state events
     */
    @CheckResult @NonNull
    public Observable<State> state() {
        return stateSubject;
    }

    /**
     * Returns the current state
     */
    public State getState() {
        return state;
    }

    /**
     * Returns if this device is the host
     */
    public boolean isHost() {
        return state.isHost();
    }

    // ENDPOINTS

    /**
     * Emits when ever endpoints changes
     */
    @CheckResult @NonNull
    public Observable<Endpoints> endpoints() {
        return endpointsSubject;
    }

    /**
     * Returns the endpoints
     */
    public Endpoints getEndpoints() {
        return endpoints;
    }

    // CLEANING

    /**
     * Cleaning
     */
    public void release() {
        log("release");
        stopAdvertising();
        stopDiscovery();
        stopAllEndpoints();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        endpoints.clear();
        endpointsSubject.onNext(endpoints);
    }

    // ADVERTISING

    /**
     * Starts advertising
     */
    @CheckResult @NonNull
    public Observable<Endpoint> startAdvertising() {
        return startAdvertising(null, null);
    }

    /**
     * Starts advertising
     */
    @CheckResult @NonNull
    public Observable<Endpoint> startAdvertising(@NonNull String thisDeviceName) {
        return startAdvertising(thisDeviceName, null);
    }

    /**
     * Starts advertising
     */
    @CheckResult @NonNull
    public Observable<Endpoint> startAdvertising(@Nullable String thisDeviceName,
                                                 @Nullable Strategy strategy) {
        return Observable.create((ObservableOnSubscribe<Endpoint>) e ->
                advertise(e, thisDeviceName, strategy)).subscribeOn(Schedulers.io());
    }

    private void advertise(final ObservableEmitter<Endpoint> e, String thisDeviceName, Strategy strategy) {
        if (strategy == null) {
            // default strategy
            strategy = Strategy.P2P_CLUSTER;
        }

        // connect api client if not done yet
        if (!googleApiClient.isConnected()) {
            googleApiClient.blockingConnect();
        }

        // stop all endpoints
        stopAdvertising();
        stopDiscovery();
        stopAllEndpoints();

        log("start advertising");
        Nearby.Connections.startAdvertising(googleApiClient, thisDeviceName, serviceId,
                new ConnectionLifecycleCallback() {
                    @Override
                    public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
                        Endpoint endpoint = new Endpoint(
                                Endpoint.Status.STATUS_REQUESTING_ME, s, connectionInfo.getEndpointName());
                        endpoints.addEndpoint(endpoint);
                        endpointsSubject.onNext(endpoints);
                        log("connection initiated from" + endpoint.getEndpointName());
                        if (!e.isDisposed()) {
                            e.onNext(endpoint);
                        }
                    }

                    @Override
                    public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
                        Endpoint endpoint = endpoints.getEndpointById(s);
                        if (endpoint != null) {
                            if (connectionResolution.getStatus().isSuccess()) {
                                log("successfully connected " + endpoint.getEndpointName());
                                endpoint.setStatus(Endpoint.Status.STATUS_CONNECTED);
                            } else {
                                log("failed to connect to " + endpoint.getEndpointName());
                                if (endpoint.getStatus() == Endpoint.Status.STATUS_REQUESTED_BY_ME) {
                                    endpoint.setStatus(Endpoint.Status.STATUS_REQUEST_DENIED_BY_THEM);
                                } else {
                                    endpoint.setStatus(Endpoint.Status.STATUS_REQUEST_DENIED_BY_ME);
                                }
                            }

                            endpointsSubject.onNext(endpoints);

                            if (!e.isDisposed()) {
                                e.onNext(endpoint);
                            }
                        }
                    }

                    @Override
                    public void onDisconnected(String s) {
                        Endpoint endpoint = endpoints.getEndpointById(s);
                        if (endpoint != null) {
                            log("disconnected from " + endpoint.getEndpointName());
                            endpoints.removeEndpoint(endpoint);
                            endpointsSubject.onNext(endpoints);
                            endpoint.setStatus(Endpoint.Status.STATUS_DISCONNECTED);
                            e.onNext(endpoint);
                        }
                    }
                }, new AdvertisingOptions(strategy))
        .setResultCallback(startAdvertisingResult -> {
            state.setAdvertising(startAdvertisingResult.getStatus().isSuccess());
            state.setHost(state.isAdvertising());
            stateSubject.onNext(state);

            log("start advertising result " + startAdvertisingResult.getStatus().isSuccess());
            if (!e.isDisposed() && !startAdvertisingResult.getStatus().isSuccess()) {
                int statusCode = startAdvertisingResult.getStatus().getStatusCode();
                e.onError(new Throwable("status " + statusCode));
            }
        });
    }

    /**
     * Stops advertising
     */
    public void stopAdvertising() {
        log("stopping advertising");
        Nearby.Connections.stopAdvertising(googleApiClient);
        endpoints.removeEndpointByStatus(Endpoint.Status.STATUS_CONNECTED);
        endpointsSubject.onNext(endpoints);
        state.setAdvertising(false);
        state.setHost(false);
        stateSubject.onNext(state);
    }

    /**
     * Returns if were currently advertising
     */
    public boolean isAdvertising() {
        return state.isAdvertising();
    }

    // DISCOVERY

    /**
     * Starts discovery
     */
    @CheckResult @NonNull
    public Observable<Endpoint> startDiscovery() {
        return startDiscovery(null);
    }

    /**
     * Starts discovery
     */
    @CheckResult @NonNull
    public Observable<Endpoint> startDiscovery(final Strategy strategy) {
        return Observable.create((ObservableOnSubscribe<Endpoint>) e -> discover(e, strategy))
                .subscribeOn(Schedulers.io());
    }

    private void discover(final ObservableEmitter<Endpoint> e, Strategy strategy) {
        if (strategy == null) {
            // set default strategy
            strategy = Strategy.P2P_CLUSTER;
        }

        // connect api client if not done yet
        if (!googleApiClient.isConnected()) {
            googleApiClient.blockingConnect();
        }

        // clean up
        stopAdvertising();
        stopDiscovery();
        stopAllEndpoints();

        log("start discovery");
        Nearby.Connections.startDiscovery(googleApiClient, serviceId, new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {
                Endpoint endpoint = new Endpoint(Endpoint.Status.STATUS_FOUND, s, discoveredEndpointInfo.getEndpointName());
                endpoints.addEndpoint(endpoint);
                endpointsSubject.onNext(endpoints);
                log("endpoint found " + endpoint.getEndpointName());
                if (!e.isDisposed()) {
                    e.onNext(endpoint);
                }
            }

            @Override
            public void onEndpointLost(String s) {
                Endpoint endpoint = endpoints.getEndpointById(s);
                if (endpoint != null) {
                    log("endpoint lost " + endpoint.getEndpointName());
                    endpoints.removeEndpoint(endpoint);
                    endpointsSubject.onNext(endpoints);
                    endpoint.setStatus(Endpoint.Status.STATUS_LOST);
                    if (!e.isDisposed()) {
                        e.onNext(endpoint);
                    }
                }
            }
        }, new DiscoveryOptions(strategy)).setResultCallback(status -> {
            state.setDiscovering(status.isSuccess());
            stateSubject.onNext(state);
            log("start discovery result " + status.isSuccess());
            if (!e.isDisposed() && !status.isSuccess()) {
                int statusCode = status.getStatusCode();
                e.onError(new Throwable("status " + statusCode));
            }
        });
    }

    /**
     * Stops discovery
     */
    public void stopDiscovery() {
        log("stop discovery");
        Nearby.Connections.stopDiscovery(googleApiClient);
        endpoints.removeEndpointByStatus(Endpoint.Status.STATUS_FOUND);
        endpointsSubject.onNext(endpoints);
        state.setDiscovering(false);
        stateSubject.onNext(state);
    }

    /**
     * Returns if we currently discovering
     */
    public boolean isDiscovering() {
        return state.isDiscovering();
    }

    // CONNECTIONS

    /**
     * Accepts the connection to the specified endpoint
     */
    public void acceptConnection(@NonNull Endpoint endpoint) {
        checkNotNull(endpoint, "endpoint == null");
        log("accepting connection to " + endpoint.getEndpointName());
        Nearby.Connections.acceptConnection(googleApiClient, endpoint.getEndpointId(), new PayloadCallback() {
            @Override
            public void onPayloadReceived(String s, Payload payload) {
                Endpoint endpoint = endpoints.getEndpointById(s);
                if (endpoint != null) {
                    log("message received from " + endpoint.getEndpointName());
                    messages.onNext(new Message(endpoint, payload));
                }
            }

            @Override
            public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
                // ignore
            }
        });
    }

    /**
     * Rejects the connection to the specified endpoint
     */
    public void rejectConnection(@NonNull Endpoint endpoint) {
        checkNotNull(endpoint, "endpoint == null");
        log("reject connection to " + endpoint.getEndpointName());
        endpoint.setStatus(Endpoint.Status.STATUS_REQUEST_DENIED_BY_ME);
        endpointsSubject.onNext(endpoints);
        Nearby.Connections.rejectConnection(googleApiClient, endpoint.getEndpointId());
    }

    /**
     * Requests the connection to the specified endpoint
     */
    @CheckResult @NonNull
    public Single<ConnectionEvent> requestConnection(@NonNull Endpoint endpoint) {
        return requestConnection(endpoint, null);
    }

    /**
     * Requests the connection to the specified endpoint
     */
    @CheckResult @NonNull
    public Single<ConnectionEvent> requestConnection(@NonNull final Endpoint endpoint, @Nullable final String thisDeviceName) {
        checkNotNull(endpoint, "endpoint == null");
        return Single.create((SingleOnSubscribe<ConnectionEvent>) e ->
                makeConnectionRequest(e, endpoint, thisDeviceName))
                .subscribeOn(Schedulers.io());
    }

    private void makeConnectionRequest(final SingleEmitter<ConnectionEvent> e, final Endpoint endpoint, String thisDeviceName) {
        log("requesting connection to " + endpoint.getEndpointName());
        Nearby.Connections.requestConnection(
                googleApiClient, thisDeviceName, endpoint.getEndpointId(), new ConnectionLifecycleCallback() {
                    @Override
                    public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
                        Endpoint initEndpoint = new Endpoint(Endpoint.Status.STATUS_REQUEST_DENIED_BY_ME, s, connectionInfo.getEndpointName());
                        log("auto accepting our initiated connection to " + initEndpoint.getEndpointName());
                        endpoints.addEndpoint(initEndpoint);
                        endpointsSubject.onNext(endpoints);
                        acceptConnection(initEndpoint); // auto accept -> were requesting the connection
                    }

                    @Override
                    public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
                        Endpoint resultEndpoint = endpoints.getEndpointById(s);
                        if (resultEndpoint != null) {
                            if (connectionResolution.getStatus().isSuccess()) {
                                resultEndpoint.setStatus(Endpoint.Status.STATUS_CONNECTED);
                                endpointsSubject.onNext(endpoints);
                                log("connected to " + resultEndpoint.getEndpointName());
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
                .setResultCallback(status -> {
                    if (e.isDisposed()) {
                        // the emitter is not longer interested
                        return;
                    }
                    ConnectionEvent event;

                    if (status.isSuccess()) {
                        log("successfully connected to " + endpoint.getEndpointName());
                        event = new ConnectionEvent(ConnectionEvent.EventType.REQUEST_ACCEPTED, endpoint);
                    } else {
                        log("failed to connect to " + endpoint.getEndpointName() + " " + status.getStatusCode());
                        event = new ConnectionEvent(ConnectionEvent.EventType.REQUEST_REJECTED, endpoint);
                    }

                    e.onSuccess(event);
                });
    }

    /**
     * Disconnects from the specified endpoint
     */
    public void disconnectFromEndpoint(@NonNull Endpoint endpoint) {
        checkNotNull(endpoint, "endpoint == null");
        log("disconnecting from endpoint " + endpoint.getEndpointName());
        Nearby.Connections.disconnectFromEndpoint(googleApiClient, endpoint.getEndpointId());
        endpoints.removeEndpoint(endpoint);
        endpointsSubject.onNext(endpoints);
    }

    /**
     * Stops discovering and advertising and clears all values
     */
    public void stopAllEndpoints() {
        log("stop all endpoints");
        Nearby.Connections.stopAllEndpoints(googleApiClient);
        endpoints.clear();
        endpointsSubject.onNext(endpoints);
    }

    // MESSAGES

    /**
     * Emits when a new message was received
     */
    @CheckResult @NonNull
    public Observable<Message> messages() {
        return messages;
    }

    /**
     * Sends the payload to all connected devices
     */
    public void sendMessageToAllConnectedEndpoints(@NonNull Payload payload) {
        checkNotNull(payload, "payload == null");
        log("sending message to all");
        for (Endpoint endpoint : endpoints.getEndpointsByStatus(Endpoint.Status.STATUS_CONNECTED)) {
            sendMessage(new Message(endpoint, payload));
        }
    }

    /**
     * Sends the message
     */
    public void sendMessage(@NonNull Message message) {
        checkNotNull(message, "message == null");
        log("sending message to " + message.getEndpoint().getEndpointName());
        Nearby.Connections.sendPayload(googleApiClient, message.getEndpoint().getEndpointId(), message.getPayload());
    }

}
