package com.valhallagame.friendserviceserver.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptParameter {
	private String accepter;
	private String acceptee;
}
