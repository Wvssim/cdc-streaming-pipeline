package ma.wvssim.siem.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface DepositRepository extends JpaRepository<Deposit, Long> {

    long countByActorAndUploadedAtAfter(String actor, OffsetDateTime after);
}
