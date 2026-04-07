import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class FtpControlImplementation extends UnicastRemoteObject
        implements FtpControlInterface, Serializable {

    private static final long serialVersionUID = 1L;
    private static final int PASSIVE_TTL_MILLIS = 20000;

    private final SessionManager sessionManager;
    private final FileService fileService;
    private final UserService userService;
    private final DataTransferService dataTransferService;

    public FtpControlImplementation(SessionManager sessionManager,
                                    FileService fileService,
                                    UserService userService,
                                    DataTransferService dataTransferService) throws RemoteException {
        super();
        this.sessionManager = sessionManager;
        this.fileService = fileService;
        this.userService = userService;
        this.dataTransferService = dataTransferService;
    }

    public String login(String username, String password) throws RemoteException {
        if (!userService.authenticate(username, password)) {
            throw new RemoteException("Authentication failed");
        }

        try {
            Path userRoot = userService.getUserRoot(username);
            Files.createDirectories(userRoot);

            Session session = sessionManager.createSession(username, userRoot);
            return session.getSessionId();
        } catch (Exception e) {
            throw new RemoteException("Login failed: " + e.getMessage(), e);
        }
    }

    public void logout(String sessionId) throws RemoteException {
        try {
            closePassiveChannel(sessionId);
        } catch (Exception ignored) {
        }
        sessionManager.removeSession(sessionId);
    }

    public String pwd(String sessionId) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            return fileService.getWorkingDirectory(session);
        } catch (Exception e) {
            throw new RemoteException("PWD failed: " + e.getMessage(), e);
        }
    }

    public boolean cwd(String sessionId, String path) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            return fileService.changeWorkingDirectory(session, path);
        } catch (Exception e) {
            throw new RemoteException("CWD failed: " + e.getMessage(), e);
        }
    }

    public boolean mkd(String sessionId, String path) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            return fileService.makeDirectory(session, path);
        } catch (Exception e) {
            throw new RemoteException("MKD failed: " + e.getMessage(), e);
        }
    }

    public boolean rmd(String sessionId, String path) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            return fileService.deleteDirectory(session, path);
        } catch (Exception e) {
            throw new RemoteException("RMD failed: " + e.getMessage(), e);
        }
    }

    public boolean dele(String sessionId, String path) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            return fileService.deleteFile(session, path);
        } catch (Exception e) {
            throw new RemoteException("DELE failed: " + e.getMessage(), e);
        }
    }

    public String[] list(String sessionId, String path) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            return fileService.list(session, path);
        } catch (Exception e) {
            throw new RemoteException("LIST failed: " + e.getMessage(), e);
        }
    }

    public PassiveConnectionInfo pasv(String sessionId) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            closePassiveContext(session);

            String host = resolvePassiveHost();
            ServerSocket serverSocket = new ServerSocket(0);
            PassiveDataContext context = new PassiveDataContext(serverSocket, host, PASSIVE_TTL_MILLIS);
            session.setPassiveContext(context);

            return new PassiveConnectionInfo(context.getPassiveHost(), context.getPassivePort(), sessionId, context.getExpiresAt());
        } catch (Exception e) {
            throw new RemoteException("PASV failed: " + e.getMessage(), e);
        }
    }

    public TransferTicket prepareList(String sessionId, String path) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            ensurePassiveContextReady(session);

            Path target = fileService.resolveSafePath(session, path);
            if (!Files.exists(target) || !Files.isDirectory(target)) {
                return TransferTicket.fail("LIST", path, "Directory not found");
            }

            String transferId = UUID.randomUUID().toString();
            dataTransferService.startListTransfer(session, target, transferId);
            return TransferTicket.ok(transferId, "LIST", fileService.toVirtualPath(session, target), "Ready to send listing");
        } catch (SecurityException e) {
            return TransferTicket.fail("LIST", path, e.getMessage());
        } catch (Exception e) {
            throw new RemoteException("prepareList failed: " + e.getMessage(), e);
        }
    }

    public TransferTicket prepareRetr(String sessionId, String path) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            ensurePassiveContextReady(session);

            Path target = fileService.resolveSafePath(session, path);
            if (!Files.exists(target) || Files.isDirectory(target)) {
                return TransferTicket.fail("RETR", path, "File not found");
            }

            String transferId = UUID.randomUUID().toString();
            dataTransferService.startRetrTransfer(session, target, transferId);
            return TransferTicket.ok(transferId, "RETR", fileService.toVirtualPath(session, target), "Ready to send file");
        } catch (SecurityException e) {
            return TransferTicket.fail("RETR", path, e.getMessage());
        } catch (Exception e) {
            throw new RemoteException("prepareRetr failed: " + e.getMessage(), e);
        }
    }

    public TransferTicket prepareStor(String sessionId, String path) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            ensurePassiveContextReady(session);

            Path target = fileService.resolveSafePath(session, path);
            Path parent = target.getParent();

            if (parent == null || !Files.exists(parent) || !Files.isDirectory(parent)) {
                return TransferTicket.fail("STOR", path, "Parent directory does not exist");
            }

            if (Files.exists(target) && Files.isDirectory(target)) {
                return TransferTicket.fail("STOR", path, "Target path is a directory");
            }

            String transferId = UUID.randomUUID().toString();
            dataTransferService.startStorTransfer(session, target, transferId);
            return TransferTicket.ok(transferId, "STOR", fileService.toVirtualPath(session, target), "Ready to receive file");
        } catch (SecurityException e) {
            return TransferTicket.fail("STOR", path, e.getMessage());
        } catch (Exception e) {
            throw new RemoteException("prepareStor failed: " + e.getMessage(), e);
        }
    }

    public void closePassiveChannel(String sessionId) throws RemoteException {
        try {
            Session session = sessionManager.getSession(sessionId);
            closePassiveContext(session);
        } catch (IllegalArgumentException ignored) {
        } catch (Exception e) {
            throw new RemoteException("closePassiveChannel failed: " + e.getMessage(), e);
        }
    }

    private void ensurePassiveContextReady(Session session) {
        PassiveDataContext context = session.getPassiveContext();
        if (context == null) {
            throw new IllegalStateException("PASV was not called");
        }
        if (!context.isReady()) {
            closePassiveContext(session);
            throw new IllegalStateException("Passive data channel is not ready");
        }
    }

    private void closePassiveContext(Session session) {
        PassiveDataContext context = session.clearPassiveContext();
        if (context != null) {
            context.close();
        }
    }

    private String resolvePassiveHost() {
        String envHost = System.getenv("FTP_PASSIVE_HOST");
        if (envHost != null && !envHost.trim().isEmpty()) {
            return envHost.trim();
        }

        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            if (host == null || host.isBlank()) {
                return "127.0.0.1";
            }
            return host;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
