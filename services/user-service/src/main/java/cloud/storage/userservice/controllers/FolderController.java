package cloud.storage.userservice.controllers;

import cloud.storage.userservice.DTO.requests.folderRequests.*;
import cloud.storage.userservice.DTO.responses.folderResponses.*;
import cloud.storage.userservice.services.FolderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/folder")
@AllArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping("/create")
    public ResponseEntity<CreateFolderResponse> createFolder(@RequestBody CreateFolderRequest request, Principal principal){
        try {
            return ResponseEntity.ok(folderService.createFolder(request, principal));
        } catch (Exception e){
            return ResponseEntity.status(Integer.parseInt(e.getMessage().split("\\.")[0])).body(new CreateFolderResponse(e.getMessage().split("\\.")[1]));
        }
    }
}
