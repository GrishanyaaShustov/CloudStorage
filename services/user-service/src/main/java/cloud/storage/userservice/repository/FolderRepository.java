package cloud.storage.userservice.repository;

import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    @Query("SELECT f FROM Folder f JOIN FETCH f.user WHERE f.id = :id")
    Optional<Folder> findFolderById(@Param("id") Long id);
    boolean existsByNameAndParentAndUser(String name, Folder parent, User user);
}
