package com.valhallagame.friendserviceserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.personserviceclient.PersonServiceClient;

@Configuration
@Profile("default")
public class DevConfig {
	@Bean
	public CharacterServiceClient characterServiceClient() {
		return CharacterServiceClient.get();
	}

	@Bean
	public PersonServiceClient personServiceClient() {
		return PersonServiceClient.get();
	}
}
