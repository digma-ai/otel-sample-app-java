package org.springframework.samples.petclinic.clinicfeedback;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.samples.petclinic.model.ClinicFeedback;

import java.util.List;

public interface FeedbackRepository extends ElasticsearchRepository<ClinicFeedback, String> {

}
