package space.themelon.ksocks5;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class SimpleBind {
  public static void main(String[] args) throws IOException {
    ProxyServer proxy = new ProxyServer(12345);

    Socket socket = new Socket("localhost", 12345);

    OutputStream output = socket.getOutputStream();
    InputStream input = socket.getInputStream();

    output.write(0x05); // VERSION
    output.write(0x01); // NAUTH
    output.write(0x00); // NO AUTH

    if (input.read() != 0x05 || input.read() != 0x00) {
      throw new IOException("Unable to greet");
    }

    output.write(0x05); // VERSION
    output.write(0x02); // COMMAND BIND
    output.write(0x00); // RSV
    output.write(0x01); // ADDRESS IPV4
    output.write(new byte[]{0x00, 0x00, 0x00, 0x00}); // IP ADDRESS
    output.write(new byte[]{0x00, 0x00}); // BIND PORT

    if (
        input.read() != 0x05
            || input.read() != 0x00
            || input.read() != 0x01
    ) {
      throw new IOException("Unable to connect");
    }
    byte[] hostname = new byte[4];
    input.read(hostname);

    InetAddress address = InetAddress.getByAddress(hostname);
    int port = input.read() << 8 | input.read();

    System.out.println("Address: " + address);
    System.out.println("Bound on: " + port);

    // simple echo play, mimic EXTERNAL -> PROXY -> CLIENT

    new Thread(() -> {
      try {
        OutputStream echoOutput = new Socket(address, port).getOutputStream();
        echoOutput.write("hello world".getBytes());
        echoOutput.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).start();

    for (;;) {
      int n = input.read();
      if (n == -1) {
        break;
      }
      System.out.print((char) n);
    }
    socket.close();
    proxy.close();
  }
}
