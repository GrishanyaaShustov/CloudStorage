package cloud.storage.userservice.customExceptions.grpcExceptions;

public class FolderAccessDeniedException extends RuntimeException {
    public FolderAccessDeniedException(String message) {
        super(message);
    }
}
