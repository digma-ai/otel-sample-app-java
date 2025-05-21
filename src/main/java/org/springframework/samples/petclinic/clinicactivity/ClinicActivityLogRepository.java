package org.springframework.samples.petclinic.clinicactivity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.samples.petclinic.model.ClinicActivityLog;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClinicActivityLogRepository extends JpaRepository<ClinicActivityLog, Integer> {

    @Query("SELECT cal FROM ClinicActivityLog cal WHERE cal.activityType = :activityType " +
           "AND cal.numericValue >= :minNumericValue AND cal.numericValue <= :maxNumericValue " +
           "AND cal.eventTimestamp >= :startDate AND cal.eventTimestamp < :endDate " +
           "AND cal.statusFlag = :statusFlag")
    List<ClinicActivityLog> findByComplexCriteria(
        @Param("activityType") String activityType,
        @Param("minNumericValue") Integer minNumericValue,
        @Param("maxNumericValue") Integer maxNumericValue,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("statusFlag") Boolean statusFlag
    );
} 