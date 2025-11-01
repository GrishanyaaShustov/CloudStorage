package cloud.storage.fileservice.customExceptions.grpcExceptions;

public class GrpcAccessDeniedException extends RuntimeException {
    public GrpcAccessDeniedException(String message) {
        super(message);
    }
}
