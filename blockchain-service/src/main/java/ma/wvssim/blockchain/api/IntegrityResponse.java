package ma.wvssim.blockchain.api;

import ma.wvssim.blockchain.domain.HashChain;

import java.time.OffsetDateTime;

public record IntegrityResponse(
        Long seq,
        Long docId,
        String docHash,
        String prevHash,
        String chainHash,
        OffsetDateTime createdAt
) {

    public static IntegrityResponse from(HashChain h) {
        return new IntegrityResponse(h.getSeq(), h.getDocId(), h.getDocHash(), h.getPrevHash(),
                h.getChainHash(), h.getCreatedAt());
    }
}
