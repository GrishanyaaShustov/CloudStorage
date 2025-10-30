package cloud.storage.fileservice.services;

import cloud.storage.fileservice.dto.requests.*;
import cloud.storage.fileservice.dto.responses.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

public interface FileService {
    UploadFileResponse uploadFile(UploadFileRequest request, Principal principal);
    DeleteFileResponse deleteFile(DeleteFileRequest request, Principal principal);
    GetFilesInDirectoryResponse getFiles(GetFilesInDirectoryRequest request, Principal principal);
    MoveFileResponse moveFile(MoveFileRequest request, Principal principal);
    ResponseEntity<InputStreamResource> downloadFileResponse(Long fileId, Principal principal);
}
