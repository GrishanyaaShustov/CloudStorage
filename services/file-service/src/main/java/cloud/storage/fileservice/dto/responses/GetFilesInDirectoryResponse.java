package cloud.storage.fileservice.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GetFilesInDirectoryResponse {
    private Map<String, Long> filesMap;// хэш мап: имя файла -> айди
    private String message;
}
