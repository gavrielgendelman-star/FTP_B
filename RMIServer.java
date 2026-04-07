import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIServer {
    static int portnumber;
    static String start = "start";

    public static void main(String[] args) {
        try {
            if (args.length < 2 || !start.equals(args[0])) {
                System.out.println("Usage: java RMIServer start <port>");
                return;
            }

            portnumber = Integer.parseInt(args[1]);
            String storageRoot = System.getenv("SERVER_STORAGE");
            if (storageRoot == null || storageRoot.trim().isEmpty()) {
                storageRoot = "ServerStorage";
            }

            Registry reg = LocateRegistry.createRegistry(portnumber);

            SessionManager sessionManager = new SessionManager();
            FileService fileService = new FileService();
            UserService userService = new UserService(storageRoot);
            DataTransferService dataTransferService = new DataTransferService();

            FtpControlImplementation control =
                    new FtpControlImplementation(sessionManager, fileService, userService, dataTransferService);

            reg.bind("remoteObject", control);

            System.out.println("Server is ready.");
            System.out.println("Port: " + portnumber);
            System.out.println("Storage root: " + storageRoot);

        } catch (Exception e) {
            System.out.println("Server failed: " + e);
            e.printStackTrace();
        }
    }
}
