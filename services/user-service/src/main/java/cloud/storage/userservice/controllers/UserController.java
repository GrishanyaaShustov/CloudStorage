package cloud.storage.userservice.controllers;

import cloud.storage.userservice.dto.requests.CreateFolderRequest;
import cloud.storage.userservice.dto.response.CreateFolderResponse;
import cloud.storage.userservice.services.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * Создание новой папки
     */
    @PostMapping("/folder/create")
    public ResponseEntity<CreateFolderResponse> createFolder(
            @RequestBody CreateFolderRequest request,
            Principal principal
    ){
        log.info("Request on uploading file by user {}", principal.getName());
        return ResponseEntity.ok(userService.createFolder(request, principal));
    }

}
