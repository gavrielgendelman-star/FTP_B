import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class PassiveDataContext {
    private final ServerSocket dataServerSocket;
    private final int passivePort;
    private final String passiveHost;
    private final long createdAt;
    private final long expiresAt;
    private volatile boolean ready;

    public PassiveDataContext(ServerSocket dataServerSocket, String passiveHost, int ttlMillis) throws IOException {
        this.dataServerSocket = dataServerSocket;
        this.passivePort = dataServerSocket.getLocalPort();
        this.passiveHost = passiveHost;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + ttlMillis;
        this.ready = true;
        this.dataServerSocket.setSoTimeout(ttlMillis);
    }

    public Socket acceptClient() throws IOException {
        return dataServerSocket.accept();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public void close() {
        ready = false;
        try {
            dataServerSocket.close();
        } catch (IOException ignored) {
        }
    }

    public ServerSocket getDataServerSocket() {
        return dataServerSocket;
    }

    public int getPassivePort() {
        return passivePort;
    }

    public String getPassiveHost() {
        return passiveHost;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isReady() {
        return ready && !dataServerSocket.isClosed() && !isExpired();
    }
}
