package ma.wvssim.blockchain.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HashChainRepository extends JpaRepository<HashChain, Long> {

    boolean existsByDocId(Long docId);

    Optional<HashChain> findTopByOrderBySeqDesc();

    List<HashChain> findAllByOrderBySeqAsc();
}
