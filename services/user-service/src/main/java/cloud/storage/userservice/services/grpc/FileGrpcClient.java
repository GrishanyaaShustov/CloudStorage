package cloud.storage.userservice.services.grpc;

import cloud.storage.userservice.customExceptions.grpcExceptions.GrpcFileNotFoundException;
import cloud.storage.userservice.customExceptions.grpcExceptions.GrpcAccessDeniedException;
import fileservice.FileServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import fileservice.File.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileGrpcClient {
    private final FileServiceGrpc.FileServiceBlockingStub fileStub;

    public DeleteFileResponse deleteFile(Long fileId) {
        DeleteFileRequest request = DeleteFileRequest.newBuilder()
                .setFileId(fileId)
                .build();

        try {
            return fileStub.deleteFile(request);
        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();

            switch (code) {
                case NOT_FOUND -> {
                    log.warn("File not found (id={}): {}", fileId, e.getMessage());
                    throw new GrpcFileNotFoundException("File not found");
                }
                case PERMISSION_DENIED -> {
                    log.warn("Access denied to file (id={}): {}", fileId, e.getMessage());
                    throw new GrpcAccessDeniedException("Access denied to this file");
                }
                default -> {
                    log.error("Unexpected gRPC error: {}", e.getMessage(), e);
                    throw new RuntimeException("Unexpected gRPC error: " + e.getMessage(), e);
                }
            }
        } catch (Exception e){
            log.error("Unexpected error fetching file: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot fetch file", e);
        }
    }
}
