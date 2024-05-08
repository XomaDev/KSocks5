package space.themelon.ksocks5;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Random;

public final class SocketUtils {

  private static final int PORT_RANGE = 64511;
  private static final int PORT_RANGE_MAX = 65535;
  private static final int PORT_RANGE_MIN = 1024;
  public static final SocketUtils INSTANCE = new SocketUtils();
  private static final Random random = new Random(System.currentTimeMillis());

  private SocketUtils() {

  }

  private boolean isPortAvailable(int port) {
    ServerSocket sSocket = null;
    DatagramSocket dSocket = null;
    try {
      sSocket = new ServerSocket(port);
      sSocket.setReuseAddress(true);

      dSocket = new DatagramSocket(port);
      dSocket.setReuseAddress(true);
      dSocket.close();
      try {
        sSocket.close();
      } catch (IOException ignored) {
      }
      return true;
    } catch (IOException e2) {
      if (dSocket != null) {
        dSocket.close();
      }
      if (sSocket != null) {
        try {
          sSocket.close();
          return false;
        } catch (IOException e3) {
          return false;
        }
      }
      return false;
    } catch (Throwable th) {
      if (dSocket != null) {
        dSocket.close();
      }
      if (sSocket != null) {
        try {
          sSocket.close();
        } catch (IOException ignored) {
        }
      }
      throw th;
    }
  }

  private int findRandomPort() {
    return random.nextInt(PORT_RANGE + 1) + 1024;
  }

  public int findAvailableTcpPort() {
    int candidatePort;
    int searchCounter = 0;
    do {
      if (!(searchCounter <= PORT_RANGE)) {
        String format = String.format("Could not find an available TCP port in the range [%d, %d] after %d attempts",
            PORT_RANGE_MIN, PORT_RANGE_MAX, searchCounter);
        throw new IllegalStateException(format);
      }
      candidatePort = findRandomPort();
      searchCounter++;
    } while (!isPortAvailable(candidatePort));
    return candidatePort;
  }
}