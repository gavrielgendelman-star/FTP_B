import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileService {

    public Path resolveSafePath(Session session, String ftpPath) {
        Path root = session.getRootDir().toAbsolutePath().normalize();
        Path current = session.getCurrentDir().toAbsolutePath().normalize();

        Path resolved;

        if (ftpPath == null || ftpPath.trim().isEmpty() || ".".equals(ftpPath.trim())) {
            resolved = current;
        } else if (ftpPath.startsWith("/")) {
            resolved = root.resolve(ftpPath.substring(1)).normalize();
        } else {
            resolved = current.resolve(ftpPath).normalize();
        }

        if (!resolved.startsWith(root)) {
            throw new SecurityException("Access denied: path escapes user root");
        }

        return resolved;
    }

    public String getWorkingDirectory(Session session) {
        Path root = session.getRootDir().toAbsolutePath().normalize();
        Path current = session.getCurrentDir().toAbsolutePath().normalize();

        if (root.equals(current)) {
            return "/";
        }

        Path relative = root.relativize(current);
        return "/" + relative.toString().replace("\\", "/");
    }

    public String toVirtualPath(Session session, Path path) {
        Path root = session.getRootDir().toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();

        if (root.equals(normalized)) {
            return "/";
        }

        Path relative = root.relativize(normalized);
        return "/" + relative.toString().replace("\\", "/");
    }

    public boolean changeWorkingDirectory(Session session, String ftpPath) {
        Path target = resolveSafePath(session, ftpPath);

        if (!Files.exists(target)) {
            return false;
        }

        if (!Files.isDirectory(target)) {
            return false;
        }

        session.setCurrentDir(target);
        return true;
    }

    public boolean makeDirectory(Session session, String ftpPath) {
        Path target = resolveSafePath(session, ftpPath);

        try {
            if (Files.exists(target)) {
                return false;
            }

            Files.createDirectory(target);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteDirectory(Session session, String ftpPath) {
        Path target = resolveSafePath(session, ftpPath);

        try {
            if (!Files.exists(target)) {
                return false;
            }

            if (!Files.isDirectory(target)) {
                return false;
            }

            try (Stream<Path> entries = Files.list(target)) {
                if (entries.findAny().isPresent()) {
                    return false;
                }
            }

            Files.delete(target);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteFile(Session session, String ftpPath) {
        Path target = resolveSafePath(session, ftpPath);

        try {
            if (!Files.exists(target)) {
                return false;
            }

            if (Files.isDirectory(target)) {
                return false;
            }

            Files.delete(target);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String[] list(Session session, String ftpPath) {
        Path target = resolveSafePath(session, ftpPath);

        if (!Files.exists(target) || !Files.isDirectory(target)) {
            return new String[0];
        }

        try (Stream<Path> stream = Files.list(target)) {
            return stream
                    .map(p -> p.getFileName().toString())
                    .toArray(String[]::new);
        } catch (IOException e) {
            return new String[0];
        }
    }
}
