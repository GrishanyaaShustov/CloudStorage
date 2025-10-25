package cloud.storage.userservice.repository;

import cloud.storage.userservice.models.File;
import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    boolean existsByNameAndFolderAndUser(String name, Folder folder, User user);
}
