import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient {
    private static final int BUFFER_SIZE = 4096;

    private static void printUsage() {
        System.out.println("Commands:");
        System.out.println("  login <username> <password>");
        System.out.println("  pwd <sessionId>");
        System.out.println("  cwd <sessionId> <path>");
        System.out.println("  mkdir <sessionId> <path>");
        System.out.println("  rmdir <sessionId> <path>");
        System.out.println("  dele <sessionId> <path>");
        System.out.println("  dir <sessionId> <path>");
        System.out.println("  pasv <sessionId>");
        System.out.println("  list <sessionId> <path>");
        System.out.println("  retr <sessionId> <remotePath> <localPath>");
        System.out.println("  stor <sessionId> <localPath> <remotePath>");
        System.out.println("  closepasv <sessionId>");
        System.out.println("  logout <sessionId>");
        System.out.println();
        System.out.println("Set environment variable:");
        System.out.println("  SERVER_PORT=127.0.0.1:1099");
        System.out.println("Optional on server side:");
        System.out.println("  SERVER_STORAGE=C:\\path\\to\\storage");
        System.out.println("  FTP_PASSIVE_HOST=127.0.0.1");
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                return;
            }

            String environment = System.getenv("SERVER_PORT");
            if (environment == null || environment.trim().isEmpty()) {
                System.out.println("SERVER_PORT is not set");
                return;
            }

            String[] hostPort = environment.split(":");
            String hostname = hostPort[0];
            int portnumber = Integer.parseInt(hostPort[1]);

            Registry myreg = LocateRegistry.getRegistry(hostname, portnumber);
            FtpControlInterface inter = (FtpControlInterface) myreg.lookup("remoteObject");

            String cmd = args[0].toLowerCase();

            switch (cmd) {
                case "login" -> handleLogin(inter, args);
                case "pwd" -> handlePwd(inter, args);
                case "cwd" -> handleCwd(inter, args);
                case "mkdir" -> handleMkdir(inter, args);
                case "rmdir" -> handleRmdir(inter, args);
                case "dele" -> handleDele(inter, args);
                case "dir" -> handleDir(inter, args);
                case "pasv" -> handlePasv(inter, args);
                case "list" -> handleList(inter, args);
                case "retr" -> handleRetr(inter, args);
                case "stor" -> handleStor(inter, args);
                case "closepasv" -> handleClosePasv(inter, args);
                case "logout" -> handleLogout(inter, args);
                default -> printUsage();
            }
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static void handleLogin(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }
        String sessionId = inter.login(args[1], args[2]);
        System.out.println("Login successful");
        System.out.println("Session ID: " + sessionId);
    }

    private static void handlePwd(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            return;
        }
        System.out.println(inter.pwd(args[1]));
    }

    private static void handleCwd(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }
        boolean ok = inter.cwd(args[1], args[2]);
        System.out.println("CWD result: " + ok);
    }

    private static void handleMkdir(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }
        boolean ok = inter.mkd(args[1], args[2]);
        System.out.println("MKD result: " + ok);
    }

    private static void handleRmdir(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }
        boolean ok = inter.rmd(args[1], args[2]);
        System.out.println("RMD result: " + ok);
    }

    private static void handleDele(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }
        boolean ok = inter.dele(args[1], args[2]);
        System.out.println("DELE result: " + ok);
    }

    private static void handleDir(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }
        String[] files = inter.list(args[1], args[2]);
        for (String file : files) {
            System.out.println(file);
        }
    }

    private static void handlePasv(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            return;
        }
        PassiveConnectionInfo info = inter.pasv(args[1]);
        System.out.println("227 Entering Passive Mode");
        System.out.println("Host: " + info.getHost());
        System.out.println("Port: " + info.getPort());
        System.out.println("ExpiresAt: " + info.getExpiresAt());
    }

    private static void handleList(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 3) {
            printUsage();
            return;
        }

        String sessionId = args[1];
        String remotePath = args[2];

        PassiveConnectionInfo info = inter.pasv(sessionId);
        TransferTicket ticket = inter.prepareList(sessionId, remotePath);

        if (!ticket.isSuccess()) {
            System.out.println("LIST failed: " + ticket.getMessage());
            return;
        }

        System.out.println("150 Opening data connection for LIST");
        try (Socket socket = new Socket(info.getHost(), info.getPort());
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        System.out.println("226 LIST complete");
    }

    private static void handleRetr(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 4) {
            printUsage();
            return;
        }

        String sessionId = args[1];
        String remotePath = args[2];
        Path localPath = Paths.get(args[3]);
        Path parent = localPath.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        PassiveConnectionInfo info = inter.pasv(sessionId);
        TransferTicket ticket = inter.prepareRetr(sessionId, remotePath);

        if (!ticket.isSuccess()) {
            System.out.println("RETR failed: " + ticket.getMessage());
            return;
        }

        System.out.println("150 Opening data connection for RETR");
        try (Socket socket = new Socket(info.getHost(), info.getPort());
             InputStream in = socket.getInputStream();
             OutputStream out = Files.newOutputStream(localPath)) {
            copy(in, out);
        }
        System.out.println("226 RETR complete -> " + localPath.toAbsolutePath().normalize());
    }

    private static void handleStor(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 4) {
            printUsage();
            return;
        }

        String sessionId = args[1];
        Path localPath = Paths.get(args[2]).toAbsolutePath().normalize();
        String remotePath = args[3];

        if (!Files.exists(localPath) || Files.isDirectory(localPath)) {
            System.out.println("STOR failed: local file not found");
            return;
        }

        PassiveConnectionInfo info = inter.pasv(sessionId);
        TransferTicket ticket = inter.prepareStor(sessionId, remotePath);

        if (!ticket.isSuccess()) {
            System.out.println("STOR failed: " + ticket.getMessage());
            return;
        }

        System.out.println("150 Opening data connection for STOR");
        try (Socket socket = new Socket(info.getHost(), info.getPort());
             InputStream in = Files.newInputStream(localPath);
             OutputStream out = socket.getOutputStream()) {
            copy(in, out);
            socket.shutdownOutput();
        }
        System.out.println("226 STOR complete -> " + remotePath);
    }

    private static void handleClosePasv(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            return;
        }
        inter.closePassiveChannel(args[1]);
        System.out.println("Passive channel closed");
    }

    private static void handleLogout(FtpControlInterface inter, String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            return;
        }
        inter.logout(args[1]);
        System.out.println("Logged out");
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }
}
