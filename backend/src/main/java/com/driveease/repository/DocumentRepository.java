package com.driveease.repository;

import com.driveease.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByVehicleId(Long vehicleId);
}
