package ma.wvssim.blockchain.consumer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HashUtilTest {

    @Test
    void calculeUnSha256Hexadecimal64Caracteres() {
        String hash = HashUtil.sha256Hex("bonjour".getBytes(StandardCharsets.UTF_8));
        assertEquals(64, hash.length());
        assertEquals("2cb4b1431b84ec15d35ed83bb927e27e8967d75f4bcd9cc4b25c8d879ae23e18", hash);
    }

    @Test
    void chaineDeuxHashDeFaconDeterministe() {
        String h1 = HashUtil.chainHash("aaa", HashUtil.GENESIS);
        String h2 = HashUtil.chainHash("aaa", HashUtil.GENESIS);
        assertEquals(h1, h2);
        assertNotEquals(h1, HashUtil.chainHash("bbb", HashUtil.GENESIS));
    }
}
