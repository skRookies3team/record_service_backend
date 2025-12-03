package com.example.petlog.repository;

import com.example.petlog.entity.PhotoArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoArchiveRepository extends JpaRepository<PhotoArchive, Long> {
}