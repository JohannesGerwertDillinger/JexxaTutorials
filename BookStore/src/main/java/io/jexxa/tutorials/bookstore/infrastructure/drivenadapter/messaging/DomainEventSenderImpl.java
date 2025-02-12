package io.jexxa.tutorials.bookstore.infrastructure.drivenadapter.messaging;

import io.jexxa.addend.applicationcore.DomainEvent;
import io.jexxa.addend.infrastructure.DrivenAdapter;
import io.jexxa.infrastructure.messaging.MessageSender;
import io.jexxa.tutorials.bookstore.domainservice.DomainEventSender;

import java.util.Objects;
import java.util.Properties;

import static io.jexxa.infrastructure.MessageSenderManager.getMessageSender;


@SuppressWarnings("unused")
@DrivenAdapter
public class DomainEventSenderImpl implements DomainEventSender {
    private final MessageSender messageSender;

    public DomainEventSenderImpl(Properties properties)
    {
        // Request a MessageSender from the framework, so that we can configure it in our properties file
        messageSender = getMessageSender(DomainEventSender.class, properties);
    }

    @Override
    public void publish(Object domainEvent)
    {
        // We just allow sending DomainEvents
        validateDomainEvent(domainEvent);

        // For publishing a DomainEvent we use a fluent API in Jexxa
        messageSender
                .send(domainEvent)
                .toTopic("BookStore")
                .addHeader("Type", domainEvent.getClass().getSimpleName())
                .asJson();
    }

    private void validateDomainEvent(Object domainEvent)
    {
        Objects.requireNonNull(domainEvent);
        if ( domainEvent.getClass().getAnnotation(DomainEvent.class) == null )
        {
            throw new IllegalArgumentException("Given object is not annotated with @DomainEvent");
        }
    }
}
