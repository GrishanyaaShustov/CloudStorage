package cloud.storage.fileservice.services;

import cloud.storage.fileservice.dto.requests.*;
import cloud.storage.fileservice.dto.responses.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

public interface FileService {
    UploadFileResponse uploadFile(UploadFileRequest request, Principal principal) throws SecurityException;
    DeleteFileResponse deleteFile(DeleteFileRequest request, Principal principal) throws SecurityException;
    GetFilesInDirectoryResponse getFiles(GetFilesInDirectoryRequest request, Principal principal) throws SecurityException;
    ResponseEntity<InputStreamResource> downloadFileResponse(Long fileId, Principal principal) throws SecurityException;
}
