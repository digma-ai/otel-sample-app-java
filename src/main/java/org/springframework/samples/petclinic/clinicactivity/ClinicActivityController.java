package org.springframework.samples.petclinic.clinicactivity;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/clinic-activity")
public class ClinicActivityController implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ClinicActivityController.class);

    private final ClinicActivityDataService dataService;
    private final ClinicActivityLogRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    private OpenTelemetry openTelemetry;

    private Tracer otelTracer;

    @Autowired
    public ClinicActivityController(ClinicActivityDataService dataService,
                                    ClinicActivityLogRepository repository,
                                    JdbcTemplate jdbcTemplate) {
        this.dataService = dataService;
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.otelTracer = openTelemetry.getTracer("ClinicActivityController");
    }

    @PostMapping("/populate-logs")
    public ResponseEntity<String> populateData(@RequestParam(name = "count", defaultValue = "2000000") int count) {
        logger.info("Received request to populate {} clinic activity logs.", count);
        if (count <= 0) {
            return ResponseEntity.badRequest().body("Count must be a positive integer.");
        }
        try {
            dataService.populateData(count);
            return ResponseEntity.ok("Successfully initiated population of " + count + " clinic activity logs.");
        } catch (Exception e) {
            logger.error("Error during clinic activity log population", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during data population: " + e.getMessage());
        }
    }

    @GetMapping("/query-logs")
    public ResponseEntity<String> getLogs(
            @RequestParam(name = "repetitions", defaultValue = "100") int repetitions) {

        if (repetitions <= 0) {
            return ResponseEntity.badRequest().body("Repetitions must be a positive integer.");
        }

        int numericValueToTest = 50000;
        String sql = "SELECT id, activity_type, numeric_value, event_timestamp, status_flag, payload FROM clinic_activity_logs WHERE numeric_value = ?";

        logger.info("Executing direct JDBC query for numeric_value = {}, {} times.", numericValueToTest, repetitions);

        long totalTimeForAllRepetitionsNanos = 0;
        int rowsFoundLastCall = 0;
        List<Map<String, Object>> lastResults; // To store results from the last call

        for (int i = 0; i < repetitions; i++) {
            long startTimeNanos = System.nanoTime();
            lastResults = jdbcTemplate.queryForList(sql, numericValueToTest);
            long endTimeNanos = System.nanoTime();
            totalTimeForAllRepetitionsNanos += (endTimeNanos - startTimeNanos);
            if (i == repetitions - 1) { // Get row count from the last execution
                 rowsFoundLastCall = lastResults.size();
            }
        }

        long totalDurationMillis = totalTimeForAllRepetitionsNanos / 1_000_000;

        String message = String.format(
            "Executed JDBC query for numeric_value = %d, %d time(s). Last call found %d rows. Total execution time: %d ms.",
            numericValueToTest, repetitions, rowsFoundLastCall, totalDurationMillis
        );
        logger.info(message);

        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/cleanup-logs")
    public ResponseEntity<String> cleanupLogs() {
        logger.info("Received request to cleanup all clinic activity logs.");
        try {
            dataService.cleanupActivityLogs();
            return ResponseEntity.ok("Successfully cleaned up all clinic activity logs.");
        } catch (Exception e) {
            logger.error("Error during clinic activity log cleanup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during cleanup: " + e.getMessage());
        }
    }

    @GetMapping("/run-simulated-queries")
    public ResponseEntity<String> runSimulatedQueries(
		@RequestParam(name = "uniqueQueriesCount", defaultValue = "3") int uniqueQueriesCount,
		@RequestParam(name = "repetitions", defaultValue = "100") int repetitions
	) {
        long startTime = System.currentTimeMillis();
        int totalOperations = 0;

        for (int queryTypeIndex = 0; queryTypeIndex < uniqueQueriesCount; queryTypeIndex++) {
            char queryTypeChar = (char) ('A' + queryTypeIndex);
            String parentSpanName = "Batch_Type" + queryTypeChar;
            Span typeParentSpan = otelTracer.spanBuilder(parentSpanName).startSpan();

            try (Scope scope = typeParentSpan.makeCurrent()) {
                for (int execution = 1; execution <= repetitions; execution++) {
                    String operationName = "SimulatedClinicQuery_Type" + queryTypeChar;
                    performObservableOperation(operationName);
                    totalOperations++;
                }
            } finally {
                typeParentSpan.end();
            }
        }

        long endTime = System.currentTimeMillis();
        String message = String.format("Executed %d simulated clinic query operations in %d ms.", totalOperations, (endTime - startTime));
        logger.info(message);
        return ResponseEntity.ok(message);
    }

    private void performObservableOperation(String operationName) {
        Span span = otelTracer.spanBuilder(operationName)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("db.system", "postgresql")
            .setAttribute("db.name", "petclinic")
            .setAttribute("db.statement", "SELECT * FROM some_table" + operationName)
            .setAttribute("db.operation", "SELECT")
            .startSpan();
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 6));
            logger.debug("Executing simulated operation: {}", operationName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Simulated operation {} interrupted", operationName, e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }
}
