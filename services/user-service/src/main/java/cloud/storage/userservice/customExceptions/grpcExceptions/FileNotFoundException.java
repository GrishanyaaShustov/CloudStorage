package cloud.storage.userservice.customExceptions.grpcExceptions;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String message) {
        super(message);
    }
}
