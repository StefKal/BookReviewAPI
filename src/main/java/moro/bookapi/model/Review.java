package moro.bookapi.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String reviewText;

    public Review(Long bookId, Integer rating, String reviewText) {
        this.bookId = bookId;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public int getRating() {
        return this.rating;
    }

    public Long getBookId() {
        return this.bookId;
    }

    public String getReviewText() {
        return this.reviewText;
    }

}
