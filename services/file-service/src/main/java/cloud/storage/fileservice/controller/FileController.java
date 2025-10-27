package cloud.storage.fileservice.controller;

import cloud.storage.fileservice.dto.requests.DeleteFileRequest;
import cloud.storage.fileservice.dto.requests.GetFilesInDirectoryRequest;
import cloud.storage.fileservice.dto.requests.UploadFileRequest;
import cloud.storage.fileservice.dto.responses.DeleteFileResponse;
import cloud.storage.fileservice.dto.responses.GetFilesInDirectoryResponse;
import cloud.storage.fileservice.dto.responses.UploadFileResponse;
import cloud.storage.fileservice.services.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<UploadFileResponse> uploadFile(@RequestParam("file")MultipartFile file, @RequestParam(name = "folderId", required = false) Long folderId, Principal principal){
        try {
            return ResponseEntity.ok(fileService.uploadFile(new UploadFileRequest(file, folderId), principal));
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UploadFileResponse(false));
        }
    }

    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<DeleteFileResponse> deleteFile(@PathVariable Long fileId, Principal principal){
        try {
            return ResponseEntity.ok(fileService.deleteFile(new DeleteFileRequest(fileId), principal));
        } catch (Exception e){
            int statusCode = Integer.parseInt(e.getMessage().split("\\.")[0]);
            String message = e.getMessage().split("\\.")[1];
            return ResponseEntity.status(statusCode).body(new DeleteFileResponse(false, message));
        }
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<GetFilesInDirectoryResponse> getFiles(@PathVariable(name = "folderId", required = false) Long folderId, Principal principal){
        try {
            return ResponseEntity.ok(fileService.getFiles(new GetFilesInDirectoryRequest(folderId), principal));
        } catch (Exception e){
            int statusCode = Integer.parseInt(e.getMessage().split("\\.")[0]);
            String message = e.getMessage().split("\\.")[1];
            return ResponseEntity.status(statusCode).body(new GetFilesInDirectoryResponse(null, message));
        }
    }

    @GetMapping
    public ResponseEntity<GetFilesInDirectoryResponse> getFiles(Principal principal){
        try {
            return ResponseEntity.ok(fileService.getFiles(new GetFilesInDirectoryRequest(null), principal));
        } catch (Exception e){
            int statusCode = Integer.parseInt(e.getMessage().split("\\.")[0]);
            String message = e.getMessage().split("\\.")[1];
            return ResponseEntity.status(statusCode).body(new GetFilesInDirectoryResponse(null, message));
        }
    }
}
