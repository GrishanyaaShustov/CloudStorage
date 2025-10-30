package cloud.storage.fileservice.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RenameFileRequest {
    private Long fileId;
    private String newName;
}
