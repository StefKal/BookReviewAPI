## Book API
This document provides instructions for using the endpoints of a RESTful API designed for managing and retrieving book-related data, including submitting reviews, listing top-rated books, searching books, and getting average ratings per month for a specific book.

## Features

- Search books by title
- Submit a book review
- Get top "N" reviewed books
- Get average rating per month of a book

## Running the project

- Assuming you have docker set up, (if not download it from https://www.docker.com).
- Open a terminal inside the project directory and run
```
$ docker compose up --build
```

## Interactive Documentation
- Now go tο your browser at ```localhost:8080/docs.html```
- There you can find all the available endpoints of the API

## Endpoints
### 1. Submit a Book Review
- **Endpoint:** `/reviews`
- **Method:** POST
- **Description:** Submit a review for a book.
- **Request Body:** JSON containing `bookId`, `rating`, and `reviewText`.
- **Example Request:**
  ```json
  {
    "bookId": 1,
    "rating": 5,
    "reviewText": "Amazing book!"
  }

### 2. Get Top Books
- **Endpoint:** `/top`
- **Method:** GET
- **Description:** Retrieve top N rated books.
- **Parameters:** `n` (integer, optional, default 10) - Number of top books to retrieve.
- **Example Request:** `GET /top?n=5`
- **Response:** 200 status code with a list of top books in JSON.

### 3. Search Books
- **Endpoint:** `/search`
- **Method:** GET
- **Description:** Search for books by title.
- **Parameters:** 
  - `title` (string, optional, default "")
  - `page` (integer, optional, default 1)
- **Example Request:** `GET /search?title=Great Gatsby&page=2`
- **Response:** 200 status code with search results in JSON.

### 4. Get Average Rating Per Month
- **Endpoint:** `/averageRatingPerMonth`
- **Method:** GET
- **Description:** Returns the average rating per month for a given book ID.
- **Parameters:** `bookId` (integer, required) - Book ID for which to get the average rating.
- **Example Request:** `GET /averageRatingPerMonth?bookId=1`
- **Response:** 200 status code with average monthly ratings in JSON.

## Usage 
To use these endpoints, ensure that your API server is running locally on port 8080. You can then make HTTP requests to the endpoints using tools like curl, Postman, or programmatically through HTTP client libraries in various programming languages.

## Error Handling
Responses to invalid requests will include an appropriate HTTP status code and, where possible, a JSON body explaining the error.

## Testing
- Open your docker container terminal and run the following command 
```
> mvn test
```