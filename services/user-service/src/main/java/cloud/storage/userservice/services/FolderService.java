package cloud.storage.userservice.services;

import cloud.storage.userservice.DTO.requests.folderRequests.*;
import cloud.storage.userservice.DTO.responses.folderResponses.*;

import java.security.Principal;

public interface FolderService {
    CreateFolderResponse createFolder(CreateFolderRequest request, Principal principal) throws SecurityException;
    CopyFolderResponse copyFolder(CopyFolderRequest request, Principal principal) throws SecurityException;
    ReplaceFolderResponse replaceFolder(ReplaceFolderRequest request, Principal principal) throws SecurityException;
    DeleteFolderResponse deleteFolder(DeleteFolderRequest request, Principal principal) throws SecurityException;
    GetFolderListInDirectoryResponse getFolderListInDirectory(GetFolderListInDirectoryRequest request, Principal principal) throws SecurityException;
    RenameFolderResponse renameFolder(RenameFolderRequest request, Principal principal) throws SecurityException;
}
