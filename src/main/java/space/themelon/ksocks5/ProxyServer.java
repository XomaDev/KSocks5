package space.themelon.ksocks5;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProxyServer {

  private final AtomicBoolean running = new AtomicBoolean(true);

  private final ServerSocket server;

  public ProxyServer(int port) throws IOException {
    server = new ServerSocket(port);
    new Thread(() -> {
      while (running.get()) {
        try {
          acceptConnections();
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

  private void acceptConnections() throws IOException {
    Socket client = server.accept();
    new Thread(() -> {
      try {
        new Socks5(client);
      } catch (IOException ignored) {

      }
    }).start();
  }
}
