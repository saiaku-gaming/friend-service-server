package com.valhallagame.friendserviceserver.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.valhallagame.common.rabbitmq.RabbitMQRouting;

@Configuration
public class RabbitMQConfig {

	// Friend configs
	@Bean
	public DirectExchange friendExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.FRIEND.name());
	}

	@Bean
	public FriendConsumer consumer() {
		return new FriendConsumer();
	}

	// Person configs
	@Bean
	public Queue personDeleteQueue() {
		return new AnonymousQueue();
	}

	@Bean
	public DirectExchange personExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.PERSON.name());
	}

	@Bean
	public Binding bindingPersonDelete(DirectExchange personExchange, Queue personDeleteQueue) {
		return BindingBuilder.bind(personDeleteQueue).to(personExchange).with(RabbitMQRouting.Person.DELETE);
	}

}
