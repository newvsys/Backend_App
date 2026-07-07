package com.user.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "return_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnStatusHistoryEO {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "return_request_id", nullable = false)
	private ReturnRequestEO returnRequest;

	@Column(name = "new_status", length = 30)
	private String newStatus;

	@Column(name = "activity_type", length = 50)
	private String activityType;

	@Column(name = "remarks", length = 255)
	private String remarks;

	@Column(name = "changed_by", length = 100)
	private String changedBy;

	@Column(name = "changed_at")
	private LocalDateTime changedAt;

}
