package org.springframework.samples.petclinic.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;
import java.util.UUID;

@Data
@Document(indexName = "feedbacks")
public class ClinicFeedback {
	@Id
	private String id;
	private String userEmail;
	private String comment;

	public ClinicFeedback(String userEmail, String comment) {
		this.id = UUID.randomUUID().toString();
		this.userEmail = userEmail;
		this.comment = comment;
	}
}
