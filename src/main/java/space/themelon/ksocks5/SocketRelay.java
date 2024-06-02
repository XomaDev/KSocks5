package space.themelon.ksocks5;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;

public class SocketRelay {

  private final Socket client, server;

  private final InputStream clientInput, serverInput;
  private final OutputStream clientOutput, serverOutput;

  private final byte[] buffer = new byte[4096];

  public SocketRelay(Socket client, Socket server) throws IOException {
    client.setSoTimeout(25);
    server.setSoTimeout(25);

    this.client = client;
    this.server = server;

    clientInput = client.getInputStream();
    serverInput = server.getInputStream();

    clientOutput = client.getOutputStream();
    serverOutput = server.getOutputStream();

    relay();
  }

  private void relay() {
    boolean active = true;
    while (active) {
      int len = readBuffer(clientInput);

      if (len < 0) {
        active = false;
      } else if (len > 0) {
        safeWrite(len, serverOutput);
      }

      len = readBuffer(serverInput);
      if (len < 0) {
        active = false;
      } else if (len > 0) {
        safeWrite(len, clientOutput);
      }
    }
  }

  private int readBuffer(InputStream input) {
    int len;
    try {
      len = input.read(buffer);
    } catch (InterruptedIOException e) {
      len = 0;
    } catch (IOException e) {
      close();
      len = -1;
    }
    if (len == -1) close();
    return len;
  }

  private void safeWrite(int len, OutputStream output) {
    try {
      output.write(buffer, 0, len);
      output.flush();
    } catch (IOException ignored) { }
  }

  private void close() {
    try {
      clientOutput.flush();
      clientOutput.close();
    } catch (IOException ignored) { }

    try {
      serverOutput.flush();
      serverOutput.close();
    } catch (IOException ignored) { }

    safeClose(client);
    safeClose(server);
  }

  private void safeClose(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ignored) { }
    }
  }
}
