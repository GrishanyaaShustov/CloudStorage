package cloud.storage.userservice.controllers;

import cloud.storage.userservice.DTO.requests.fileRequests.*;
import cloud.storage.userservice.DTO.responses.fileResponses.*;
import cloud.storage.userservice.services.FileService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/file")
@AllArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<UploadFileResponse> uploadFile(@RequestParam MultipartFile file, @RequestParam(value = "folderId", required = false) Long folderId, Principal principal) {
        try {
            return ResponseEntity.ok(fileService.uploadFile(new UploadFileRequest(file, folderId), principal));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new UploadFileResponse(e.getMessage(), false));
        }
    }
}
