package ma.wvssim.siem;

import ma.wvssim.siem.domain.Alert;
import ma.wvssim.siem.domain.AlertRepository;
import ma.wvssim.siem.domain.DepositRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * T6.1 : meme chaine CDC reelle (Postgres + Kafka + Connect/Debezium en containers) que
 * AuditServiceE2ETest, appliquee a siem-service. Scenario : la regle 3 (extension suspecte).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SiemServiceE2ETest {

    private static final Network NETWORK = Network.newNetwork();
    private static final Path INFRA = Path.of("..", "infra");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withNetwork(NETWORK)
            .withNetworkAliases("cdc-postgres")
            .withUsername("cdc")
            .withPassword("cdc")
            .withDatabaseName("docdb")
            .withCommand("postgres", "-c", "wal_level=logical", "-c", "max_wal_senders=10", "-c", "max_replication_slots=10")
            .withCopyFileToContainer(MountableFile.forHostPath(INFRA.resolve("init-scripts/01-documents.sql")), "/docker-entrypoint-initdb.d/01-documents.sql")
            .withCopyFileToContainer(MountableFile.forHostPath(INFRA.resolve("init-scripts/02-consumer-schemas.sql")), "/docker-entrypoint-initdb.d/02-consumer-schemas.sql");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.0"))
            .withNetwork(NETWORK)
            .withListener("kafka:19092");

    @Container
    static final GenericContainer<?> CONNECT = new GenericContainer<>(DockerImageName.parse("quay.io/debezium/connect:3.5"))
            .withNetwork(NETWORK)
            .withNetworkAliases("connect")
            .withExposedPorts(8083)
            .withEnv(Map.of(
                    "BOOTSTRAP_SERVERS", "kafka:19092",
                    "GROUP_ID", "cdc-connect-test",
                    "CONFIG_STORAGE_TOPIC", "connect_configs",
                    "OFFSET_STORAGE_TOPIC", "connect_offsets",
                    "STATUS_STORAGE_TOPIC", "connect_statuses",
                    "CONFIG_STORAGE_REPLICATION_FACTOR", "1",
                    "OFFSET_STORAGE_REPLICATION_FACTOR", "1",
                    "STATUS_STORAGE_REPLICATION_FACTOR", "1"
            ))
            .waitingFor(Wait.forHttp("/connectors").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(120)))
            .dependsOn(KAFKA, POSTGRES);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private DepositRepository depositRepository;

    /** Enregistre le meme connecteur qu'en prod (infra/connectors/postgres-source.json), inchange. */
    @BeforeAll
    static void registerConnector() throws Exception {
        String connectorConfig = Files.readString(INFRA.resolve("connectors/postgres-source.json"));
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + CONNECT.getHost() + ":" + CONNECT.getMappedPort(8083) + "/connectors"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(connectorConfig))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IllegalStateException("echec enregistrement connecteur : " + response.statusCode() + " " + response.body());
        }
    }

    @Test
    void extensionSuspecte_declencheUneAlerte() throws Exception {
        long docId = insertDocument("script-malveillant.exe");

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(findAlert(docId, "EXTENSION_SUSPECTE")).isPresent());
    }

    @Test
    void extensionOrdinaire_neDeclenchePasDAlerte() throws Exception {
        long docId = insertDocument("facture.pdf");

        // depositRepository.existsById(docId) est le signal fiable que le message a ete
        // consomme et les 3 regles evaluees (ecrit en tout premier dans SiemService.evaluate).
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> assertThat(depositRepository.existsById(docId)).isTrue());

        assertThat(findAlert(docId, "EXTENSION_SUSPECTE")).isEmpty();
    }

    private Optional<Alert> findAlert(long docId, String rule) {
        return alertRepository.findAll().stream()
                .filter(a -> a.getDocId() != null && a.getDocId() == docId && rule.equals(a.getRule()))
                .findFirst();
    }

    private long insertDocument(String filename) throws Exception {
        try (Connection conn = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "INSERT INTO public.documents (filename, content_type, size, storage_key, uploaded_by) " +
                            "VALUES ('" + filename + "', 'application/pdf', 1024, 'documents/" + filename + "', 'wassim') RETURNING id")) {
                rs.next();
                return rs.getLong("id");
            }
        }
    }
}
