package cloud.storage.storageservice.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface S3ProviderService {

    InputStream downloadFile(String key);

    void uploadFile(String key, InputStream stream, long size, String contentType);

    void deleteFile(String key);

    void copyFile(String key, String destinationKey);

    void uploadFileByChunks(String key, String contentType, List<byte[]> chunks) throws IOException;
}
