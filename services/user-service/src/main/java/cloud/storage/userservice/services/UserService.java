package cloud.storage.userservice.services;

import cloud.storage.userservice.dto.requests.CreateFolderRequest;
import cloud.storage.userservice.dto.response.CreateFolderResponse;

import java.security.Principal;

public interface UserService {
    CreateFolderResponse createFolder(CreateFolderRequest request, Principal principal);
}
