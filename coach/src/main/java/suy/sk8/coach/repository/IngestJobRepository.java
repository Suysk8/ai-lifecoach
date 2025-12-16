package suy.sk8.coach.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import suy.sk8.coach.entity.IngestJob;

public interface IngestJobRepository extends JpaRepository<IngestJob, Long> {
}
