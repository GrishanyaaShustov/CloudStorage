package cloud.storage.userservice.dto.requests.folderRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReplaceFolderRequest {
    private Long replaceFolderId;
    private Long replacedFolderId;
}
