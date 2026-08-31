package com.documind.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_document_hash",
                        columnNames = {"user_id", "document_hash"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    /*
     * SHA-256 hash of the uploaded PDF.
     */
    @Column(
            name = "document_hash",
            nullable = false,
            length = 64
    )
    private String documentHash;
}