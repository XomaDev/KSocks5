package space.themelon.ksocks5;

import space.themelon.ksocks5.interfaces.ClientCallback;
import space.themelon.ksocks5.interfaces.ConnectionCallback;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ProxyServer {

  public static class Builder {

    private final String hostname;
    private final int port;

    private boolean reverseProxy = false;

    private AuthMode[] auth = AuthMode.NO_AUTH;
    
    private ClientCallback clientCallback;
    private ConnectionCallback connectionCallback;

    public Builder(int port) {
      this("localhost", port);
    }

    public Builder(String hostname, int port) {
      this.hostname = hostname;
      this.port = port;
    }

    public Builder reverseProxy() {
      reverseProxy = true;
      return this;
    }

    public Builder auth(AuthMode... auth) {
      this.auth = auth;
      return this;
    }
    
    public Builder clientMonitor(ClientCallback callback) {
      this.clientCallback = callback;
      return this;
    }

    public Builder connectionMonitor(ConnectionCallback callback) {
      this.connectionCallback = callback;
      return this;
    }

    public ProxyServer build() throws IOException {
      return new ProxyServer(hostname, port, reverseProxy, auth, clientCallback, connectionCallback);
    }
  }

  private final ServerSocket server;
  private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

  private final AuthMode[] auth;

  private final ClientCallback clientCallback;
  private final ConnectionCallback connectionCallback;

  private ProxyServer(
      String hostname,
      int port,
      boolean reverseProxy,
      AuthMode[] auth,
      ClientCallback clientCallback,
      ConnectionCallback connectionCallback
  ) throws IOException {
    this.auth = auth;

    this.clientCallback = clientCallback;
    this.connectionCallback = connectionCallback;

    if (reverseProxy) {
      server = null;
    } else {
      server = new ServerSocket(port);
      server.setReceiveBufferSize(100);
      server.setPerformancePreferences(0, 1, 2);
    }

    service.execute(() -> {
      for (;;) {
        try {
          Socket client = reverseProxy ? new Socket(hostname, port) : server.accept();
          initSocket(client);
        } catch (IOException e) {
          close();
          break;
        }
      }
    });
  }

  private void initSocket(Socket client) {
    if (clientCallback != null && !clientCallback.newClient(client.getInetAddress())) {
      closeSafely(client);
      return;
    }
    initSocks5(auth, client);
  }

  private void initSocks5(AuthMode[] auth, Socket client) {
    new Thread(() -> {
      try {
        new Socks5(client, connectionCallback, auth);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();
  }

  public void close() {
    closeSafely(server);
    service.shutdownNow();
  }

  private static void closeSafely(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ignored) {

      }
    }
  }
}
