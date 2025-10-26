package cloud.storage.storageservice.services;

import cloud.storage.grpc.UploadFileResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.*;
import java.util.concurrent.*;

public class MultipartUploader {

    private static final Logger log = LoggerFactory.getLogger(MultipartUploader.class);

    private final ExecutorService executor;
    private final Semaphore semaphore;

    public MultipartUploader(int parallelism) {
        int maxThreads = Math.min(parallelism, Runtime.getRuntime().availableProcessors());
        this.executor = Executors.newFixedThreadPool(maxThreads);
        this.semaphore = new Semaphore(maxThreads);
    }

    public UploadSession startSession(
            String bucket,
            String key,
            String contentType,
            S3Client s3Client,
            StreamObserver<UploadFileResponse> responseObserver
    ) {
        return new UploadSession(bucket, key, contentType, s3Client, responseObserver, executor, semaphore);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
