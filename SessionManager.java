import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();

    public Session createSession(String username, Path rootDir) {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, username, rootDir);
        sessions.put(sessionId, session);
        return session;
    }

    public Session getSession(String sessionId) {
        Session session = sessions.get(sessionId);

        if (session == null) {
            throw new IllegalArgumentException("Invalid session");
        }

        if (!session.isAuthenticated()) {
            throw new IllegalArgumentException("Session is not authenticated");
        }

        session.touch();
        return session;
    }

    public void removeSession(String sessionId) {
        Session session = sessions.remove(sessionId);
        if (session != null) {
            session.setAuthenticated(false);
        }
    }
}