package cloud.storage.userservice.DTO.requests.folderRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeleteFolderRequest {
    private Long deleteFolderId;
}
