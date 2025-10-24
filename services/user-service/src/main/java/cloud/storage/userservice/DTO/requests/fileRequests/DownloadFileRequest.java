package cloud.storage.userservice.DTO.requests.fileRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DownloadFileRequest {
    private Long fileId;
}
