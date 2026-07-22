package ma.wvssim.siem.consumer;

import java.util.Set;

/** Logique pure des 3 regles de detection, testable sans Spring ni base de donnees. */
public final class SiemRules {

    private SiemRules() {
    }

    /** Regle 1 : trop de depots par le meme acteur sur la fenetre glissante. */
    public static boolean exceedsFrequency(long depositsInWindow, int maxDeposits) {
        return depositsInWindow > maxDeposits;
    }

    /** Regle 2 : depot en dehors des heures ouvrees habituelles. */
    public static boolean isOutsideBusinessHours(int hour, int businessStart, int businessEnd) {
        return hour < businessStart || hour >= businessEnd;
    }

    /** Regle 3 : extension de fichier jugee suspecte (executables, scripts). */
    public static boolean isSuspiciousExtension(String filename, Set<String> suspiciousExtensions) {
        String ext = extensionOf(filename);
        return ext != null && suspiciousExtensions.contains(ext);
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
