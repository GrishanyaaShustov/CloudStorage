package cloud.storage.userservice.services;

import cloud.storage.userservice.dto.requests.CreateFolderRequest;
import cloud.storage.userservice.dto.response.CreateFolderResponse;
import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.models.User;
import cloud.storage.userservice.repository.FolderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final FolderRepository folderRepository;

    private final HelperService helperService;

    @Override
    @Transactional
    public CreateFolderResponse createFolder(CreateFolderRequest request, Principal principal) {
        User user = helperService.validateAndGetUser(principal);

        Folder parentFolder = helperService.findFolderById(user, request.getParentId());
        helperService.validateFolderNameUniq(user, request.getFolderName(), parentFolder);

        folderRepository.save(Folder.builder()
                .user(user)
                .name(request.getFolderName())
                .parent(parentFolder)
                .build());

        return new CreateFolderResponse("Folder created successfully");
    }
}
