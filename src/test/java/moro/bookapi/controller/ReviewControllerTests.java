package moro.bookapi.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;

import moro.bookapi.model.Review;

public class ReviewControllerTests {

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DataSource dataSource;

    @InjectMocks
    private ReviewController reviewController;

    @BeforeEach
    public void setup() {
        jdbcTemplate = mock(JdbcTemplate.class);
        dataSource = mock(DataSource.class);
        reviewController = new ReviewController(dataSource);
    }

    @Test
    public void testSubmitEmptyReview() {
        long bookId = 1;
        int rating = 2;
        String reviewText = null; // Review text is null
        Review review = new Review(bookId, rating, reviewText);

        ResponseEntity<Review> response = reviewController.submitReview(review);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testSubmitReviewOverFiveStars() {
        long bookId = 1;
        int rating = 6; // Rating above 5
        String reviewText = "Great book!";
        Review review = new Review(bookId, rating, reviewText);

        ResponseEntity<Review> response = reviewController.submitReview(review);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testSubmitReviewUnderZeroStars() {
        long bookId = 1;
        int rating = -1; // Rating below 0
        String reviewText = "Great book!";
        Review review = new Review(bookId, rating, reviewText);

        ResponseEntity<Review> response = reviewController.submitReview(review);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
