package cloud.storage.fileservice.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MoveFileRequest {
    private Long fileId;
    private Long folderId;
}

