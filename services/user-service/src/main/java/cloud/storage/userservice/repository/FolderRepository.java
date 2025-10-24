package cloud.storage.userservice.repository;

import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    Optional<Folder> findFolderById(Long id);
    boolean existsByNameAndParentAndUser(String name, Folder parent, User user);
}
