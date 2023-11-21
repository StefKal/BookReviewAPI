package moro.bookapi.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import moro.bookapi.model.AuthorDto;
import moro.bookapi.model.BookDto;
import moro.bookapi.model.RatingDto;


@RestController
public class BookController {
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    public BookController(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search books", description = "Search for books by title")
    @ApiResponse(responseCode = "200", description = "Successful response", 
                 content = @Content(
                     mediaType = "application/json", 
                     schema = @Schema(implementation = Map.class),
                     examples = @ExampleObject(
                         name = "Example Book Search Response",
                         summary = "Example response for book search",
                         value = "{\"next\": null, \"previous\": null, \"count\": 1, \"results\": [{\"id\": 64317, \"title\": \"The Great Gatsby\", \"authors\": [{\"name\": \"Fitzgerald, F. Scott (Francis Scott)\", \"birthYear\": 1896, \"deathYear\": 1940}], \"languages\": [\"en\"], \"downloadCount\": 24469, \"rating\": 2.6, \"reviews\": [\"Such a Great book\", \"The worst book I've read\", \"Noice\", \"How to write reviews\", \"bleh\"]}]}"
                     )
                 ))
    @ResponseBody
    public Map<String, Object> getBooks(
            @RequestParam(value = "title", defaultValue = "") String title,
            @RequestParam(value = "page", defaultValue = "1") int page) {
        
        // Enforce positive page number
        if (page <= 0) {
            return Collections.singletonMap("error", "Page number must be a positive integer");
        }

        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String url = "https://gutendex.com/books?search=" + encodedTitle;

        try {
            // Make the API call and get the response as a Map
            String resp = restTemplate.getForObject(url, String.class);
            JsonParser springParser = JsonParserFactory.getJsonParser();
            Map<String, Object> map = springParser.parseMap(resp);

            // Extract the relevant information from the response
            List<Map<String, Object>> results = (List<Map<String, Object>>) map.get("results");

            // Create a new map with paginated and filtered results
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("count", map.get("count"));
            responseMap.put("next", map.get("next"));
            responseMap.put("previous", map.get("previous"));
            responseMap.put("results", extractBooks(results));

            return responseMap;
        } catch (RestClientException e) {
            // handle the exception
            return Collections.singletonMap("error", "An error occurred while fetching the books: " + e.getMessage());
        }
    }

    private List<BookDto> extractBooks(List<Map<String, Object>> results) {
        List<BookDto> books = new ArrayList<>();
        for (Map<String, Object> result : results) {
            BookDto bookDto = new BookDto();
                bookDto.setId((int) result.get("id"));
                bookDto.setTitle((String) result.get("title"));
                bookDto.setAuthors(extractAuthors(result));
                bookDto.setLanguages((List<String>) result.get("languages"));
                bookDto.setDownloadCount((int) result.get("download_count"));
                // Fetch and set reviews for this book
                int bookId = (int) result.get("id");
                embedReviewDetailsInBook(bookDto, bookId);
                books.add(bookDto);
        }
        return books;
    }


    private void embedReviewDetailsInBook(BookDto bookDto, int bookId) {
        String sql = "SELECT review_text, rating FROM reviews WHERE book_id = ?";

        List<String> reviewTexts = new ArrayList<>();
        AtomicInteger totalRating = new AtomicInteger(0);
        AtomicInteger reviewCount = new AtomicInteger(0);

        jdbcTemplate.query(sql, ps -> ps.setInt(1, bookId), (rs, rowNum) -> {
            reviewTexts.add(rs.getString("review_text"));
            totalRating.addAndGet(rs.getInt("rating"));
            reviewCount.incrementAndGet();
            return null;
        });

        // Set average rating and review texts in bookDto
        double averageRating = reviewCount.get() > 0 ? (double) totalRating.get() / reviewCount.get() : 0;
        bookDto.setRating(averageRating);
        bookDto.setReviews(reviewTexts);
    }
    
    private List<AuthorDto> extractAuthors(Map<String, Object> result) {
        List<Map<String, Object>> authors = (List<Map<String, Object>>) result.get("authors");
        List<AuthorDto> authorDtos = new ArrayList<>();

        for (Map<String, Object> author : authors) {
            AuthorDto authorDto = new AuthorDto();
            authorDto.setName((String) author.get("name"));
            // Check for null before trying to access the Integer values
            authorDto.setBirthYear(author.get("birth_year") != null ? ((Integer) author.get("birth_year")) : null);
            authorDto.setDeathYear(author.get("death_year") != null ? ((Integer) author.get("death_year")) : null);
            
            authorDtos.add(authorDto);
        }

        return authorDtos;
    }

    @GetMapping(value = "/search/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get book by ID", description = "Retrieve a book and its details by ID")
    @ApiResponse(responseCode = "200", description = "Successful response", 
                 content = @Content(
                     mediaType = "application/json", 
                     schema = @Schema(implementation = BookDto.class)))
    public BookDto getBookById(@PathVariable("id") int bookId) {
        String url = "https://gutendex.com/books?ids=" + String.valueOf(bookId);
        try {
            String resp = restTemplate.getForObject(url, String.class);
            JsonParser springParser = JsonParserFactory.getJsonParser();
            Map<String, Object> map = springParser.parseMap(resp);
           // Extract the relevant information from the response
            List<Map<String, Object>> results = (List<Map<String, Object>>) map.get("results");
            return extractBooks(results).get(0);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred ", e);
        }
    }


    @ApiResponse(responseCode = "200", description = "Successful response", 
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    name = "Example Top Book Search Response",
                    summary = "Example response for top book search",
                    value = "{\n" + 
                    "  \"books\": [\n" + 
                    "    {\n" + 
                    "      \"id\": 1,\n" + 
                    "      \"title\": \"Title of First Book\",\n" + 
                    "      \"authors\": [\"Author1\", \"Author2\"],\n" + 
                    "      \"languages\": [\"en\"],\n" + 
                    "      \"downloadCount\": 42,\n" + 
                    "      \"rating\": 4.5,\n" + 
                    "      \"reviews\": \"Some review text...\"\n" + 
                    "    },\n" + 
                    "    {\n" + 
                    "      \"id\": 2,\n" + 
                    "      \"title\": \"Title of Second Book\",\n" + 
                    "      \"authors\": [\"Author3\"],\n" + 
                    "      \"languages\": [\"en\", \"es\"],\n" + 
                    "      \"downloadCount\": 35,\n" + 
                    "      \"rating\": 3.8,\n" + 
                    "      \"reviews\": \"Another review text...\"\n" + 
                    "    }\n" + 
                    "  ]\n" + 
                    "}" 
        )
            ))
    @GetMapping(value = "/top", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get top books", description = "Get the top N rated books")
    public Map<String, Object> getTopBooks(
        @RequestParam(value = "n", defaultValue = "10") int n
    ){
        if (n <= 0) {
            return Collections.singletonMap("error", "Number must be a positive integer");
        }
    
        String sql = "SELECT book_id, AVG(rating) as average_rating " +
                     "FROM reviews GROUP BY book_id ORDER BY average_rating DESC LIMIT ?";
    
        RowMapper<BookDto> rowMapper = (rs, rowNum) -> {
            BookDto book = new BookDto();
            book.setId(rs.getInt("book_id"));
            // Fetch book details from the Gutendex API or another source as needed
            fetchBookDetails(book);
            book.setRating(rs.getDouble("average_rating")); // Setting the average rating
            return book;
        };
    
        List<BookDto> topBooks = jdbcTemplate.query(sql, rowMapper, n);
    
        Map<String, Object> response = new HashMap<>();
        response.put("books", topBooks);
        return response;
    }
    
    private void fetchBookDetails(BookDto book) {
        String url = "https://gutendex.com/books?ids=" + book.getId();
    
        try {
            String resp = restTemplate.getForObject(url, String.class);
            JsonParser jsonParser = JsonParserFactory.getJsonParser();
            Map<String, Object> responseMap = jsonParser.parseMap(resp);
    
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
            if (results != null && !results.isEmpty()) {
                Map<String, Object> bookData = results.get(0); // Get the first book's details
    
                book.setTitle((String) bookData.get("title"));
                book.setLanguages((List<String>) bookData.get("languages"));
                book.setAuthors(extractAuthors(bookData));
            }
        } catch (RestClientException e) {
        }
    }

    @ApiResponse(responseCode = "200", description = "Successful response", 
                content = @Content(
                    mediaType = "application/json", 
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(
                        name = "Example Average Rating Search Response",
                        summary = "Example response for average rating search",
                        value = "{\n" +
                        "  \"monthlyRatings\": [\n" +
                        "    {\n" +
                        "      \"year\": 2023,\n" +
                        "      \"month\": 11,\n" +
                        "      \"averageRating\": 5\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"bookId\": 1\n" +
                        "}"
                    )
                ))
    @GetMapping(value = "/averageRatingPerMonth", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get average rating per month for a book", description = "Returns the average rating per month for a given book ID")
    public Map<String, Object> getAverageRatingPerMonth(@RequestParam(value = "bookId") int bookId) {
        String sql = "SELECT strftime('%Y', created_at) as year, strftime('%m', created_at) as month, AVG(rating) as average_rating " +
        "FROM reviews " +
        "WHERE book_id = ? " +
        "GROUP BY strftime('%Y', created_at), strftime('%m', created_at) " +
        "ORDER BY year, month";

        try {
            RowMapper<RatingDto> rowMapper = new RowMapper<RatingDto>() {
                @Override
                public RatingDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                    RatingDto monthlyRating = new RatingDto();
                    monthlyRating.setYear(rs.getInt("year"));
                    monthlyRating.setMonth(rs.getInt("month"));
                    monthlyRating.setAverageRating(rs.getDouble("average_rating"));
                    return monthlyRating;
                }
            };

            List<RatingDto> monthlyRatings = jdbcTemplate.query(sql, rowMapper, bookId);

            if (monthlyRatings.isEmpty()) {
                return Collections.singletonMap("message", "No ratings found for the given book ID");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("bookId", bookId);
            response.put("monthlyRatings", monthlyRatings);
            return response;
        } catch (Exception e) {
            return Collections.singletonMap("error", "An error occurred: " + e.getMessage());
        }
    }


    
}

