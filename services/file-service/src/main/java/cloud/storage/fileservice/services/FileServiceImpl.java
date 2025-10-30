package cloud.storage.fileservice.services;

import cloud.storage.fileservice.customExceptions.S3UploadException;
import cloud.storage.fileservice.dto.requests.*;
import cloud.storage.fileservice.dto.responses.*;
import cloud.storage.fileservice.models.Folder;
import cloud.storage.fileservice.models.User;
import cloud.storage.fileservice.repository.FileRepository;
import cloud.storage.fileservice.services.S3Services.S3AsyncService;
import cloud.storage.fileservice.services.S3Services.S3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private final HelperService helperService;
    private final S3AsyncService s3AsyncService;
    private final S3Service s3Service;

    private final FileRepository fileRepository;

    // 100 MB
    private static final long MULTIPART_THRESHOLD = 100L * 1024 * 1024;

    @Override
    @Transactional
    public UploadFileResponse uploadFile(UploadFileRequest request, Principal principal) {
        MultipartFile file = request.getFile();
        helperService.validateFileNotEmpty(file);
        User user = helperService.validateAndGetUser(principal);
        Folder folder = helperService.validateAndGetFolder(user, request.getFolderId());
        helperService.validateFileNameUniq(user, folder, file.getOriginalFilename());
        String s3Key = helperService.generateS3Key(file.getOriginalFilename());

        try {
            fileRepository.save(cloud.storage.fileservice.models.File.builder()
                    .name(file.getOriginalFilename())
                    .s3Key(s3Key)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .user(user)
                    .folder(folder)
                    .build());

            // Создаем временный файл
            File tempFile = File.createTempFile("upload-", ".tmp");
            file.transferTo(tempFile);

            // Асинхронная загрузка в фоне
            uploadFileAsync(s3Key, tempFile, file.getContentType())
                    .doFinally(signal -> tempFile.delete())
                    .doOnError(e -> log.error("Ошибка при загрузке файла в S3: {}", e.getMessage()))
                    .block();

        } catch (Exception e) {
            log.error("Ошибка при подготовке файла для загрузки в S3: {}", e.getMessage(), e);
            throw new S3UploadException("Ошибка при загрузке файла в S3", e);
        }

        return new UploadFileResponse(true);
    }

    @Override
    @Transactional
    public DeleteFileResponse deleteFile(DeleteFileRequest request, Principal principal){
        User user = helperService.validateAndGetUser(principal);
        cloud.storage.fileservice.models.File file = helperService.validateAndGetFile(user, request.getFileId());
        fileRepository.delete(file);
        s3Service.deleteFile(file.getS3Key());
        return new DeleteFileResponse(true, "File delete successfully");
    }

    @Override
    public GetFilesInDirectoryResponse getFiles(GetFilesInDirectoryRequest request, Principal principal) throws SecurityException {
        User user = helperService.validateAndGetUser(principal);
        Folder folder = helperService.validateAndGetFolder(user, request.getFolderId());
        Map<String, Long> fileMap = fileRepository.findFilesByFolder(folder)
                .stream()
                .collect(Collectors.toMap(cloud.storage.fileservice.models.File::getName, cloud.storage.fileservice.models.File::getId));

        return new GetFilesInDirectoryResponse(fileMap, "Received all files in directory id: " + (request.getFolderId() == null ? "root" : request.getFolderId()));
    }

    @Override
    @Transactional
    public MoveFileResponse moveFile(MoveFileRequest request, Principal principal) {
        User user = helperService.validateAndGetUser(principal);
        Folder targetFolder = helperService.validateAndGetFolder(user, request.getFolderId());
        cloud.storage.fileservice.models.File file = helperService.validateAndGetFile(user, request.getFileId());
        helperService.validateFileNameUniq(user, targetFolder, file.getName());
        file.setFolder(targetFolder);
        fileRepository.save(file);
        return new MoveFileResponse("File moved to folder id: " + targetFolder.getId());
    }

    @Override
    @Transactional
    public RenameFileResponse renameFile(RenameFileRequest request, Principal principal) {
        User user = helperService.validateAndGetUser(principal);
        cloud.storage.fileservice.models.File file = helperService.validateAndGetFile(user, request.getFileId());

        String oldName = file.getName(); // исходное имя файла
        String newName = request.getNewName().trim();
        helperService.validateFileNameUniq(user, file.getFolder(), newName);

        // Разбираем старое имя на base + расширение
        int oldDotIndex = oldName.lastIndexOf('.');
        String oldExtension = oldDotIndex != -1 ? oldName.substring(oldDotIndex + 1) : "";

        // Разбираем новое имя на base + расширение
        int newDotIndex = newName.lastIndexOf('.');
        String newBaseName = newDotIndex != -1 ? newName.substring(0, newDotIndex) : newName;
        String newExtension = newDotIndex != -1 ? newName.substring(newDotIndex + 1) : "";

        // Логика по смене расширения
        if (!newExtension.isEmpty() && !newExtension.equalsIgnoreCase(oldExtension)) {
            // Пользователь сменил расширение
            file.setName(newName);
            // Определяем новый MIME type через Tika
            String mimeType = helperService.detectMimeType(newName);
            file.setContentType(mimeType);
        } else if (newExtension.isEmpty()) {
            // Пользователь удалил расширение — сохраняем старое
            file.setName(newBaseName + (oldExtension.isEmpty() ? "" : "." + oldExtension));
        } else {
            // Пользователь сменил только имя, расширение осталось
            file.setName(newBaseName + (oldExtension.isEmpty() ? "" : "." + oldExtension));
        }

        fileRepository.save(file);
        return new RenameFileResponse("File renamed successfully");
    }

    @Override
    @Transactional
    public CopyFileResponse copyFile(CopyFileRequest request, Principal principal) {
        User user = helperService.validateAndGetUser(principal);
        Folder targetFolder = helperService.validateAndGetFolder(user, request.getTargetFolderId());
        cloud.storage.fileservice.models.File file = helperService.validateAndGetFile(user, request.getFileId());
        helperService.validateFileNameUniq(user, targetFolder, file.getName());
        String copyS3Key = helperService.generateS3Key(file.getName());
        s3Service.copyFile(file.getS3Key(), copyS3Key);
        cloud.storage.fileservice.models.File copiedFile = cloud.storage.fileservice.models.File.builder()
                .user(user)
                .name(file.getName())
                .s3Key(copyS3Key)
                .folder(targetFolder)
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
        fileRepository.save(copiedFile);
        return new CopyFileResponse("File copied successfully");
    }

    @Override
    public ResponseEntity<InputStreamResource> downloadFileResponse(Long fileId, Principal principal) {
        User user = helperService.validateAndGetUser(principal);
        cloud.storage.fileservice.models.File file = helperService.validateAndGetFile(user, fileId);

        log.info("Начинается скачивание файла {} пользователем {}", file.getName(), user.getEmail());

        InputStreamResource resource = new InputStreamResource(s3Service.downloadFile(file.getS3Key()));

        String encodedFilename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename)
                .header("Content-Type", file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .contentLength(file.getSize())
                .body(resource);
    }

    private Mono<Void> uploadFileAsync(String s3Key, File file, String contentType) throws Exception {
        long size = file.length();

        // Определяем размер буфера: 10 MB для маленьких файлов, 50 MB для больших
        int bufferSize = size > MULTIPART_THRESHOLD ? 50 * 1024 * 1024 : 10 * 1024 * 1024;

        InputStream inputStream = new FileInputStream(file);

        // Преобразуем InputStream во Flux<DataBuffer>
        var dataBufferFlux = DataBufferUtils.readInputStream(
                () -> inputStream,
                new DefaultDataBufferFactory(),
                bufferSize
        );

        // Отправляем поток в S3
        return s3AsyncService.uploadStreamInParts(s3Key, dataBufferFlux, contentType);
    }
}
