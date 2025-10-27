package cloud.storage.fileservice.services.S3Services;

import java.io.InputStream;

public interface S3Service {
    InputStream downloadFile(String key);

    void uploadFile(String key, InputStream stream, long size, String contentType);

    void deleteFile(String key);

    void copyFile(String key, String destinationKey);
}
