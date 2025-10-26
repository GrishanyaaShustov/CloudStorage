package cloud.storage.userservice.services;

import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.models.User;
import cloud.storage.userservice.repository.FileRepository;
import cloud.storage.userservice.repository.FolderRepository;
import cloud.storage.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HelperService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;

    public User validateAndGetUser(Principal principal){
        return userRepository.findUserByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("404.User does not exists"));
    }

    public Folder validateAndGetFolder(Long folderId, User user){
        if(folderId == null) return  null;

        Folder folder = folderRepository.findFolderById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("404.Folder does not exist"));

        if(!folder.getUser().getId().equals(user.getId())) throw new SecurityException("401.Access denied");

        return folder;
    }

    public void validateFileNameUniqueness(String fileName, Folder folder,  User user){
        if (fileRepository.existsByNameAndFolderAndUser(fileName, folder, user)) {
            throw new IllegalArgumentException("400.File with that name already exists");
        }
    }

    public void validateFileNotEmpty(MultipartFile file){
        if(file.isEmpty()) throw new IllegalArgumentException("404.File is empty");
    }

    public String buildS3Key(String fileName){
        return UUID.randomUUID() + "_" + fileName;
    }

}
