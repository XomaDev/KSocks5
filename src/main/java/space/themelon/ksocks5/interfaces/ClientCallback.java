package space.themelon.ksocks5.interfaces;

import java.net.InetAddress;

public interface ClientCallback {
  boolean newClient(InetAddress address);
}
