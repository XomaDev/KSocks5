package space.themelon.ksocks5;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Socks5 {

  private static final byte SOCKS_VERSION = 0x05;

  private static final byte NO_AUTH = 0x00;

  private static final byte ADDR_IPV4 = 0x01;
  private static final byte ADDR_DOMAIN = 0x03;
  private static final byte ADDR_IPV6 = 0x04;

  private static final byte CMD_STREAM = 0x01;
  private static final byte CMD_BIND = 0x02;
  private static final byte CMD_ASSOCIATE_UDP = 0x03;

  private static final byte STATUS_GRANTED = 0x00;
  private static final byte STATUS_FAILURE = 0x01;
  private static final byte STATUS_NOT_ALLOWED = 0x02;
  private static final byte STATUS_NETWORK_UNREACHABLE = 0x03;
  private static final byte STATUS_HOST_UNREACHABLE = 0x04;
  private static final byte STATUS_CONNECTION_REFUSED = 0x05;
  // private static final byte STATUS_TTL_EXPIRED = 0x06; (Not possible in Java)
  private static final byte STATUS_PROTOCOL_ERROR = 0x07;
  private static final byte STATUS_ADDRESS_UNSUPPORTED = 0X08;

  private final Socket client;
  private final InputStream input;
  private final OutputStream output;

  public Socks5(Socket client) throws IOException {
    this.client = client;

    input = client.getInputStream();
    output = client.getOutputStream();

    acceptGreet();
    serveProxy();
  }

  private void acceptGreet() throws IOException {
    matchVersion();
    int nAuth = read();
    byte[] auths = readArray(nAuth);
    for (byte auth : auths) {
      if (auth == NO_AUTH) {
        write(SOCKS_VERSION);
        write(auth);
        break;
      }
    }
  }

  private void serveProxy() throws IOException {
    matchVersion();
    byte command = (byte) read();
    matchRSV();
    byte addrType = (byte) read();

    byte[] rawAddress = null;
    InetAddress address = null;

    switch (addrType) {
      case ADDR_IPV4:
      case ADDR_IPV6: {
        rawAddress = readArray(addrType == ADDR_IPV4 ? 4 : 16);
        address = InetAddress.getByAddress(rawAddress);
        break;
      }
      case ADDR_DOMAIN: {
        byte[] domain = readString();
        address = InetAddress.getByName(new String(domain));
        rawAddress = new byte[1 + domain.length];
        System.arraycopy(domain, 0, rawAddress, 1, domain.length);
        break;
      }
    }
    byte[] portBytes = readArray(2);

    write(SOCKS_VERSION);
    if (address == null) {
      write(STATUS_ADDRESS_UNSUPPORTED);
      writeAddress(addrType, rawAddress, portBytes);
      return;
    }

    int port = (portBytes[0] & 0xff) << 8 | portBytes[1] & 0xff;

    switch (command) {
      case CMD_STREAM: {
        Socket socket = createSocket(address, port);
        writeAddress(addrType, rawAddress, portBytes);
        if (socket != null) {
          new SocketRelay(client, socket);
        }
        break;
      }
      case CMD_BIND: {
        System.out.println("Received bind request");
        String hostname = address.getHostAddress();
        int bindPort = port == 0 ? SocketUtils.INSTANCE.findAvailableTcpPort() : port;

        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(hostname, bindPort));

        byte[] bindPortBytes = new byte[] {
            (byte) (bindPort >> 8), (byte) (bindPort)
        };
        writeAddress(addrType, InetAddress.getByName(hostname).getAddress(), bindPortBytes);

        Socket socket = server.accept();
        server.close();

        new SocketRelay(client, socket);
        break;
      }
      default: {
        write(STATUS_PROTOCOL_ERROR);
        writeAddress(addrType, rawAddress, portBytes);
      }
    }
  }

  private Socket createSocket(InetAddress address, int port) throws IOException {
    try {
      Socket socket = new Socket(address, port);
      write(STATUS_GRANTED);
      return socket;
    } catch (NoRouteToHostException e) {
      write(STATUS_NETWORK_UNREACHABLE);
    } catch (UnknownHostException e) {
      write(STATUS_HOST_UNREACHABLE);
    } catch (ConnectException e) {
      write(STATUS_CONNECTION_REFUSED);
    } catch (Exception e) {
      write(STATUS_FAILURE);
    }
    return null;
  }

  private void writeAddress(byte addrType, byte[] address, byte[] port) throws IOException {
    write((byte) 0); // RSV
    write(addrType);
    writeArray(address);
    writeArray(port);
  }

  private void matchVersion() throws IOException {
    if (read() != SOCKS_VERSION) {
      throw new Socks5Exception("Expected Socks 5 Version");
    }
  }

  private void matchRSV() throws IOException {
    if (read() != 0x00) {
      throw new Socks5Exception("Expected RSV byte");
    }
  }

  private byte[] readString() throws IOException {
    int length = read();
    byte[] string = new byte[length];
    for (int i = 0; i < length; i++) {
      string[i] = (byte) read();
    }
    return string;
  }

  private void writeArray(byte[] bytes) throws IOException {
    for (byte b : bytes) {
      write(b);
    }
  }

  private void write(byte b) throws IOException {
    output.write(b);
  }

  private byte[] readArray(int len) throws IOException {
    byte[] bytes = new byte[len];
    for (int i = 0; i < len; i++) {
      bytes[i] = (byte) read();
    }
    return bytes;
  }

  private int read() throws IOException {
    return input.read();
  }
}
