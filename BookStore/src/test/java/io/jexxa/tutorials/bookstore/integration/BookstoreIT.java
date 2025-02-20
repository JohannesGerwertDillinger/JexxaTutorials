package io.jexxa.tutorials.bookstore.integration;

import io.jexxa.drivingadapter.messaging.JMSConfiguration;
import io.jexxa.jexxatest.JexxaIntegrationTest;
import io.jexxa.jexxatest.integrationtest.messaging.MessageBinding;
import io.jexxa.jexxatest.integrationtest.rest.RESTBinding;
import io.jexxa.tutorials.bookstore.BookStore;
import io.jexxa.tutorials.bookstore.applicationservice.BookStoreService;
import io.jexxa.tutorials.bookstore.domain.book.BookSoldOut;
import io.jexxa.tutorials.bookstore.domain.book.ISBN13;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.jexxa.tutorials.bookstore.domain.book.ISBN13.createISBN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BookstoreIT
{
    static private final String addToStock = "addToStock";
    static private final String amountInStock = "amountInStock";
    static private final String sell = "sell";
    private static final ISBN13 ANY_BOOK = createISBN("978-3-86490-387-8" );


    private static JexxaIntegrationTest jexxaIntegrationTest;  // Simplified IT testing with jexxa-test
    private static RESTBinding restBinding;                    // Binding to access application under test via REST
    private static MessageBinding messageBinding;              // Binding to access application under test via JMS

    @BeforeAll
    static void initBeforeAll()
    {
        jexxaIntegrationTest = new JexxaIntegrationTest(BookStore.class);
        messageBinding = jexxaIntegrationTest.getMessageBinding();
        restBinding = jexxaIntegrationTest.getRESTBinding();
    }


    @Test
    void testStartupApplication()
    {
        //Arrange -
        var boundedContext = restBinding.getBoundedContext();

        //Act / Assert
        var result = boundedContext.contextName();

        //Assert
        assertEquals(BookStore.class.getSimpleName(), result);
    }

    @Test
    void testAddBook()
    {
        //Arrange
        var bookStoreService = restBinding.getRESTHandler(BookStoreService.class);

        var addedBooks = 5;
        var inStock = bookStoreService.postRequest(Integer.class, amountInStock, ANY_BOOK );

        //Act
        bookStoreService.postRequest(Void.class, addToStock, ANY_BOOK, addedBooks);
        var result = bookStoreService.postRequest(Integer.class, amountInStock, ANY_BOOK );

        //Assert
        assertEquals(inStock + addedBooks, result);
    }

    @Test
    void testSellLastBook()
    {
        //Arrange
        var bookStoreService = restBinding.getRESTHandler(BookStoreService.class);
        var messageListener = messageBinding.getMessageListener("BookStore", JMSConfiguration.MessagingType.TOPIC);

        bookStoreService.postRequest(Void.class, addToStock, ANY_BOOK, 5);
        var inStock = bookStoreService.postRequest(Integer.class, amountInStock, ANY_BOOK );

        //Act - Sell all books in stock
        for (int i = 0; i < inStock; ++i)
        {
            bookStoreService.postRequest(Void.class, sell, ANY_BOOK);
        }

        // Receive the jms message
        var result = messageListener
                .awaitMessage(5, TimeUnit.SECONDS)
                .pop(BookSoldOut.class);

        //Assert
        assertEquals(ANY_BOOK, result.isbn13());
    }

    @AfterAll
    static void shutDown()
    {
        jexxaIntegrationTest.shutDown();
    }
}
