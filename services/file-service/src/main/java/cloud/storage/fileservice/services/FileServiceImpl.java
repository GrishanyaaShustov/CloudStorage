package cloud.storage.fileservice.services;

import cloud.storage.fileservice.dto.requests.DeleteFileRequest;
import cloud.storage.fileservice.dto.requests.GetFilesInDirectoryRequest;
import cloud.storage.fileservice.dto.requests.UploadFileRequest;
import cloud.storage.fileservice.dto.responses.DeleteFileResponse;
import cloud.storage.fileservice.dto.responses.GetFilesInDirectoryResponse;
import cloud.storage.fileservice.dto.responses.UploadFileResponse;
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
        User user = helperService.validateAndGetUser(principal);
        Folder folder = helperService.validateAndGetFolder(user, request.getFolderId());
        helperService.validateFileNameUniq(user, folder, request.getFile().getOriginalFilename());
        String s3Key = helperService.generateS3Key(request.getFile().getOriginalFilename());
        MultipartFile file = request.getFile();

        try {
            fileRepository.save(cloud.storage.fileservice.models.File.builder()
                    .name(request.getFile().getOriginalFilename())
                    .s3Key(s3Key)
                    .size(request.getFile().getSize())
                    .contentType(request.getFile().getContentType())
                    .user(user)
                    .folder(folder)
                    .build());

            // Создаем временный файл, чтобы он не удалился до окончания загрузки
            File tempFile = File.createTempFile("upload-", ".tmp");
            file.transferTo(tempFile);

            // Запускаем асинхронную загрузку в фоне
            uploadFileAsync(s3Key, tempFile, file.getContentType())
                    .doFinally(signal -> tempFile.delete()) // удаляем temp файл после загрузки
                    .doOnError(e -> log.error("Ошибка при загрузке файла в S3: {}", e.getMessage()))
                    .block();

        } catch (Exception e) {
            log.error("Ошибка при подготовке файла для загрузки в S3: {}", e.getMessage());
            throw new RuntimeException("Ошибка при загрузке файла", e);
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
