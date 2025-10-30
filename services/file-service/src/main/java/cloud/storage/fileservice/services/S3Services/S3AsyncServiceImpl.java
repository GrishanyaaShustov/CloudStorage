package cloud.storage.fileservice.services.S3Services;

import cloud.storage.fileservice.configuration.S3Configuration;
import cloud.storage.fileservice.customExceptions.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3AsyncServiceImpl implements S3AsyncService {

    private final S3Configuration s3Configuration;
    private final S3AsyncClient s3AsyncClient;

    private static final long PART_SIZE = 50L * 1024 * 1024; // 50 MB

    @Override
    public Mono<Void> uploadStreamInParts(String key, Flux<DataBuffer> dataStream, String contentType) {
        String bucket = s3Configuration.getBucket();

        // 1 Создание multipart upload
        Mono<CreateMultipartUploadResponse> createUploadMono =
                Mono.fromFuture(() -> s3AsyncClient.createMultipartUpload(
                        CreateMultipartUploadRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(contentType)
                                .build()
                ));

        // Хранилище загруженных частей
        Queue<CompletedPart> completedParts = new ConcurrentLinkedQueue<>();

        return createUploadMono.flatMap(createResp -> {
            String uploadId = createResp.uploadId();

            // 2Разбиваем поток DataBuffer на байтовые блоки
            Flux<PartChunk> partFlux = chunkDataBuffers(dataStream)
                    .index()
                    .map(tuple -> new PartChunk(tuple.getT1().intValue() + 1, tuple.getT2()));

            // Отправляем части в S3
            return partFlux.flatMap(partChunk -> {
                        UploadPartRequest uploadRequest = UploadPartRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .uploadId(uploadId)
                                .partNumber(partChunk.partNumber())
                                .build();

                        return Mono.fromFuture(() ->
                                s3AsyncClient.uploadPart(uploadRequest, AsyncRequestBody.fromBytes(partChunk.data()))
                        ).map(uploadResp -> {
                            completedParts.add(CompletedPart.builder()
                                    .partNumber(partChunk.partNumber())
                                    .eTag(uploadResp.eTag())
                                    .build());
                            return uploadResp;
                        });
                    }, 10) // ограничение параллельности
                    .publishOn(Schedulers.parallel()) // ускоряем операции по сети
                    // Завершение загрузки
                    .then(Mono.fromFuture(() ->
                            s3AsyncClient.completeMultipartUpload(
                                    CompleteMultipartUploadRequest.builder()
                                            .bucket(bucket)
                                            .key(key)
                                            .uploadId(uploadId)
                                            .multipartUpload(CompletedMultipartUpload.builder()
                                                    .parts(completedParts.stream()
                                                            .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                                                            .toList())
                                                    .build())
                                            .build()
                            )
                    ))
                    // При ошибке — прерывание multipart upload
                    .onErrorResume(ex -> {
                        // Сначала прерываем загрузку
                        return Mono.fromFuture(() ->
                                        s3AsyncClient.abortMultipartUpload(
                                                AbortMultipartUploadRequest.builder()
                                                        .bucket(bucket)
                                                        .key(key)
                                                        .uploadId(uploadId)
                                                        .build()
                                        )
                                )
                                .doOnSuccess(r -> log.warn("Multipart upload aborted for key: {}", key))
                                .then(Mono.error(new S3UploadException("S3 multipart upload failed", ex)));
                    })
                    .then();
        });
    }

    // Вспомогательная структура для части
    private record PartChunk(int partNumber, byte[] data) {}

    //  Реактивная агрегация DataBuffer в блоки заданного размера
    private Flux<byte[]> chunkDataBuffers(Flux<DataBuffer> source) {
        return source
                .publishOn(Schedulers.boundedElastic())
                .flatMapSequential(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return Flux.just(bytes);
                })
                .bufferUntil(accumulateOver())
                .map(this::mergeBuffers);
    }

    //  Функция, чтобы разбивать поток, когда накопленный размер >= chunkSize
    private java.util.function.Predicate<byte[]> accumulateOver() {
        AtomicLong counter = new AtomicLong();
        return bytes -> {
            long current = counter.addAndGet(bytes.length);
            if (current >= S3AsyncServiceImpl.PART_SIZE) {
                counter.set(0);
                return true;
            }
            return false;
        };
    }

    //  Объединение нескольких байтовых массивов в один
    private byte[] mergeBuffers(List<byte[]> buffers) {
        int total = buffers.stream().mapToInt(b -> b.length).sum();
        byte[] combined = new byte[total];
        int pos = 0;
        for (byte[] b : buffers) {
            System.arraycopy(b, 0, combined, pos, b.length);
            pos += b.length;
        }
        return combined;
    }
}