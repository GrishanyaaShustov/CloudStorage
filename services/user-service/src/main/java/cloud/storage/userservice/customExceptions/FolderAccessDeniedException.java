package cloud.storage.userservice.customExceptions;

public class FolderAccessDeniedException extends RuntimeException {
    public FolderAccessDeniedException(String message) {
        super(message);
    }
}
