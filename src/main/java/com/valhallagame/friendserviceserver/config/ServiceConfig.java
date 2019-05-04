package com.valhallagame.friendserviceserver.config;

import com.valhallagame.characterserviceclient.CharacterServiceClient;
import com.valhallagame.common.DefaultServicePortMappings;
import com.valhallagame.personserviceclient.PersonServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"production", "development"})
public class ServiceConfig {
	@Bean
	public CharacterServiceClient characterServiceClient() {
		CharacterServiceClient.init("http://character-service.character-service:" + DefaultServicePortMappings.CHARACTER_SERVICE_PORT);
		return CharacterServiceClient.get();
	}

	@Bean
	public PersonServiceClient personServiceClient() {
		PersonServiceClient.init("http://person-service.person-service:" + DefaultServicePortMappings.PERSON_SERVICE_PORT);
		return PersonServiceClient.get();
	}
}
