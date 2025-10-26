package cloud.storage.storageservice.services;

import cloud.storage.grpc.UploadFileRequest;
import cloud.storage.grpc.UploadFileResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class UploadSession {

    private static final Logger log = LoggerFactory.getLogger(UploadSession.class);

    private final String bucket;
    private final String key;
    private final S3Client s3Client;
    private final StreamObserver<UploadFileResponse> responseObserver;
    private final ExecutorService executor;
    private final Semaphore semaphore;

    private final Map<Integer, CompletableFuture<CompletedPart>> partFutures = new ConcurrentHashMap<>();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean responseSent = new AtomicBoolean(false);

    private final String uploadId;
    private int partNumber = 1;

    private final AtomicLong totalUploadedBytes = new AtomicLong(0);
    private final AtomicLong totalReceivedBytes = new AtomicLong(0);
    private long lastLoggedProgress = 0;
    private final Object progressLock = new Object();

    public UploadSession(String bucket,
                         String key,
                         String contentType,
                         S3Client s3Client,
                         StreamObserver<UploadFileResponse> responseObserver,
                         ExecutorService executor,
                         Semaphore semaphore) {

        this.bucket = bucket;
        this.key = key;
        this.s3Client = s3Client;
        this.responseObserver = responseObserver;
        this.executor = executor;
        this.semaphore = semaphore;

        CreateMultipartUploadRequest req = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        this.uploadId = s3Client.createMultipartUpload(req).uploadId();

        log.info("Started upload session: key={}, uploadId={}", key, uploadId);
    }

    public void handleChunk(UploadFileRequest chunk) {
        if (completed.get()) return;

        if (!chunk.getChunkData().isEmpty()) {
            //  Учитываем размер полученных данных
            long received = chunk.getChunkData().size();
            totalReceivedBytes.addAndGet(received);
            logProgress(totalUploadedBytes.get()); // лог каждые ~50МБ при приёме
            submitPart(chunk);
        }

        if (chunk.getIsLastChunk() && completed.compareAndSet(false, true)) {
            completeUploadAsync();
        }
    }

    private void logProgress(long uploadedBytes) {
        synchronized (progressLock) {
            // лог каждые 50 МБ
            long progressLogIntervalBytes = 50 * 1024 * 1024;
            if (uploadedBytes - lastLoggedProgress >= progressLogIntervalBytes) {
                lastLoggedProgress = uploadedBytes;

                double uploadedMB = uploadedBytes / 1024.0 / 1024.0;
                double receivedMB = totalReceivedBytes.get() / 1024.0 / 1024.0;
                double progressPercent = (receivedMB > 0)
                        ? (uploadedMB / receivedMB) * 100.0
                        : 0.0;

                log.info(String.format(
                        " Upload progress for key=%s: %.2f MB / %.2f MB (%.1f%%)",
                        key, uploadedMB, receivedMB, progressPercent
                ));
            }
        }
    }

    private void submitPart(UploadFileRequest chunk) {
        final int currentPart = partNumber++;
        final byte[] data = chunk.getChunkData().toByteArray();

        semaphore.acquireUninterruptibly();

        CompletableFuture<CompletedPart> future = CompletableFuture.supplyAsync(() -> {
            try {
                UploadPartRequest req = UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(currentPart)
                        .build();

                UploadPartResponse resp = s3Client.uploadPart(req, RequestBody.fromBytes(data));

                // после успешной загрузки части — увеличиваем счётчик и логируем
                totalUploadedBytes.addAndGet(data.length);
                logProgress(totalUploadedBytes.get());

                return CompletedPart.builder()
                        .partNumber(currentPart)
                        .eTag(resp.eTag())
                        .build();

            } catch (Exception e) {
                throw new CompletionException(e);
            } finally {
                Arrays.fill(data, (byte) 0);
                semaphore.release();
            }
        }, executor);

        partFutures.put(currentPart, future);
    }

    private void completeUploadAsync() {
        CompletableFuture<?>[] futures = partFutures.values().toArray(new CompletableFuture[0]);
        if (futures.length == 0) {
            completeEmpty();
            return;
        }

        CompletableFuture.allOf(futures)
                .thenApply(v -> partFutures.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getValue().join())
                        .collect(Collectors.toList()))
                .thenAccept(this::finishUpload)
                .exceptionally(ex -> {
                    fail(ex);
                    return null;
                });
    }

    private void finishUpload(List<CompletedPart> parts) {
        try {
            CompleteMultipartUploadRequest req = CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    .build();

            s3Client.completeMultipartUpload(req);
            partFutures.clear();
            log.info(" Completed upload for key={} ({} parts)", key, parts.size());
            sendSuccess();
        } catch (Exception e) {
            fail(e);
        }
    }

    private void completeEmpty() {
        try {
            CompleteMultipartUploadRequest req = CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(Collections.emptyList()).build())
                    .build();

            s3Client.completeMultipartUpload(req);
            sendSuccess();
        } catch (Exception e) {
            fail(e);
        }
    }

    public void fail(Throwable t) {
        if (!completed.compareAndSet(false, true)) return;

        log.error("Upload failed for key={}: {}", key, t.getMessage(), t);

        partFutures.values().forEach(f -> f.cancel(true));
        partFutures.clear();

        try {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build());
        } catch (Exception e) {
            log.error("Abort failed for key={}: {}", key, e.getMessage());
        }

        if (responseSent.compareAndSet(false, true)) {
            responseObserver.onNext(UploadFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Upload failed: " + t.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private void sendSuccess() {
        if (responseSent.compareAndSet(false, true)) {
            responseObserver.onNext(UploadFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File uploaded successfully: " + key)
                    .build());
            responseObserver.onCompleted();
        }
    }

    public void finishIfIncomplete() {
        if (!completed.get()) {
            fail(new IllegalStateException("Client closed stream without last chunk"));
        }
    }
}
