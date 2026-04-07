import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FtpControlInterface extends Remote {
    String login(String username, String password) throws RemoteException;
    void logout(String sessionId) throws RemoteException;

    String pwd(String sessionId) throws RemoteException;
    boolean cwd(String sessionId, String path) throws RemoteException;
    boolean mkd(String sessionId, String path) throws RemoteException;
    boolean rmd(String sessionId, String path) throws RemoteException;
    boolean dele(String sessionId, String path) throws RemoteException;

    String[] list(String sessionId, String path) throws RemoteException;

    PassiveConnectionInfo pasv(String sessionId) throws RemoteException;
    TransferTicket prepareList(String sessionId, String path) throws RemoteException;
    TransferTicket prepareRetr(String sessionId, String path) throws RemoteException;
    TransferTicket prepareStor(String sessionId, String path) throws RemoteException;
    void closePassiveChannel(String sessionId) throws RemoteException;
}
