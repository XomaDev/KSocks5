package space.themelon.ksocks5;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SocketRelay {

  private final Socket client;
  private final Socket host;

  private final ScheduledExecutorService hostClientExecutor = Executors.newSingleThreadScheduledExecutor();
  private final ScheduledExecutorService clientHostExecutor = Executors.newSingleThreadScheduledExecutor();

  public SocketRelay(Socket client, Socket host) throws IOException {
    this.client  = client;
    this.host = host;

    InputStream hostInput = host.getInputStream();
    OutputStream clientOutput = client.getOutputStream();

    hostClientExecutor.execute(() -> {
      try {
        transfer(hostInput, clientOutput);
      } catch (Exception e) {
        close();
      }
    });

    InputStream clientInput = client.getInputStream();
    OutputStream hostOutput = host.getOutputStream();

    clientHostExecutor.execute(() -> {
      try {
        transfer(clientInput, hostOutput);
      } catch (Exception e) {
        close();
      }
    });
  }

  private void close() {
    try {
      client.close();
      host.close();
    } catch (Exception ignored) {

    }
    if (!hostClientExecutor.isShutdown()) {
      hostClientExecutor.shutdownNow();
    }
    if (!clientHostExecutor.isShutdown()) {
      clientHostExecutor.shutdownNow();
    }
  }

  private void transfer(InputStream input, OutputStream output) throws IOException {
    byte[] bytes = new byte[4096];
    int read;
    while ((read = input.read(bytes)) != -1) {
      output.write(bytes, 0, read);
      output.flush();
    }
    throw new IOException("Socket Closed");
  }
}
