import java.nio.file.Path;
import java.nio.file.Paths;

public class UserService 
{
    private final Path baseRoot;

    public UserService(String baseRootPath)
    {
        this.baseRoot = Paths.get(baseRootPath).toAbsolutePath().normalize();
    }

    public boolean authenticate(String username, String password) 
    {
        return "admin".equals(username) && "1234".equals(password);
    }

    public Path getUserRoot(String username) {
        return baseRoot.resolve(username).normalize();
    }
}
