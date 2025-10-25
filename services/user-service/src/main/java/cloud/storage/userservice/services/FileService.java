package cloud.storage.userservice.services;

import cloud.storage.userservice.DTO.requests.fileRequests.*;
import cloud.storage.userservice.DTO.responses.fileResponses.*;

import java.security.Principal;

public interface FileService {

    UploadFileResponse uploadFile(UploadFileRequest request, Principal principal) throws SecurityException;

    CopyFileResponse copyFile(CopyFileRequest request, Principal principal) throws SecurityException;

    ReplaceFileResponse replaceFile(ReplaceFileRequest request, Principal principal) throws SecurityException;

    DeleteFileResponse deleteFile(DeleteFileRequest request, Principal principal) throws SecurityException;

    GetFilesInDirectoryResponse getFilesInDirectory(GetFilesInDirectoryRequest request, Principal principal) throws SecurityException;

    DownloadFileResponse downloadFile(DownloadFileRequest request, Principal principal) throws SecurityException;

    RenameFileResponse renameFile(RenameFileRequest request, Principal principal) throws SecurityException;
}
