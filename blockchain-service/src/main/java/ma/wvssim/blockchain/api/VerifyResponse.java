package ma.wvssim.blockchain.api;

public record VerifyResponse(boolean valid, long linksChecked, Long brokenAtSeq) {
}
