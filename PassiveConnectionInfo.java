import java.io.Serializable;

public class PassiveConnectionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String host;
    private final int port;
    private final String sessionId;
    private final long expiresAt;

    public PassiveConnectionInfo(String host, int port, String sessionId, long expiresAt) {
        this.host = host;
        this.port = port;
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
