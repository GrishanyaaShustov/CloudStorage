package cloud.storage.userservice.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CopyFolderRequest {
    private Long copiedFolderId;
    private Long copyFolderId;
}
