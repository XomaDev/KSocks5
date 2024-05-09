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

    private final int port;
    private AuthMode[] auth = AuthMode.NO_AUTH;
    
    private ClientCallback clientMonitor;
    private ConnectionCallback connectionMonitor;

    public Builder(int port) {
      this.port = port;
    }

    public Builder auth(AuthMode... auth) {
      this.auth = auth;
      return this;
    }
    
    public Builder clientMonitor(ClientCallback callback) {
      this.clientMonitor = callback;
      return this;
    }

    public Builder connectionMonitor(ConnectionCallback callback) {
      this.connectionMonitor = callback;
      return this;
    }

    public ProxyServer build() throws IOException {
      return new ProxyServer(port, auth, clientMonitor, connectionMonitor);
    }
  }

  private final ServerSocket server;
  private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

  private final ConnectionCallback connectionCallback;

  private ProxyServer(
      int port, AuthMode[] auth, ClientCallback clientMonitor,
      ConnectionCallback connectionMonitor
  ) throws IOException {
    this.connectionCallback = connectionMonitor;

    server = new ServerSocket(port);
    server.setReceiveBufferSize(100);
    server.setPerformancePreferences(0, 1, 2);

    service.execute(() -> {
      for (;;) {
        try {
          Socket client = server.accept();
          if (clientMonitor != null && !clientMonitor.newClient(client.getInetAddress())) {
            System.out.println("rejected " + client.getInetAddress());
            client.close();
            return;
          }
          initSocks5(auth, client);
        } catch (IOException e) {
          close();
          break;
        }
      }
    });
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
    try {
      closeable.close();
    } catch (IOException ignored) {

    }
  }
}
