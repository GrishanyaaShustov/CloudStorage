package cloud.storage.userservice.services;

import cloud.storage.userservice.DTO.requests.folderRequests.*;
import cloud.storage.userservice.DTO.responses.folderResponses.*;
import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.models.User;
import cloud.storage.userservice.repository.FolderRepository;
import cloud.storage.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;

@AllArgsConstructor
@Service
public class FolderServiceImplementation implements FolderService{

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;

    @Override
    @Transactional
    public CreateFolderResponse createFolder(CreateFolderRequest request, Principal principal) throws SecurityException {
        User user = userRepository.findUserByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("404.User does not exist"));

        Folder parent = null;
        if(request.getParentId() != null) {
            parent = folderRepository.findFolderById(request.getParentId()).orElseThrow(() -> new IllegalArgumentException("404.Directory with that id does not exist"));
            if (!parent.getUser().getId().equals(user.getId())) throw new SecurityException("403.Access denied");
        }

        if(folderRepository.existsByNameAndParentAndUser(request.getFolderName(), parent, user)) throw new IllegalArgumentException("400.Folder with this name already exists");

        Folder folder = Folder.builder()
                .name(request.getFolderName())
                .user(user)
                .parent(parent)
                .build();

        folderRepository.save(folder);

        return new CreateFolderResponse("Folder created successfully");
    }

    @Override
    public CopyFolderResponse copyFolder(CopyFolderRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public ReplaceFolderResponse replaceFolder(ReplaceFolderRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public DeleteFolderResponse deleteFolder(DeleteFolderRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public GetFolderListInDirectoryResponse getFolderListInDirectory(GetFolderListInDirectoryRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public RenameFolderResponse renameFolder(RenameFolderRequest request, Principal principal) throws SecurityException {
        return null;
    }
}
