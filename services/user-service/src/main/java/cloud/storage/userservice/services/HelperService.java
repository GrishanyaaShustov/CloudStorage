package cloud.storage.userservice.services;

import cloud.storage.userservice.configuration.jwt.CustomUserPrincipal;
import cloud.storage.userservice.customExceptions.AccessDeniedException;
import cloud.storage.userservice.customExceptions.FolderAlreadyExistsException;
import cloud.storage.userservice.customExceptions.FolderNotFoundException;
import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.models.User;
import cloud.storage.userservice.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@RequiredArgsConstructor
public class HelperService {

    private final FolderRepository folderRepository;

    public User validateAndGetUser(Principal principal){
        if(principal instanceof UsernamePasswordAuthenticationToken token &&
            token.getPrincipal() instanceof CustomUserPrincipal userPrincipal){
            return User.builder()
                    .id(userPrincipal.getId())
                    .email(userPrincipal.getEmail())
                    .build();
        }
        throw new AccessDeniedException("Invalid authentication principal");
    }

    public Folder findFolderById(User user, Long folderId){
        if(folderId == null) return null;
        Folder folder = folderRepository.findFolderById(folderId).orElseThrow(() -> new FolderNotFoundException("Folder not found"));
        if(!folder.getUser().getId().equals(user.getId())) throw new AccessDeniedException("Access denied");
        return folder;
    }

    public void validateFolderNameUniq(User user, String folderName, Folder parentFolder){
        if(folderRepository.existsByNameAndParentAndUser(folderName, parentFolder, user))
            throw new FolderAlreadyExistsException("Folder with that name already exist");
    }
}
