package cloud.storage.fileservice.services;

import cloud.storage.fileservice.configuration.jwt.CustomUserPrincipal;
import cloud.storage.fileservice.customExceptions.*;
import cloud.storage.fileservice.models.File;
import cloud.storage.fileservice.models.Folder;
import cloud.storage.fileservice.models.User;
import cloud.storage.fileservice.repository.FileRepository;
import cloud.storage.fileservice.repository.FolderRepository;
import cloud.storage.fileservice.services.grpc.FolderGrpcClient;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HelperService {

    private final FileRepository fileRepository;

    private final Tika tika = new Tika();
    private final FolderGrpcClient folderGrpcClient;

    public User validateAndGetUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token &&
                token.getPrincipal() instanceof CustomUserPrincipal userPrincipal) {
            return User.builder()
                    .id(userPrincipal.getId())
                    .email(userPrincipal.getEmail())
                    .build();
        }
        throw new AccessDeniedException("Invalid authentication principal");
    }

    public Folder validateAndGetFolder(User user, Long folderId) {
        if (folderId == null) return null;

        userservice.Folder.getFolderFolderDataResponse folderDataResponse = folderGrpcClient.getFolderData(folderId, user.getId());
        if (folderDataResponse.getUserId() != user.getId()) throw new AccessDeniedException("Access denied to this folder");

        Folder.FolderBuilder folderBuilder = Folder.builder()
                .id(folderDataResponse.getId())
                .name(folderDataResponse.getName())
                .user(user);

        if(folderDataResponse.getParentId() != 0){
            Folder parent = Folder.builder()
                    .id(folderDataResponse.getParentId())
                    .build();
            folderBuilder.parent(parent);
        }

        return folderBuilder.build();

    }

    public File validateAndGetFile(User user, Long fileId) {
        File file = fileRepository.findFileById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File with that id does not exist"));

        if (!file.getUser().getId().equals(user.getId()))
            throw new AccessDeniedException("Access denied to this file");

        return file;
    }

    public void validateFileNotEmpty(MultipartFile file){
        if(file == null || file.isEmpty()){
            throw new FileUploadException("No file provided for upload", null);
        }
    }

    public void validateFileNameUniq(User user, Folder folder, String fileName) {
        if (fileRepository.existsByNameAndFolderAndUser(fileName, folder, user))
            throw new FileAlreadyExistsException("File with that name already exists");
    }

    public String generateS3Key(String fileName) {
        return UUID.randomUUID() + "_" + fileName;
    }

    public String detectMimeType(String fileName) {
        try {
            return tika.detect(fileName);
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    public String detectMimeType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

}
