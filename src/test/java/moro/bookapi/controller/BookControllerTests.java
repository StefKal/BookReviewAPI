package moro.bookapi.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

class BookControllerTests {

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private RestTemplate restTemplate;

    private BookController bookController;

    @BeforeEach
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        bookController = new BookController(jdbcTemplate, restTemplate);
    }

    @Test
    void getBooksSuccessTest() {

        // Mock external API response
        String mockApiResponse = "{\n" +
                "  \"next\": null,\n" +
                "  \"previous\": null,\n" +
                "  \"count\": 1,\n" +
                "  \"results\": [\n" +
                "    {\n" +
                "      \"id\": 64317,\n" +
                "      \"title\": \"Test Book\",\n" +
                "      \"authors\": [\n" +
                "        {\n" +
                "          \"name\": \"Fitzgerald, F. Scott (Francis Scott)\",\n" +
                "          \"birth_year\": 1896,\n" +
                "          \"death_year\": 1940\n" +
                "        }\n" +
                "      ],\n" +
                "      \"languages\": [\n" +
                "        \"en\"\n" +
                "      ],\n" +
                "      \"download_count\": 24469,\n" +
                "      \"rating\": 2.6,\n" +
                "      \"reviews\": [\n" +
                "        \"Such a Great book\",\n" +
                "        \"The worst book I've read\",\n" +
                "        \"Noice\",\n" +
                "        \"How to write reviews\",\n" +
                "        \"bleh\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockApiResponse);

        // Call getBooks method
        Map<String, Object> response = bookController.getBooks("Test Book", 1);

        // Assertions
        assertNotNull(response);
        assertEquals(1, response.get("count"));
    }

}
