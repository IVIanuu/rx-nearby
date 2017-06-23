# RxNearby
RxJava wrapper around the nearby connections api

## Introduction
This library wraps the nearby connections api in an easy to use reactive interface.

## Download
```groovy

android {
   
    ...
    
    // include this to fix a build error
    packagingOptions {
        exclude 'META-INF/rxjava.properties'
    }
}

dependencies {
	 compile 'com.github.IVIanuu.RxNearby:LATEST-VERSION'
}
```

## Usage

First create a rxnearby instance

```java
public class MyClass {
    
    private RxNearby rxNearby;
    
    public MyClass(Context context) {
        String serviceId = context.getPackageName(); // should be a unique id such as your package name
        rxNearby = new RxNearby(context, serviceId);
    }
}
```

To advertise a service call the startAdvertising method

```java
public class MyClass {

    ...

    public void advertiseMyAmazingService() {
        rxNearby.startAdvertising()
                .subscribe(new Consumer<Endpoint>() {
                    @Override
                    public void accept(@NonNull Endpoint endpoint) throws Exception {
                        switch (endpoint.getStatus()) {
                            case Endpoint.STATUS_CONNECTED:
                                // this endpoint is connected now
                                break;
                            case Endpoint.STATUS_DISCONNECTED:
                                // this endpoint is disconnected now
                                break;
                            case Endpoint.STATUS_REQUESTING_ME:
                                // this endpoint requests a connection
                                handleConnectionRequest(endpoint);
                                break;
                        }
                    }
                });
    }
}

```

Stop advertising by calling stopAdvertising

```java
public class MyClass {

    ...

   public void doNotAdvertiseAnymore() {
        rxNearby.stopAdvertising();
    }
    
}
```

To discover those advertised services from another device you have to use the startDiscovery method

```java
public class MyClass {

    ...

     public void discoverMyCoolServices() {
        rxNearby.startDiscovery()
                .subscribe(new Consumer<Endpoint>() {
                    @Override
                    public void accept(@NonNull Endpoint endpoint) throws Exception {
                        switch (endpoint.getStatus()) {
                            case Endpoint.STATUS_FOUND:
                                // oh cool we found this nice service let's connect to it
                                connectToMySuperCoolEndpoint(endpoint);
                                break;
                            case Endpoint.STATUS_LOST:
                                // oh no the service is gone
                                break;
                        }
                    }
                });
    }   
}
```

To stop discovery just call the stopDiscoveryMethod

```java
public class MyClass {

    ...

    public void imNotInterestedInOtherServices() {
        rxNearby.stopDiscovery();
    }
    
}
```

Here are some other methods which you can use to do stuff with this library

```java
public class MyClass {

    private RxNearby rxNearby;

    public MyClass(Context context) {

        String serviceId = context.getPackageName(); // should be a unique id such as your package name
        rxNearby = new RxNearby(context, serviceId);

        // observe messages
        rxNearby.messages()
                .subscribe(message -> {
                    // do something with the received message
                });

        // disconnects from all devices
        rxNearby.stopAllEndpoints();

        // requests a connection to the specified endpoint
        rxNearby.requestConnection(endpoint);

        // accepts a connection after request
        rxNearby.acceptConnection(endpoint);

        // do not allow connection
        rxNearby.rejectConnection(endpoint);

        // disconnects from the endpoint
        rxNearby.disconnectFromEndpoint(endpoint);

        // sends the message
        rxNearby.sendMessage(message);

        // stops everything and disconnects from all devices
        rxNearby.release();

        // observes the current state for example Discovering, Advertising
        rxNearby.state();
        
        // many many more just check it out
    }
}

```

## License

Copyright 2017 Manuel Wrage

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
