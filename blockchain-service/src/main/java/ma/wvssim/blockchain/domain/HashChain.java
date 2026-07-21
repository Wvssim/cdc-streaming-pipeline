package ma.wvssim.blockchain.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Un maillon du registre d'integrite append-only (table {@code integrity.hash_chain}). */
@Entity
@Table(name = "hash_chain", schema = "integrity")
public class HashChain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "doc_hash")
    private String docHash;

    @Column(name = "prev_hash")
    private String prevHash;

    @Column(name = "chain_hash")
    private String chainHash;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected HashChain() {
        // requis par JPA
    }

    public HashChain(Long docId, String docHash, String prevHash, String chainHash, OffsetDateTime createdAt) {
        this.docId = docId;
        this.docHash = docHash;
        this.prevHash = prevHash;
        this.chainHash = chainHash;
        this.createdAt = createdAt;
    }

    public Long getSeq() {
        return seq;
    }

    public Long getDocId() {
        return docId;
    }

    public String getDocHash() {
        return docHash;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getChainHash() {
        return chainHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
