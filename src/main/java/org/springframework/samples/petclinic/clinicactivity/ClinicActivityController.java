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
import org.springframework.samples.petclinic.model.ClinicActivityLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/clinic-activity")public class ClinicActivityController implements InitializingBean {

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
    }// This ep is here to throw error
	@GetMapping("active-errors-ratio")
	public int getActiveErrorsRatio() {
		return dataService.getActiveLogsRatio("errors");
	}

	@PostMapping("/populate-logs")
    public ResponseEntity<String> populateData(@RequestParam(name = "count", defaultValue = "6000000") int count) {
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
    }@GetMapping(value = "/query-logs", produces = "application/json")
    @Cacheable(value = "query-logs", key = "#page + '-' + #size")
    public Page<ClinicActivityLog> getLogs(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "100") int size) {
        return repository.findByNumericValue(50000, PageRequest.of(page, size));
    }@DeleteMapping("/cleanup-logs")
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
        int totalOperations = 0;for (int queryTypeIndex = 0; queryTypeIndex < uniqueQueriesCount; queryTypeIndex++) {
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
        }long endTime = System.currentTimeMillis();
        String message = String.format("Executed %d simulated clinic query operations in %d ms.", totalOperations, (endTime - startTime));
        logger.info(message);
        return ResponseEntity.ok(message);
    }

	@PostMapping("/recreate-and-populate-logs")
	public ResponseEntity<String> recreateAndPopulateLogs(@RequestParam(name = "count", defaultValue = "6000000") int count) {
		logger.info("Received request to recreate and populate {} clinic activity logs.", count);
		if (count <= 0) {
			return ResponseEntity.badRequest().body("Count must be a positive integer.");
		}
		try {
			// Drop the table
			jdbcTemplate.execute("DROP TABLE IF EXISTS clinic_activity_logs");
			logger.info("Table 'clinic_activity_logs' dropped successfully.");// Recreate the table
			String createTableSql = "CREATE TABLE clinic_activity_logs (" +
				"id SERIAL PRIMARY KEY," +
				"activity_type VARCHAR(255)," +
				"numeric_value INTEGER," +
				"event_timestamp TIMESTAMP," +
				"status_flag BOOLEAN," +
				"payload TEXT" +
				")";
			jdbcTemplate.execute(createTableSql);
			logger.info("Table 'clinic_activity_logs' created successfully.");

			// Populate data
			dataService.populateData(count);
			return ResponseEntity.ok("Successfully recreated and initiated population of " + count + " clinic activity logs.");
		} catch (Exception e) {
			logger.error("Error during clinic activity log recreation and population", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during data recreation and population: " + e.getMessage());
		}
	}@PostMapping("/io-intensive-load")
	public ResponseEntity<String> createIOIntensiveLoad(@RequestParam(name = "duration", defaultValue = "5") int durationMinutes,
														@RequestParam(name = "threads", defaultValue = "6") int numThreads,
														@RequestParam(name = "limit", defaultValue = "400000") int limit) {
		logger.warn("Received request to create I/O INTENSIVE LOAD for {} minutes with {} threads and {} limit - This will MAX OUT disk I/O operations!",
			durationMinutes, numThreads, limit);
		if (durationMinutes <= 0) {
			return ResponseEntity.badRequest().body("Duration must be a positive integer.");
		}
		if (durationMinutes > 60) {
			return ResponseEntity.badRequest().body("Duration too high for I/O intensive load - maximum 60 minutes to prevent storage overload.");
		}
		if (numThreads <= 0) {
			return ResponseEntity.badRequest().body("Number of threads must be a positive integer.");
		}
		if (numThreads > 20) {return ResponseEntity.badRequest().body("Too many threads for I/O intensive load - maximum 20 to prevent system crash.");
		}
		if (limit <= 0) {
			return ResponseEntity.badRequest().body("Limit must be a positive integer.");
		}
		if (limit > 1000000) {
			return ResponseEntity.badRequest().body("Limit too high for I/O intensive load - maximum 1,000,000 to prevent excessive resource usage.");
		}
		try {
			dataService.createIOIntensiveLoad(durationMinutes, numThreads, limit);
			return ResponseEntity.ok("Successfully completed I/O INTENSIVE LOAD for " + durationMinutes + " minutes with " + numThreads + " threads and " + limit + " limit - Disk I/O was maxed out!");
		} catch (Exception e) {
			logger.error("Error during I/O intensive load", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during I/O intensive load: " + e.getMessage());
		}
	}private void performObservableOperation(String operationName) {
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