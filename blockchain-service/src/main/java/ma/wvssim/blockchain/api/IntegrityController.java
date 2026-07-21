package ma.wvssim.blockchain.api;

import ma.wvssim.blockchain.consumer.HashUtil;
import ma.wvssim.blockchain.domain.HashChain;
import ma.wvssim.blockchain.domain.HashChainRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrity")
public class IntegrityController {

    private final HashChainRepository repository;

    public IntegrityController(HashChainRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<IntegrityResponse> list() {
        return repository.findAllByOrderBySeqAsc()
                .stream()
                .map(IntegrityResponse::from)
                .toList();
    }

    /** Recalcule la chaine (doc_hash + prev_hash -> chain_hash) et detecte toute alteration des maillons stockes. */
    @GetMapping("/verify")
    public VerifyResponse verify() {
        List<HashChain> chain = repository.findAllByOrderBySeqAsc();
        String expectedPrev = HashUtil.GENESIS;
        for (HashChain link : chain) {
            if (!expectedPrev.equals(link.getPrevHash())) {
                return new VerifyResponse(false, chain.size(), link.getSeq());
            }
            String recomputed = HashUtil.chainHash(link.getDocHash(), link.getPrevHash());
            if (!recomputed.equals(link.getChainHash())) {
                return new VerifyResponse(false, chain.size(), link.getSeq());
            }
            expectedPrev = link.getChainHash();
        }
        return new VerifyResponse(true, chain.size(), null);
    }
}
