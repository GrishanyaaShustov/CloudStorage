package cloud.storage.userservice.dto.requests.folderRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RenameFolderRequest {
    private Long renameFolderId;
    private String newFolderName;
}