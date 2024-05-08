package space.themelon.ksocks5;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class SimpleProxy {
  public static void main(String[] args) throws IOException {
    int port = 1234;
    ProxyServer server = new ProxyServer(port);

    if (true) {
      return;
    }

    Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", port));
    HttpURLConnection connection = (HttpURLConnection) new URL("https://api.ipify.org").openConnection(proxy);
    connection.setRequestMethod("GET");

    InputStream input = connection.getInputStream();
    for (;;) {
      int read = input.read();
      if (read == -1) {
        break;
      }
      System.out.print((char) read);
    }
    input.close();
    server.close();
    System.exit(0);
  }
}
