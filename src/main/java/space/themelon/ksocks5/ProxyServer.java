package space.themelon.ksocks5;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProxyServer {

  private final AtomicBoolean running = new AtomicBoolean(true);

  private final ServerSocket server;

  public ProxyServer(int port) throws IOException {
    this(port, AuthMode.NO_AUTH);
  }

  public ProxyServer(int port, AuthMode... authModes) throws IOException {
    server = new ServerSocket(port);
    new Thread(() -> {
      while (running.get()) {
        try {
          acceptConnections(authModes);
        } catch (IOException e) {
          running.set(false);
          break;
        }
      }
    }).start();
  }

  public void close() throws IOException {
    running.set(false);
    server.close();
  }

  private void acceptConnections(AuthMode[] authModes) throws IOException {
    Socket client = server.accept();
    new Thread(() -> {
      try {
        new Socks5(client, authModes);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();
  }
}
