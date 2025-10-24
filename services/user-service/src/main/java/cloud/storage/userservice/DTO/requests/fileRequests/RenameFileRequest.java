package cloud.storage.userservice.DTO.requests.fileRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RenameFileRequest {
    private Long fileId;
    private String newFileName;
}
