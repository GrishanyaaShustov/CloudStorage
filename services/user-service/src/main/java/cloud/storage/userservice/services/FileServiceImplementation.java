package cloud.storage.userservice.services;

import cloud.storage.userservice.DTO.requests.fileRequests.*;
import cloud.storage.userservice.DTO.responses.fileResponses.*;
import cloud.storage.userservice.configuration.GrpcServerProperties;
import cloud.storage.userservice.configuration.JwtClientInterceptor;
import cloud.storage.userservice.repository.FileRepository;
import cloud.storage.userservice.repository.FolderRepository;
import cloud.storage.userservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
@AllArgsConstructor
public class FileServiceImplementation implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImplementation.class);

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;

    private final JwtClientInterceptor jwtClientInterceptor;
    private final GrpcServerProperties grpcServerProperties;

    @Override
    public UploadFileResponse uploadFile(UploadFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public CopyFileResponse copyFile(CopyFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public ReplaceFileResponse replaceFile(ReplaceFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public DeleteFileResponse deleteFile(DeleteFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public GetFilesInDirectoryResponse getFilesInDirectory(GetFilesInDirectoryRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public DownloadFileResponse downloadFile(DownloadFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public RenameFileResponse renameFile(RenameFileRequest request, Principal principal) throws SecurityException {
        return null;
    }
}
