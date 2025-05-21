package org.springframework.samples.petclinic.clinicactivity;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@Service
public class ClinicActivityDataService {

    private static final Logger logger = LoggerFactory.getLogger(ClinicActivityDataService.class);
    private static final int BATCH_SIZE = 1000;

    private final ClinicActivityLogRepository repository;
    private final DataSource dataSource;

    // List of 15 possible activity types
    private static final List<String> ACTIVITY_TYPES = List.of(
            "Patient Check-in", "Patient Check-out", "Appointment Scheduling", "Medical Record Update",
            "Prescription Issuance", "Lab Test Order", "Lab Test Result Review", "Billing Generation",
            "Payment Processing", "Inventory Check", "Staff Shift Start", "Staff Shift End",
            "Emergency Alert", "Consultation Note", "Follow-up Reminder"
    );
    private final Random random = new Random();

    @Autowired
    public ClinicActivityDataService(ClinicActivityLogRepository repository, DataSource dataSource) {
        this.repository = repository;
        this.dataSource = dataSource;
    }

    @Transactional
    public void cleanupActivityLogs() {
        logger.info("Received request to clean up all clinic activity logs.");
        long startTime = System.currentTimeMillis();
        try {
            repository.deleteAllInBatch(); // Efficiently delete all entries
            long endTime = System.currentTimeMillis();
            logger.info("Successfully cleaned up all clinic activity logs in {} ms.", (endTime - startTime));
        } catch (Exception e) {
            logger.error("Error during clinic activity log cleanup", e);
            throw new RuntimeException("Error cleaning up activity logs: " + e.getMessage(), e);
        }
    }

    public void populateData(int totalEntries) {
        logger.info("Starting COPY data population for {} clinic activity logs.", totalEntries);
        long startTime = System.currentTimeMillis();
        Faker faker = new Faker(new Locale("en-US"));
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        int flushEvery = 10_000; // flush every 10k rows (~few MB)
        try (Connection connection = DataSourceUtils.getConnection(dataSource)) {
            connection.setAutoCommit(false);
            CopyManager copyManager = connection.unwrap(PGConnection.class).getCopyAPI();
            for (int i = 0; i < totalEntries; i++) {
                // Select a random activity type
                String activityType = ACTIVITY_TYPES.get(random.nextInt(ACTIVITY_TYPES.size()));

                int numericVal = faker.number().numberBetween(1, 100_000);
                String ts = dtf.format(LocalDateTime.ofInstant(
                        faker.date().past(5 * 365, TimeUnit.DAYS).toInstant(), ZoneId.systemDefault()));
                boolean status = faker.bool().bool();
                String payload = String.join(" ", faker.lorem().paragraphs(faker.number().numberBetween(1, 3)));

                sb.append(csv(activityType)).append(',')
                  .append(numericVal).append(',')
                  .append(csv(ts)).append(',')
                  .append(status).append(',')
                  .append(csv(payload)).append('\n');

                if (i > 0 && i % flushEvery == 0) {
                    copyManager.copyIn("COPY clinic_activity_logs (activity_type, numeric_value, event_timestamp, status_flag, payload) FROM STDIN WITH (FORMAT csv)", new java.io.StringReader(sb.toString()));
                    sb.setLength(0);
                    logger.info("COPY inserted {} / {} clinic activity logs...", i, totalEntries);
                }
            }
            if (sb.length() > 0) {
                copyManager.copyIn("COPY clinic_activity_logs (activity_type, numeric_value, event_timestamp, status_flag, payload) FROM STDIN WITH (FORMAT csv)", new java.io.StringReader(sb.toString()));
            }
            connection.commit();
        } catch (Exception ex) {
            logger.error("Error during COPY population", ex);
            throw new RuntimeException(ex);
        }
        long endTime = System.currentTimeMillis();
        logger.info("Finished COPY data population for {} clinic activity logs in {} ms.", totalEntries, (endTime - startTime));
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
