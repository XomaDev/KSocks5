package space.themelon.ksocks5.interfaces;

import java.net.InetAddress;

public interface ConnectionCallback {
  boolean newConnection(InetAddress from, InetAddress address, int port);
}
