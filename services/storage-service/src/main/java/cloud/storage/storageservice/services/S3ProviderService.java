package cloud.storage.storageservice.services;

import java.io.InputStream;

public interface S3ProviderService {

    void uploadFile(String key, InputStream stream, long size, String contentType);

    void deleteFile(String key);

    void copyFile(String key, String destinationKey);

    InputStream downloadFile(String key);
}
