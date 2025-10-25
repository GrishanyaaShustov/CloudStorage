package cloud.storage.userservice.DTO.responses.fileResponses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class UploadFileResponse {
    private String message;
    private boolean isSuccess;
}
