package com.valhallagame.friendserviceserver.config;

import com.valhallagame.common.rabbitmq.RabbitMQRouting;
import com.valhallagame.common.rabbitmq.RabbitSender;
import com.valhallagame.friendserviceserver.rabbitmq.FriendConsumer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class RabbitMQConfig {

	private RabbitTemplate rabbitTemplate;

    public RabbitMQConfig(@Lazy RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

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
	public Queue friendPersonDeleteQueue() {
		return new Queue("friendPersonDeleteQueue");
	}
	
	@Bean
	public Queue friendPersonOnlineQueue() {
		return new Queue("friendPersonOnlineQueue");
	}

	@Bean
	public Queue friendPersonOfflineQueue() {
		return new Queue("friendPersonOfflineQueue");
	}

	@Bean
	public DirectExchange personExchange() {
		return new DirectExchange(RabbitMQRouting.Exchange.PERSON.name());
	}

	
	@Bean
	public Binding bindingPersonOnline(DirectExchange personExchange, Queue friendPersonOnlineQueue) {
		return BindingBuilder.bind(friendPersonOnlineQueue).to(personExchange).with(RabbitMQRouting.Person.ONLINE);
	}

	@Bean
	public Binding bindingPersonOffline(DirectExchange personExchange, Queue friendPersonOfflineQueue) {
		return BindingBuilder.bind(friendPersonOfflineQueue).to(personExchange).with(RabbitMQRouting.Person.OFFLINE);
	}
	
	@Bean
	public Binding bindingPersonDelete(DirectExchange personExchange, Queue friendPersonDeleteQueue) {
		return BindingBuilder.bind(friendPersonDeleteQueue).to(personExchange).with(RabbitMQRouting.Person.DELETE);
	}

	@Bean
	public Jackson2JsonMessageConverter jacksonConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public SimpleRabbitListenerContainerFactory containerFactory() {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setMessageConverter(jacksonConverter());
		return factory;
	}

	@Bean
	public RabbitSender rabbitSender() {
		return new RabbitSender(rabbitTemplate);
	}
}
