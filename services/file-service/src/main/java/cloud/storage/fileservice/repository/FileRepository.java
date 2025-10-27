package cloud.storage.fileservice.repository;

import cloud.storage.fileservice.models.File;
import cloud.storage.fileservice.models.Folder;
import cloud.storage.fileservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
    boolean existsByNameAndFolderAndUser(String name, Folder folder, User user);
}
