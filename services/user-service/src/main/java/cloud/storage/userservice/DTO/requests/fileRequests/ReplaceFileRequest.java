package cloud.storage.userservice.DTO.requests.fileRequests;

import lombok.Data;

@Data
public class ReplaceFileRequest {
    private Long fileId;
    private Long targetFolderId;
}
