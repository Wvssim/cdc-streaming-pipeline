package ma.wvssim.ocr;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import ma.wvssim.ocr.domain.ExtractedText;
import ma.wvssim.ocr.domain.ExtractedTextRepository;
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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * AuditServiceE2ETest, appliquee a ocr-service. Un vrai MinIO recoit un fichier texte brut avant
 * l'insert ; Tika l'extrait vraiment (pas de mock du moteur).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OcrServiceE2ETest {

    private static final Network NETWORK = Network.newNetwork();
    private static final Path INFRA = Path.of("..", "infra");
    private static final String BUCKET = "documents";
    private static final String TEXTE = "Rapport d'integrite du pipeline CDC - version de test.";

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

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
            .withExposedPorts(9000)
            .withEnv(Map.of("MINIO_ROOT_USER", "minioadmin", "MINIO_ROOT_PASSWORD", "minioadmin"))
            .withCommand("server", "/data")
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).withStartupTimeout(Duration.ofSeconds(60)));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("minio.endpoint", () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
        registry.add("minio.bucket", () -> BUCKET);
    }

    @Autowired
    private ExtractedTextRepository extractedTextRepository;

    /** Enregistre le meme connecteur qu'en prod (infra/connectors/postgres-source.json), inchange. */
    @BeforeAll
    static void registerConnectorEtBucket() throws Exception {
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

        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000))
                .credentials("minioadmin", "minioadmin")
                .build();
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }

    @Test
    void creationDocument_extraitLeVraiTexteViaTika() throws Exception {
        byte[] content = TEXTE.getBytes(StandardCharsets.UTF_8);
        String storageKey = "documents/rapport.txt";
        putObject(storageKey, content);

        long docId = insertDocument("rapport.txt", "text/plain", storageKey, content.length);

        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> {
                    Optional<ExtractedText> extrait = extractedTextRepository.findById(docId);
                    assertThat(extrait).isPresent();
                    assertThat(extrait.get().getEngine()).isEqualTo("tika");
                    assertThat(extrait.get().getText()).contains("integrite du pipeline CDC");
                });
    }

    private void putObject(String storageKey, byte[] content) throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint("http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000))
                .credentials("minioadmin", "minioadmin")
                .build();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(BUCKET)
                .object(storageKey)
                .stream(new ByteArrayInputStream(content), content.length, -1)
                .build());
    }

    private long insertDocument(String filename, String contentType, String storageKey, int size) throws Exception {
        try (Connection conn = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(
                    "INSERT INTO public.documents (filename, content_type, size, storage_key, uploaded_by) " +
                            "VALUES ('" + filename + "', '" + contentType + "', " + size + ", '" + storageKey + "', 'wassim') RETURNING id")) {
                rs.next();
                return rs.getLong("id");
            }
        }
    }
}
