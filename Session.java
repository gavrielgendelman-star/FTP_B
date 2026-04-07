import java.io.Serializable;
import java.nio.file.Path;

public class Session implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String username;
    private final Path rootDir;
    private Path currentDir;
    private boolean authenticated;
    private long lastActivityTime;
    private transient PassiveDataContext passiveContext;

    public Session(String sessionId, String username, Path rootDir) {
        this.sessionId = sessionId;
        this.username = username;
        this.rootDir = rootDir;
        this.currentDir = rootDir;
        this.authenticated = true;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUsername() {
        return username;
    }

    public Path getRootDir() {
        return rootDir;
    }

    public synchronized Path getCurrentDir() {
        return currentDir;
    }

    public synchronized void setCurrentDir(Path currentDir) {
        this.currentDir = currentDir;
        touch();
    }

    public synchronized boolean isAuthenticated() {
        return authenticated;
    }

    public synchronized void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        touch();
    }

    public synchronized long getLastActivityTime() {
        return lastActivityTime;
    }

    public synchronized void setPassiveContext(PassiveDataContext passiveContext) {
        this.passiveContext = passiveContext;
        touch();
    }

    public synchronized PassiveDataContext getPassiveContext() {
        return passiveContext;
    }

    public synchronized PassiveDataContext clearPassiveContext() {
        PassiveDataContext current = passiveContext;
        passiveContext = null;
        touch();
        return current;
    }

    public synchronized void touch() {
        this.lastActivityTime = System.currentTimeMillis();
    }
}
