<h1 align="center">KSocks5</h1>
<p align="center">Standard Socks5 Server implementation in Java</p>

## Features

- TCP Streaming, Bind
- Username / Password Authentication
- Intercept/Monitor incoming clients and connections
- Reverse Proxy

Written in pure Java, no additional libraries, requires Java >= 8


## Setup

```xml
<dependency>
    <groupId>space.themelon</groupId>
    <artifactId>KSocks5</artifactId>
    <version>1.0</version>
</dependency>
```

```kotlin
implementation("space.themelon:KSocks5:1.0")
```

## Usage

- Spawn a simple open proxy server

```java
ProxyServer server = new ProxyServer.Builder(port).build();
...
server.close();
```

- Create a proxy server with Username / Password Authentication

```java
AuthMode userPassAuth = new AuthMode((username, password) -> 
    username.equals("secureusername") && password.equals("securepassword"));

ProxyServer proxy = new ProxyServer.Builder(port)
    .auth(userPassAuth)
    .build();
```

- Monitor incoming client connections (`clientMonitor`)

```java
ClientCallback monitor = address -> {
  System.out.println("New client " + address);
  return true; // approve client
};
ProxyServer server = new ProxyServer.Builder(port)
    .clientMonitor(monitor)
    .build();
```

- Monitor outgoing connection requests (`connectionMonitor`)

```java
ConnectionCallback monitor = (client, destination, port) -> {
  if (isBlacklisted(destination)) {
    System.out.println("Blocking connection to " + destination);
    return false;
  }
  return true;
};

ProxyServer server = new ProxyServer.Builder(port)
    .connectionMonitor(monitor)
    .build();
```

- Create a reverse proxy to outside, rather than awaiting connection

```java
ProxyServer server = new ProxyServer.Builder(remoteHost, remotePort)
    .reverseProxy()
    .build();
```