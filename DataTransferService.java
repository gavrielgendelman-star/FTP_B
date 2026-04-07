import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class DataTransferService {
    private static final int BUFFER_SIZE = 4096;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void startListTransfer(Session session, Path targetPath, String transferId) {
        PassiveDataContext context = consumePassiveContext(session);
        executor.submit(() -> {
            try (Socket client = context.acceptClient();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
                 Stream<Path> stream = Files.list(targetPath).sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))) {

                for (Path entry : (Iterable<Path>) stream::iterator) {
                    writer.write(formatListEntry(entry));
                    writer.newLine();
                }
                writer.flush();
            } catch (Exception e) {
                System.err.println("LIST transfer failed [" + transferId + "]: " + e.getMessage());
            } finally {
                context.close();
            }
        });
    }

    public void startRetrTransfer(Session session, Path filePath, String transferId) {
        PassiveDataContext context = consumePassiveContext(session);
        executor.submit(() -> {
            try (Socket client = context.acceptClient();
                 InputStream fileIn = Files.newInputStream(filePath);
                 OutputStream socketOut = client.getOutputStream()) {
                copy(fileIn, socketOut);
                socketOut.flush();
            } catch (Exception e) {
                System.err.println("RETR transfer failed [" + transferId + "]: " + e.getMessage());
            } finally {
                context.close();
            }
        });
    }

    public void startStorTransfer(Session session, Path filePath, String transferId) {
        PassiveDataContext context = consumePassiveContext(session);
        executor.submit(() -> {
            try (Socket client = context.acceptClient();
                 InputStream socketIn = client.getInputStream();
                 OutputStream fileOut = Files.newOutputStream(filePath)) {
                copy(socketIn, fileOut);
                fileOut.flush();
            } catch (Exception e) {
                System.err.println("STOR transfer failed [" + transferId + "]: " + e.getMessage());
            } finally {
                context.close();
            }
        });
    }

    private PassiveDataContext consumePassiveContext(Session session) {
        PassiveDataContext context;
        synchronized (session) {
            context = session.clearPassiveContext();
        }

        if (context == null) {
            throw new IllegalStateException("PASV must be called before data transfer");
        }

        if (!context.isReady()) {
            context.close();
            throw new IllegalStateException("Passive data channel is not ready");
        }

        return context;
    }

    private String formatListEntry(Path entry) throws IOException {
        boolean directory = Files.isDirectory(entry);
        long size = directory ? 0L : Files.size(entry);
        String type = directory ? "DIR" : "FILE";
        return type + "\t" + entry.getFileName() + "\t" + size;
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
