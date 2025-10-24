package cloud.storage.userservice.DTO.requests.fileRequests;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadFileRequest {
    private String fileName;
    private Long folderId;
    private String contentType;
    private long size;
    private java.io.InputStream fileStream;
}
