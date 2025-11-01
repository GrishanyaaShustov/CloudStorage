package cloud.storage.fileservice.customExceptions.grpcExceptions;

public class GrpcFileNotFoundException extends RuntimeException {
    public GrpcFileNotFoundException(String message) {
        super(message);
    }
}
