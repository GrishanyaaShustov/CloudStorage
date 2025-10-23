package cloud.storage.storageservice.services;

import cloud.storage.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@GrpcService
@RequiredArgsConstructor
public class StorageGrpcService extends StorageServiceGrpc.StorageServiceImplBase{
    private final S3ProviderService s3ProviderService;

    @Override
    public void uploadFile(UploadFileRequest request, StreamObserver<UploadFileResponse> responseObserver){
        try {
            s3ProviderService.uploadFile(
                    request.getKey(),
                    new ByteArrayInputStream(request.getFileData().toByteArray()),
                    request.getFileData().size(),
                    request.getContentType()
            );
            responseObserver.onNext(UploadFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File uploaded successfully")
                    .build());
        }
        catch (Exception e){
            responseObserver.onNext(UploadFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
        }
        finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver){
        try {
            s3ProviderService.deleteFile(request.getKey());
            responseObserver.onNext(DeleteFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File deleted successfully")
                    .build());
        }
        catch (Exception e){
            responseObserver.onNext(DeleteFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
        }
        finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void copyFile(CopyFileRequest request, StreamObserver<CopyFileResponse> responseObserver) {
        try {
            s3ProviderService.copyFile(request.getKey(), request.getDestinationKey());
            responseObserver.onNext(CopyFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File copied successfully")
                    .build());
        }
        catch (Exception e) {
            responseObserver.onNext(CopyFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error copying file: " + e.getMessage())
                    .build());
        }
        finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void downloadFile(DownloadFileRequest request, StreamObserver<DownloadFileResponse> responseObserver) {
        try {
            InputStream inputStream = s3ProviderService.downloadFile(request.getKey());

            // Читаем InputStream в byte[]
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] fileBytes = buffer.toByteArray();

            responseObserver.onNext(DownloadFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File downloaded successfully")
                    .setFileData(ByteString.copyFrom(fileBytes))
                    .build());

        }
        catch (Exception e) {
            responseObserver.onNext(DownloadFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error downloading file: " + e.getMessage())
                    .build());
        }
        finally {
            responseObserver.onCompleted();
        }
    }
}
