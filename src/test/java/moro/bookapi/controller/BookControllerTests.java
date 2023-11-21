package moro.bookapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import moro.bookapi.model.BookDto;
import moro.bookapi.model.RatingDto;

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

        assertNotNull(response);
        assertEquals(1, response.get("count"));
    }
     @Test
    void getBooksEmptyTitleTest() {
        // Mock external API response for empty title
        String mockApiResponse = "{ \"count\": 0, \"results\": [] }";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockApiResponse);

        // Call getBooks method with empty title
        Map<String, Object> response = bookController.getBooks("", 1);
        assertEquals(0, response.get("count"));
    }

    @Test
    void getBooksInvalidPageTest() {
        Map<String, Object> response = bookController.getBooks("Test Book", -1);
        // Check if the "error" key exists in the response
        assertTrue(response.containsKey("error"));
        assertEquals("Page number must be a positive integer", response.get("error"));    
    }

    @Test
    void getBooksExceptionHandlingTest() {
        // Simulate a RestClientException
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RestClientException("Error"));

        // Call getBooks method and expect an error response
        Map<String, Object> response = bookController.getBooks("Test Book", 1);
        assertTrue(response.containsKey("error"));
    }

    @Test
    void getAverageRatingPerMonthSuccessTest() {
    // Mock database response
    List<RatingDto> mockDbResponse = new ArrayList<>();
    RatingDto ratingDto = new RatingDto();
    ratingDto.setYear(2023);
    ratingDto.setMonth(11);
    ratingDto.setAverageRating(5);
    mockDbResponse.add(ratingDto);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenReturn(mockDbResponse);

    // Call getAverageRatingPerMonth method
    Map<String, Object> response = bookController.getAverageRatingPerMonth(1);

    // Check that the "bookId" key exists and its value is the expected book ID
    assertTrue(response.containsKey("bookId"));
    assertEquals(1, response.get("bookId"));

    // Check that the "monthlyRatings" key exists and its value is a list with one item
    assertTrue(response.containsKey("monthlyRatings"));
    assertTrue(response.get("monthlyRatings") instanceof List);
    assertEquals(1, ((List) response.get("monthlyRatings")).size());
    }

    @Test
    void getAverageRatingPerMonthNoRatingsTest() {
        // Mock database response for no ratings
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenReturn(new ArrayList<>());

        // Call getAverageRatingPerMonth method
        Map<String, Object> response = bookController.getAverageRatingPerMonth(1);

        // Check that the "message" key exists and its value is the expected message
        assertTrue(response.containsKey("message"));
        assertEquals("No ratings found for the given book ID", response.get("message"));
    }

    @Test
    void getAverageRatingPerMonthExceptionHandlingTest() {
        // Simulate a DataAccessException
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenThrow(new DataAccessException("Error") {});

        // Call getAverageRatingPerMonth method and expect an error response
        Map<String, Object> response = bookController.getAverageRatingPerMonth(1);
        assertTrue(response.containsKey("error"));
    }
    @Test
    void getTopBooksSuccessTest() {
        // Mock database response
        List<BookDto> mockDbResponse = new ArrayList<>();
        BookDto bookDto = new BookDto();
        bookDto.setId(1);
        bookDto.setRating(4.5);
        mockDbResponse.add(bookDto);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenReturn(mockDbResponse);

        // Call getTopBooks method
        Map<String, Object> response = bookController.getTopBooks(1);

        // Check that the "books" key exists and its value is a list with one item
        assertTrue(response.containsKey("books"));
        assertTrue(response.get("books") instanceof List);
        assertEquals(1, ((List) response.get("books")).size());
    }

    @Test
    void getTopBooksInvalidNumberTest() {
        // Call getTopBooks method with a non-positive number
        Map<String, Object> response = bookController.getTopBooks(0);

        // Check that the "error" key exists and its value is the expected error message
        assertTrue(response.containsKey("error"));
        assertEquals("Number must be a positive integer", response.get("error"));
    }

    @Test
    void getTopBooksSortedTest() {
        // Mock database response
        List<BookDto> mockDbResponse = new ArrayList<>();
        BookDto bookDto1 = new BookDto();
        bookDto1.setId(1);
        bookDto1.setRating(4.5);
        mockDbResponse.add(bookDto1);

        BookDto bookDto2 = new BookDto();
        bookDto2.setId(2);
        bookDto2.setRating(3.8);
        mockDbResponse.add(bookDto2);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenReturn(mockDbResponse);

        // Call getTopBooks method
        Map<String, Object> response = bookController.getTopBooks(2);

        // Check that the "books" key exists and its value is a list with two items
        assertTrue(response.containsKey("books"));
        assertTrue(response.get("books") instanceof List);
        assertEquals(2, ((List) response.get("books")).size());

        // Check that the books are sorted in descending order of their ratings
        List<BookDto> topBooks = (List<BookDto>) response.get("books");
        assertEquals(4.5, topBooks.get(0).getRating());
        assertEquals(3.8, topBooks.get(1).getRating());
    }
}
