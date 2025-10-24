package cloud.storage.userservice.DTO.responses.fileResponses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;

@Data
@AllArgsConstructor
public class DownloadFileResponse {
    private String fileName;
    private String contentType;
    private long size;
    private InputStream fileStream;
}
