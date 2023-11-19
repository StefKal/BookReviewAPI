package moro.bookapi.model;

public class ReviewDto {

    private int rating;
    public void setRating(int rating) {
        this.rating = rating;
    }
    private String reviewText;

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }
    public String getReviewText() {
        return reviewText;
    }
    public int getRating() {
        return rating;
    }

}
