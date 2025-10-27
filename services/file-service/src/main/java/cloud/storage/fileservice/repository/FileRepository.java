package cloud.storage.fileservice.repository;

import cloud.storage.fileservice.models.File;
import cloud.storage.fileservice.models.Folder;
import cloud.storage.fileservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findFileById(Long id);
    List<File> findFilesByFolder(Folder folder);
    boolean existsByNameAndFolderAndUser(String name, Folder folder, User user);
}
