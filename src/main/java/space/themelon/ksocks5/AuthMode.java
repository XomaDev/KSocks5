package space.themelon.ksocks5;

import java.io.IOException;

public class AuthMode {

  private static final byte USERNAME_PASSWORD_VERSION = 0x01;

  private static final byte[] USER_PASS_OKAY = new byte[] {USERNAME_PASSWORD_VERSION, 0x00};
  private static final byte[] USER_PASS_NOT_OKAY = new byte[] {USERNAME_PASSWORD_VERSION, 0x01};

  public interface ValidationCallback {
    void success() throws IOException;
    void failed() throws IOException;
  }

  public interface UsernamePasswordValidation {
    void validate(String username, String password, ValidationCallback callback) throws IOException;
  }

  public static final AuthMode[] NO_AUTH = { new AuthMode(AuthType.NO_AUTH) };

  public enum AuthType {
    NO_AUTH((byte) 0x00),
    USERNAME_PASSWORD((byte) 0x02);

    public final byte b;

    AuthType(byte b) {
      this.b = b;
    }
  }

  final AuthType type;
  UsernamePasswordValidation validation = null;

  public AuthMode(AuthType type) {
    this.type = type;
  }

  public AuthMode(UsernamePasswordValidation validation) {
    this.type = AuthType.USERNAME_PASSWORD;
    this.validation = validation;
  }

  void handle(Socks5 socks5) throws IOException {
    if (type == AuthType.NO_AUTH) {
      return;
    }
    int version = socks5.read();
    if (version != USERNAME_PASSWORD_VERSION) {
      throw new Socks5Exception("Unknown Username Password Version " + version);
    }
    byte[] username = socks5.readString();
    byte[] password = socks5.readString();

    validation.validate(new String(username), new String(password), new ValidationCallback() {
      @Override
      public void success() throws IOException {
        socks5.writeArray(USER_PASS_OKAY);
      }

      @Override
      public void failed() throws IOException {
        socks5.writeArray(USER_PASS_NOT_OKAY);
        socks5.close();
      }
    });
  }

  @Override
  public String toString() {
    return "AuthMode{" +
        "type=" + type +
        ", validation=" + validation +
        '}';
  }
}
