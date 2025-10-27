package cloud.storage.fileservice.services.S3Services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;

public interface S3AsyncService {
    Mono<Void> uploadStreamInParts(String key, Flux<DataBuffer> dataStream, String contentType);
}
