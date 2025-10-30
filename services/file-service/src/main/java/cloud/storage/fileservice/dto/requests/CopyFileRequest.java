package cloud.storage.fileservice.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CopyFileRequest {
    private Long fileId;
    private Long targetFolderId;
}
