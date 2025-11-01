package cloud.storage.userservice.customExceptions.grpcExceptions;

public class GrpcAccessDeniedException extends RuntimeException {
    public GrpcAccessDeniedException(String message) {
        super(message);
    }
}
