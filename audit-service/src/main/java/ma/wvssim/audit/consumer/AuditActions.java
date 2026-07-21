package ma.wvssim.audit.consumer;

/** Traduit l'operation Debezium (c/u/d/r) en action d'audit lisible. Logique pure, testable. */
public final class AuditActions {

    private AuditActions() {
    }

    public static String of(String op) {
        if (op == null) {
            return "INCONNU";
        }
        return switch (op) {
            case "c" -> "CREATION";
            case "u" -> "MISE_A_JOUR";
            case "d" -> "SUPPRESSION";
            case "r" -> "SNAPSHOT";
            default -> op;
        };
    }
}
