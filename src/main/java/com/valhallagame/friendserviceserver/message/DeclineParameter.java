package com.valhallagame.friendserviceserver.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeclineParameter {
	private String decliner;
	private String declinee;
}
