import java.io.Serializable;

public class TransferTicket implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String transferId;
    private final String operation;
    private final String normalizedPath;
    private final boolean success;
    private final String message;

    public TransferTicket(String transferId, String operation, String normalizedPath, boolean success, String message) {
        this.transferId = transferId;
        this.operation = operation;
        this.normalizedPath = normalizedPath;
        this.success = success;
        this.message = message;
    }

    public static TransferTicket ok(String transferId, String operation, String normalizedPath, String message) {
        return new TransferTicket(transferId, operation, normalizedPath, true, message);
    }

    public static TransferTicket fail(String operation, String normalizedPath, String message) {
        return new TransferTicket(null, operation, normalizedPath, false, message);
    }

    public String getTransferId() {
        return transferId;
    }

    public String getOperation() {
        return operation;
    }

    public String getNormalizedPath() {
        return normalizedPath;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
