package cloud.storage.userservice.DTO.requests.fileRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CopyFileRequest {
    private Long fileId;
    private Long targetFolderId;
}