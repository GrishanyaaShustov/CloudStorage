package cloud.storage.fileservice.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DownloadFileResponse {
    private String fileName;
    private String contentType;
    private long size;
    private String s3Key;
}
