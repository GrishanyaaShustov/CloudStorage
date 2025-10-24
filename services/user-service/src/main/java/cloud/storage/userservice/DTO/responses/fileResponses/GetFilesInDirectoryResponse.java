package cloud.storage.userservice.DTO.responses.fileResponses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GetFilesInDirectoryResponse {
    private Map<String, Long> fileMap;
}