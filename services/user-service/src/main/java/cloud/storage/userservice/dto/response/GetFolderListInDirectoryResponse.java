package cloud.storage.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GetFolderListInDirectoryResponse {
    private Map<String, Long> folderMap;
}
