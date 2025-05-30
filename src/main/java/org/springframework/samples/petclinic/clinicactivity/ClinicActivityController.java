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
    }

    @GetMapping("/query-logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Span span = otelTracer.spanBuilder("query_logs")
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("page", page)
            .setAttribute("size", size)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            int offset = page * size;
            String countSql = "SELECT COUNT(*) FROM clinic_activity_logs WHERE numeric_value = ?";
            String sql = "SELECT id, activity_type, numeric_value, event_timestamp, status_flag, payload " +
                        "FROM clinic_activity_logs WHERE numeric_value = ? " +
                        "ORDER BY id LIMIT ? OFFSET ?";

            int numericValueToTest = 50000;

            Span countSpan = otelTracer.spanBuilder("count_total_records")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.statement",