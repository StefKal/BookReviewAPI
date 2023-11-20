package moro.bookapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.sql.DataSource;
import moro.bookapi.model.Review;

@RestController
public class ReviewController {
    private final JdbcTemplate jdbcTemplate;

    public ReviewController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostMapping("/reviews")
    @Operation(
        summary = "Submit a book review",
        description = "Endpoint to submit a review for a book",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Review.class),
                examples = {
                    @ExampleObject(
                        name = "Simple Review",
                        summary = "Simple example",
                        description = "A simple review example",
                        value = "{\"bookId\": 1, \"rating\": 5, \"reviewText\": \"Amazing book!\"}"
                    )
                }
            )
        )
    )
    @ApiResponse(responseCode = "201", description = "Review successfully created", 
                 content = { @Content(mediaType = "application/json", 
                                      schema = @Schema(implementation = Review.class)) })
    public ResponseEntity<Review> submitReview(@RequestBody Review review) {
        try {
            // Validate the review
            if (review.getRating() < 0 || review.getRating() > 5 || review.getReviewText() == null) {
                return ResponseEntity.badRequest().build();
            }

            // Insert review into the database
            String sql = "INSERT INTO reviews (book_id, rating, review_text, created_at) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, review.getBookId(), review.getRating(), review.getReviewText(), review.getTimestamp());

            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (Exception e) {
            // Log the exception for debugging purposes
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}