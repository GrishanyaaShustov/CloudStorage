package cloud.storage.userservice.DTO.requests.fileRequests;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
public class UploadFileRequest {
    private MultipartFile file;
    private Long folderId;
}
