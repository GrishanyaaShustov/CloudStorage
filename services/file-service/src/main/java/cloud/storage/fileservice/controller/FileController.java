package cloud.storage.fileservice.controller;

import cloud.storage.fileservice.dto.requests.UploadFileRequest;
import cloud.storage.fileservice.dto.responses.UploadFileResponse;
import cloud.storage.fileservice.services.FileService;
import lombok.RequiredArgsConstructor;
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
}
