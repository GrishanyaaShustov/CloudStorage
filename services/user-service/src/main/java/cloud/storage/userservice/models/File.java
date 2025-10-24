package cloud.storage.userservice.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Table(name = "files")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name; // Имя файла

    @Column(name = "s3_key", nullable = false, length = 1024, unique = true)
    private String s3Key; // Ключ файла в S3

    @Column(nullable = false)
    private Long size; // Размер файла в байтах

    @Column(name = "content_type", length = 255)
    private String contentType; // MIME-тип

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now(ZoneOffset.UTC); // Дата загрузки

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_files_user_id"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", foreignKey = @ForeignKey(name = "fk_files_folder_id"))
    private Folder folder;
}