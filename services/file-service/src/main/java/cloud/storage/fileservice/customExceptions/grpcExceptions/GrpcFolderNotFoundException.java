package cloud.storage.fileservice.customExceptions.grpcExceptions;

public class GrpcFolderNotFoundException extends RuntimeException {
    public GrpcFolderNotFoundException(String message) {
        super(message);
    }
}
