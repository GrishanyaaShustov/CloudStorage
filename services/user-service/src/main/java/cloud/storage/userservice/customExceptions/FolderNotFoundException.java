package cloud.storage.userservice.customExceptions;

public class FolderNotFoundException extends RuntimeException {
    public FolderNotFoundException(String message) {
        super(message);
    }
}
