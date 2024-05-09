package space.themelon.ksocks5;

import space.themelon.ksocks5.interfaces.ClientCallback;
import space.themelon.ksocks5.interfaces.ConnectionCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PasswordProxy {
  public static void main(String[] args) throws IOException {
    int port = 12346;

    AuthMode userPassAuth = new AuthMode((username, password) -> username.equals("meow") && password.equals("world"));
    ClientCallback clientCallback = address -> {
      System.out.println("Address: " + address);
      return true;
    };
    ConnectionCallback connectionCallback = (from, address, port1) -> {
      System.out.println("Connecting to " + address + " port " + port1);
      return true;
    };
    ProxyServer proxy = new ProxyServer.Builder(port)
        .auth(userPassAuth)
        .clientMonitor(clientCallback)
        .connectionMonitor(connectionCallback)
        .build();

    if (true) {
      return;
    }

    Socket socket = new Socket("localhost", port);

    InputStream input = socket.getInputStream();
    OutputStream output = socket.getOutputStream();

    output.write(0x05); // VERSION
    output.write(0x01); // NAUTH
    output.write(0x02); // USERNAME PASSWORD AUTH

    if (input.read() != 0x05 || input.read() != 0x02) {
      throw new IOException("Unable to greet");
    }
    output.write(0x01); // USERNAME PASSWORD VERSION
    byte[] username = "meow".getBytes();
    output.write(username.length);
    output.write(username);

    byte[] password = "world".getBytes();
    output.write(password.length);
    output.write(password);

    if (input.read() == 0x01 && input.read() == 0x00) {
      System.out.println("Username Password Authentication Successful");
    }
    socket.close();
    proxy.close();
    System.exit(0);
  }
}
