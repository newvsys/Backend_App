package com.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnApproveRequestDTO {

	private String returnId; // required — the return_id (e.g. RET-ORD-xxx-timestamp)

	private String status; // required — APPROVED / REJECTED

	private String comments; // optional — admin remarks

	private Long userId; // required — ID of the user approving/rejecting the return

}
