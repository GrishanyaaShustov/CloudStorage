package cloud.storage.userservice.DTO.requests.folderRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateFolderRequest {
    private String folderName;
    private Long parentId;
}
