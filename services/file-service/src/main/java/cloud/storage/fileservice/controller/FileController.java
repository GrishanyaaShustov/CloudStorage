package cloud.storage.fileservice.controller;

import cloud.storage.fileservice.dto.requests.DeleteFileRequest;
import cloud.storage.fileservice.dto.requests.GetFilesInDirectoryRequest;
import cloud.storage.fileservice.dto.requests.MoveFileRequest;
import cloud.storage.fileservice.dto.requests.UploadFileRequest;
import cloud.storage.fileservice.dto.responses.DeleteFileResponse;
import cloud.storage.fileservice.dto.responses.GetFilesInDirectoryResponse;
import cloud.storage.fileservice.dto.responses.MoveFileResponse;
import cloud.storage.fileservice.dto.responses.UploadFileResponse;
import cloud.storage.fileservice.services.FileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    /**
     * Загрузка файла в S3 и сохранение метаданных в БД
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadFileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "folderId", required = false) Long folderId,
            Principal principal
    ) {
        log.info("Запрос на загрузку файла пользователем {}", principal.getName());
        return ResponseEntity.ok(fileService.uploadFile(new UploadFileRequest(file, folderId), principal));
    }

    /**
     * Перемещение файла в другую папку
     */
    @PatchMapping("/move")
    public ResponseEntity<MoveFileResponse> moveFile(
            @RequestBody MoveFileRequest request,
            Principal principal
    ){
       log.info("Запрос на перемещение файла пользователем {}", principal.getName());
       return ResponseEntity.ok(fileService.moveFile(request, principal));
    }

    /**
     * Удаление файла
     */
    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<DeleteFileResponse> deleteFile(
            @PathVariable Long fileId,
            Principal principal
    ) {
        log.info("Запрос на удаление файла id={} пользователем {}", fileId, principal.getName());
        return ResponseEntity.ok(fileService.deleteFile(new DeleteFileRequest(fileId), principal));
    }

    /**
     * Получение списка файлов в конкретной папке
     */
    @GetMapping("/{folderId}")
    public ResponseEntity<GetFilesInDirectoryResponse> getFiles(
            @PathVariable Long folderId,
            Principal principal
    ) {
        log.info("Запрос на получение файлов из папки {} пользователем {}", folderId, principal.getName());
        return ResponseEntity.ok(fileService.getFiles(new GetFilesInDirectoryRequest(folderId), principal));
    }

    /**
     * Получение списка файлов из корневой папки
     */
    @GetMapping
    public ResponseEntity<GetFilesInDirectoryResponse> getFiles(Principal principal) {
        log.info("Запрос на получение файлов из корневой директории пользователем {}", principal.getName());
        return ResponseEntity.ok(fileService.getFiles(new GetFilesInDirectoryRequest(null), principal));
    }

    /**
     * Скачивание файла
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable Long fileId,
            Principal principal
    ) {
        log.info("Запрос на скачивание файла id={} пользователем {}", fileId, principal.getName());
        return fileService.downloadFileResponse(fileId, principal);
    }
}
