package cloud.storage.userservice.DTO.responses.folderResponses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GetFolderListInDirectoryResponse {
    private Map<String, Long> folderMap;
}
