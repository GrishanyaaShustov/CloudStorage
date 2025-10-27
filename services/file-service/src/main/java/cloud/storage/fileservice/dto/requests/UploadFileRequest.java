package cloud.storage.fileservice.dto.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
public class UploadFileRequest {
    private MultipartFile file;
    private Long folderId;
}
