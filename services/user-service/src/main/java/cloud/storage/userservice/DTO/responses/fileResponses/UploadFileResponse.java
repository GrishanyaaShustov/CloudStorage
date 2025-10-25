package cloud.storage.userservice.DTO.responses.fileResponses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class UploadFileResponse {
    private  String message;
}
