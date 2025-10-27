package cloud.storage.fileservice.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeleteFileResponse {
    private boolean isSuccess;
    private String message;
}
