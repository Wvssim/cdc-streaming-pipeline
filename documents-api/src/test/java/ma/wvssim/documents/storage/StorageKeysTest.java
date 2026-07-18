package ma.wvssim.documents.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageKeysTest {

    @Test
    void conserveLeNomEtResteUnique() {
        String a = StorageKeys.forUpload("rapport.pdf");
        String b = StorageKeys.forUpload("rapport.pdf");
        assertTrue(a.endsWith("-rapport.pdf"), a);
        assertNotEquals(a, b);
    }

    @Test
    void gereLeNomNul() {
        assertTrue(StorageKeys.forUpload(null).endsWith("-fichier"));
    }

    @Test
    void neutraliseLesSeparateurs() {
        String key = StorageKeys.forUpload("dossier/sous\\fichier.txt");
        assertFalse(key.contains("/"));
        assertFalse(key.contains("\\"));
    }
}
