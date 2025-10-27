package cloud.storage.fileservice.repository;

import cloud.storage.fileservice.models.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    Optional<Folder> findFolderById(Long folderId);
}
