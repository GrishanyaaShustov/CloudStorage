package cloud.storage.fileservice.services;

import cloud.storage.fileservice.models.Folder;
import cloud.storage.fileservice.models.User;
import cloud.storage.fileservice.repository.FileRepository;
import cloud.storage.fileservice.repository.UserRepository;
import cloud.storage.fileservice.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HelperService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;

    public User validateAndGetUser(Principal principal) throws IllegalArgumentException{
        return userRepository.findUserByEmail(principal.getName()).orElseThrow(() -> new IllegalArgumentException("404.User does not exist"));
    }

    public Folder validateAndGetFolder(User user, Long folderId) throws IllegalArgumentException, SecurityException{
        Folder folder = null;
        if(folderId != null){
            folder = folderRepository.findFolderById(folderId).orElseThrow(() -> new IllegalArgumentException("404.Folder does not exist"));
            if(!folder.getUser().getId().equals(user.getId())) throw new SecurityException("401.Access denied");
        }
        return folder;
    }

    public void validateFileNameUniq(User user, Folder folder, String fileName) throws IllegalArgumentException{
        if(fileRepository.existsByNameAndFolderAndUser(fileName, folder, user)) throw new IllegalArgumentException("400.File with that name already exist");
    }

    public String generateS3Key(String fileName){
        return UUID.randomUUID() + "_" + fileName;
    }
}
