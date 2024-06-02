package space.themelon.ksocks5;

import space.themelon.ksocks5.interfaces.ConnectionCallback;

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

  private static final byte ADDR_IPV4 = 0x01;
  private static final byte ADDR_DOMAIN = 0x03;
  private static final byte ADDR_IPV6 = 0x04;

  private static final byte CMD_STREAM = 0x01;
  private static final byte CMD_BIND = 0x02;
  // private static final byte CMD_ASSOCIATE_UDP = 0x03; (Not Implemented)

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
  private final InetAddress clientAddress;

  private final InputStream input;
  private final OutputStream output;

  private final ConnectionCallback callback;
  private final AuthMode[] authModes;

  public Socks5(Socket client, ConnectionCallback callback, AuthMode... authModes) throws IOException {
    client.setReceiveBufferSize(100);

    this.client = client;
    this.callback = callback;
    this.authModes = authModes;

    clientAddress = client.getInetAddress();

    input = client.getInputStream();
    output = client.getOutputStream();

    if (acceptGreet()) {
      serveProxy();
    }
  }

  private boolean acceptGreet() throws IOException {
    matchVersion();
    int nAuth = read();
    byte[] auths = readArray(nAuth);
    for (AuthMode mode : authModes) {
      for (byte auth : auths) {
        if (auth == mode.type.b) {
          writeArray(SOCKS_VERSION, auth);
          mode.handle(this);
          return true;
        }
      }
    }
    writeArray(SOCKS_VERSION, (byte) 0xFF);
    close();
    return false;
  }

  private void serveProxy() throws IOException {
    matchVersion();
    byte command = (byte) read();
    matchRSV();
    byte addrType = (byte) read();

    InetAddress address = null;

    switch (addrType) {
      case ADDR_IPV4:
      case ADDR_IPV6: {
        byte[] bytes = readArray(addrType == ADDR_IPV4 ? 4 : 16);
        address = InetAddress.getByAddress(bytes);
        break;
      }
      case ADDR_DOMAIN: {
        byte[] domain = readString();
        address = InetAddress.getByName(new String(domain));
        break;
      }
    }

    if (address == null) {
      writeReply(STATUS_ADDRESS_UNSUPPORTED, 0);
      return;
    }

    int port = (read() & 0xff) << 8 | read() & 0xff;

    if (callback != null && !callback.newConnection(clientAddress, address, port)) {
      System.out.println("rejected");
      writeReply(STATUS_NOT_ALLOWED, 0);
      return;
    }

    switch (command) {
      case CMD_STREAM: {
        streamTcp(address, port);
        break;
      }
      case CMD_BIND: {
        String hostname = address.getHostAddress();
        int bindPort = port == 0 ? SocketUtils.INSTANCE.findAvailableTcpPort() : port;

        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(hostname, bindPort));

        writeReply(STATUS_GRANTED, bindPort);

        Socket socket = server.accept();
        server.close();

        new SocketRelay(client, socket);
        break;
      }
      default: {
        writeReply(STATUS_PROTOCOL_ERROR, 0);
      }
    }
  }

  private void streamTcp(InetAddress address, int port) throws IOException {
    Socket socket = null;
    byte status;
    try {
      socket = new Socket(address, port);
      status = STATUS_GRANTED;
    } catch (NoRouteToHostException e) {
      status = STATUS_NETWORK_UNREACHABLE;
    } catch (UnknownHostException e) {
      status = STATUS_HOST_UNREACHABLE;
    } catch (ConnectException e) {
      status = STATUS_CONNECTION_REFUSED;
    } catch (Exception e) {
      status = STATUS_FAILURE;
    }
    writeReply(status, socket == null ? 0 : socket.getLocalPort());
    if (socket != null) {
      new SocketRelay(client, socket);
    }
  }

  private void writeReply(byte status, int port) throws IOException {
    byte[] bytes = new byte[10];
    bytes[0] = SOCKS_VERSION;
    // bytes[1] = 0x00; RSV
    bytes[2] = status;
    bytes[3] = 0x01; // Ipv4 type
    // bytes[4 - 7] = address bytes
    bytes[8] = (byte) (port >> 8);
    bytes[9] = (byte) port;

    output.write(bytes);
    output.flush();
  }

  private void matchVersion() throws IOException {
    int version = read();
    if (version != SOCKS_VERSION) {
      throw new Socks5Exception("Expected Socks 5 Version, got " + version);
    }
  }

  private void matchRSV() throws IOException {
    if (read() != 0x00) {
      throw new Socks5Exception("Expected RSV byte");
    }
  }

  byte[] readString() throws IOException {
    int length = read();
    byte[] string = new byte[length];
    for (int i = 0; i < length; i++) {
      string[i] = (byte) read();
    }
    return string;
  }

  void writeArray(byte... bytes) throws IOException {
    output.write(bytes);
  }

  private byte[] readArray(int len) throws IOException {
    byte[] bytes = new byte[len];
    for (int i = 0; i < len; i++) {
      bytes[i] = (byte) read();
    }
    return bytes;
  }

  int read() throws IOException {
    return input.read();
  }

  public void close() throws IOException {
    client.close();
  }
}
