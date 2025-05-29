package org.springframework.samples.petclinic.clinicactivity;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.ClinicActivityLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import javax.sql.DataSource;
import java.sql.Connection;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.Random;

@Service
public class ClinicActivityDataService {

    private static final Logger logger = LoggerFactory.getLogger(ClinicActivityDataService.class);
    private static final int BATCH_SIZE = 1000;
    private static final int COPY_FLUSH_EVERY = 50_000;

    private final ClinicActivityLogRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    private static final List<String> ACTIVITY_TYPES = List.of(
            "Patient Check-in", "Patient Check-out", "Appointment Scheduling", "Medical Record Update",
            "Prescription Issuance", "Lab Test Order", "Lab Test Result Review", "Billing Generation",
            "Payment Processing", "Inventory Check", "Staff Shift Start", "Staff Shift End",
            "Emergency Alert", "Consultation Note", "Follow-up Reminder"
    );
    private final Random random = new Random();

    @Autowired
    public ClinicActivityDataService(ClinicActivityLogRepository repository,
                                     JdbcTemplate jdbcTemplate,
                                     DataSource dataSource,
                                     PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Transactional
    public double getActiveLogsRatio(String type) {
        long all = repository.countLogsByType(type);
        if (all == 0) {
            return 0.0;
        }
        long active = repository.countActiveLogsByType(type);
        return (double) active / all;
    }

    @Transactional
    public void cleanupActivityLogs() {
        logger.info("Received request to clean up all clinic activity logs.");
        long startTime = System.currentTimeMillis();
        try {
            repository.deleteAllInBatch();
            long endTime = System.currentTimeMillis();
            logger.info("Successfully cleaned up all clinic activity logs in {} ms.", (endTime - startTime));
        } catch (Exception e) {
            logger.error("Error during clinic activity log cleanup", e);
            throw new RuntimeException("Error cleaning up activity logs: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void populateData(int totalEntries) {
        long startTime = System.currentTimeMillis();
        Connection con = null;
        try {
            con = DataSourceUtils.getConnection(dataSource);
            String databaseProductName = con.getMetaData().getDatabaseProductName();
            DataSourceUtils.releaseConnection(con, dataSource);
            con = null;

            if ("PostgreSQL".equalsIgnoreCase(databaseProductName))