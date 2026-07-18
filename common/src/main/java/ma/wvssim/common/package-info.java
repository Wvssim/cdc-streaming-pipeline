/**
 * Types partages du pipeline CDC, consommes par les microservices.
 *
 * L'enveloppe d'evenement Debezium et son payload sont modelises en records Java :
 * {@link ma.wvssim.common.DebeziumMessage} (message Kafka brut, schema + payload),
 * {@link ma.wvssim.common.DebeziumEnvelope} (before / after / op / ts_ms + source) et
 * {@link ma.wvssim.common.DocumentPayload} (une ligne de public.documents).
 */
package ma.wvssim.common;
