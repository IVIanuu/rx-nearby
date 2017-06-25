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

package com.ivianuu.rxnearby.sample;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;

import com.ivianuu.rxnearby.ConnectionEvent;
import com.ivianuu.rxnearby.Endpoint;
import com.ivianuu.rxnearby.Endpoints;
import com.ivianuu.rxnearby.Message;
import com.ivianuu.rxnearby.RxNearby;
import com.ivianuu.rxnearby.State;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

// ACTIVITY IS LEAKING!! ALWAYS DISPOSE YOU OBSERVERS:D
public class MainActivity extends AppCompatActivity {

    private Button advertiseButton;
    private Button discoveryButton;
    private Button stopAllEndpointsButton;

    private TextView allEndpoints;
    private TextView connectedEndpoints;
    private TextView discoveredEndpoints;

    private RxNearby rxNearby;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        advertiseButton = (Button) findViewById(R.id.advertise);
        discoveryButton = (Button) findViewById(R.id.discover);
        stopAllEndpointsButton = (Button) findViewById(R.id.stop_all_endpoints);

        allEndpoints = (TextView) findViewById(R.id.all_endpoints);
        connectedEndpoints = (TextView) findViewById(R.id.connected_endpoints);
        discoveredEndpoints = (TextView) findViewById(R.id.discovered_endpoints);

        stopAllEndpointsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rxNearby.stopAllEndpoints();
            }
        });

        rxNearby = new RxNearby(this, getPackageName());

        rxNearby.state()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<State>() {
                    @Override
                    public void accept(@NonNull State state) throws Exception {
                        advertiseButton.setText(state.isAdvertising() ? "Stop advertising" : "Start advertising");
                        discoveryButton.setText(state.isDiscovering() ? "Stop discovery" : "Start discovery");
                    }
                });

        rxNearby.endpoints()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Endpoints>() {
                    @Override
                    public void accept(@NonNull Endpoints endpoints) throws Exception {
                        // all endpoints
                        List<Endpoint> allEndpoints = endpoints.getEndpoints();
                        if (allEndpoints.isEmpty()) {
                            MainActivity.this.allEndpoints.setText("No endpoints");
                        } else {
                            String text = "";
                            for (Endpoint endpoint : allEndpoints) {
                                text += endpoint.getEndpointName() + ", ";
                            }
                            MainActivity.this.allEndpoints.setText(text);
                        }

                        // connected endpoints
                        List<Endpoint> connectedEndpoints = endpoints.getEndpointsByStatus(Endpoint.Status.STATUS_CONNECTED);
                        if (connectedEndpoints.isEmpty()) {
                            MainActivity.this.connectedEndpoints.setText("No connected endpoints");
                        } else {
                            String text = "";
                            for (Endpoint endpoint : connectedEndpoints) {
                                text += endpoint.getEndpointName() + ", ";
                            }
                            MainActivity.this.connectedEndpoints.setText(text);
                        }

                        // discovered endpoints
                        List<Endpoint> discoveredEndpoints = endpoints.getEndpointsByStatus(Endpoint.Status.STATUS_FOUND);
                        if (discoveredEndpoints.isEmpty()) {
                            MainActivity.this.discoveredEndpoints.setText("No discovered endpoints");
                        } else {
                            String text = "";
                            for (Endpoint endpoint : discoveredEndpoints) {
                                text += endpoint.getEndpointName() + ", ";
                            }
                            MainActivity.this.discoveredEndpoints.setText(text);
                        }
                    }
                });

        rxNearby.messages()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Message>() {
                    @Override
                    public void accept(@NonNull Message message) throws Exception {
                        Toast.makeText(MainActivity.this, "New message " + message.getEndpoint().getEndpointName(), Toast.LENGTH_SHORT).show();
                    }
                });

        advertiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rxNearby.isAdvertising()) {
                    rxNearby.stopAdvertising();
                } else {
                    rxNearby.startAdvertising()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Endpoint>() {
                                @Override
                                public void accept(@NonNull Endpoint endpoint) throws Exception {
                                    switch (endpoint.getStatus()) {
                                        case Endpoint.Status.STATUS_CONNECTED:
                                            Toast.makeText(MainActivity.this, "Successfully connected to " + endpoint.getEndpointName(), Toast.LENGTH_SHORT).show();
                                            break;
                                        case Endpoint.Status.STATUS_DISCONNECTED:
                                            Toast.makeText(MainActivity.this, "Disconnected from " + endpoint.getEndpointName(), Toast.LENGTH_SHORT).show();
                                            break;
                                        case Endpoint.Status.STATUS_REQUESTING_ME:
                                            Toast.makeText(MainActivity.this, "Connection request from " + endpoint.getEndpointName(), Toast.LENGTH_SHORT).show();
                                            rxNearby.acceptConnection(endpoint);
                                            break;
                                    }
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(@NonNull Throwable throwable) throws Exception {
                                    Toast.makeText(MainActivity.this, "Failed to start advertising", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        discoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rxNearby.isDiscovering()) {
                    rxNearby.stopDiscovery();
                } else {
                    rxNearby.startDiscovery()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Endpoint>() {
                                @Override
                                public void accept(@NonNull Endpoint endpoint) throws Exception {
                                    switch (endpoint.getStatus()) {
                                        case Endpoint.Status.STATUS_FOUND:
                                            Toast.makeText(MainActivity.this, "Endpoint found " + endpoint.getEndpointName(), Toast.LENGTH_SHORT).show();
                                            connectToEndpoint(endpoint);
                                            break;
                                        case Endpoint.Status.STATUS_LOST:
                                            Toast.makeText(MainActivity.this, "Endpoint lost " + endpoint.getEndpointName(), Toast.LENGTH_SHORT).show();
                                            break;
                                    }
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(@NonNull Throwable throwable) throws Exception {
                                    Toast.makeText(MainActivity.this, "Start discovery failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

        final EditText editText = (EditText) findViewById(R.id.message_input);
        findViewById(R.id.send_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rxNearby.sendMessageToAllConnectedEndpoints(Payload.fromBytes(editText.getText().toString().getBytes()));
            }
        });
    }

    private void connectToEndpoint(final Endpoint endpoint) {
        rxNearby.requestConnection(endpoint)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ConnectionEvent>() {
                    @Override
                    public void accept(@NonNull ConnectionEvent connectionEvent) throws Exception {
                        if (connectionEvent.getType() == ConnectionEvent.EventType.REQUEST_ACCEPTED) {
                            Toast.makeText(MainActivity.this, "Successfully connected " + connectionEvent.getEndpoint().getEndpointName(), Toast.LENGTH_SHORT).show();
                        } else {
                            connectToEndpoint(endpoint);
                            Toast.makeText(MainActivity.this, "Failed to connect to " + connectionEvent.getEndpoint().getEndpointName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rxNearby.release();
    }
}
