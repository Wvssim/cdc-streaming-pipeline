package ma.wvssim.blockchain.consumer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 en hexadecimal. Logique pure, testable sans Spring. */
public final class HashUtil {

    /** Maillon de genese : aucun maillon precedent. */
    public static final String GENESIS = "0".repeat(64);

    private HashUtil() {
    }

    public static String sha256Hex(byte[] data) {
        return toHex(digest(data));
    }

    /** {@code chain_hash = SHA256(doc_hash || prev_hash)}. */
    public static String chainHash(String docHash, String prevHash) {
        return sha256Hex((docHash + prevHash).getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] digest(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible sur cette JVM", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
