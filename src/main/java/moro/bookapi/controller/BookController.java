package moro.bookapi.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import moro.bookapi.model.AuthorDto;
import moro.bookapi.model.BookDto;


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
}

