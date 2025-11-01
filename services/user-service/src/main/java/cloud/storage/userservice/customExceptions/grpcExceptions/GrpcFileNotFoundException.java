package cloud.storage.userservice.customExceptions.grpcExceptions;

public class GrpcFileNotFoundException extends RuntimeException {
    public GrpcFileNotFoundException(String message) {
        super(message);
    }
}
