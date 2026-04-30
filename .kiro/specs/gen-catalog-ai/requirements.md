# Requirements Document

## Introduction

GenCatalog AI is a multi-tier SaaS web application that automates e-commerce catalog enrichment. Users upload a CSV file containing raw product data (product_name, category, price), and the system generates professional product descriptions, keyword tags, SEO titles, and SEO meta descriptions for each product using a Hugging Face LLM. The enriched catalog is returned as a downloadable CSV.

The system is composed of four layers: a React frontend, a Java Spring Boot orchestration API, a Python FastAPI AI engine, and the Hugging Face Inference API as an external dependency.

---

## Glossary

- **Frontend**: The React web application responsible for file upload, progress display, results rendering, and CSV export.
- **API**: The Java Spring Boot service that accepts CSV uploads, orchestrates AI generation, and returns enriched results.
- **AI_Engine**: The Python FastAPI service that builds prompts, calls the Hugging Face API, validates and cleans AI output, and returns structured JSON.
- **HF_API**: The external Hugging Face Inference API (router.huggingface.co) used for LLM inference.
- **Product**: A single row from the input CSV with fields product_name, category, and price.
- **EnrichedProduct**: A Product extended with AI-generated description, tags, seo_title, and seo_description.
- **CatalogResponse**: The JSON payload returned by the API containing an array of EnrichedProducts, a total count, and processing_time_ms.
- **Fallback**: A pre-defined EnrichedProduct with placeholder values returned when AI generation fails after all retries.
- **HF_API_KEY**: The secret Bearer token used to authenticate requests to the HF_API, loaded exclusively from environment variables.

---

## Requirements

### Requirement 1: CSV File Upload

**User Story:** As an e-commerce manager, I want to upload a CSV file via drag and drop or file picker, so that I can submit my product catalog for AI enrichment.

#### Acceptance Criteria

1. WHEN a user drags and drops a file onto the upload zone or selects a file via the file picker, THE Frontend SHALL accept files with a `.csv` extension and initiate the upload.
2. WHEN a user attempts to upload a file that does not have a `.csv` extension, THE Frontend SHALL reject the file and display an error message identifying the invalid file type.
3. WHEN a CSV file upload is in progress, THE Frontend SHALL display an animated progress bar to indicate processing status.
4. WHEN the upload zone is in its idle state, THE Frontend SHALL display drag-and-drop instructions and a file picker affordance.

---

### Requirement 2: CSV Parsing and Validation

**User Story:** As a developer, I want the API to parse and validate uploaded CSV files, so that only well-formed product data is forwarded to the AI engine.

#### Acceptance Criteria

1. WHEN a valid CSV file is received, THE API SHALL parse it into a list of Product objects with product_name, category, and price fields populated.
2. WHEN a CSV file is missing one or more of the required columns (product_name, category, price), THE API SHALL return HTTP 400 with an error message listing the missing columns.
3. WHEN a CSV file contains a header row but no data rows, THE API SHALL return HTTP 400 with the message "CSV file contains no product rows".
4. WHEN a CSV row is missing one or more required field values, THE API SHALL skip that row, log a warning, and continue processing remaining rows.
5. WHEN the uploaded request does not use Content-Type multipart/form-data, THE API SHALL return HTTP 400 and reject the request.

---

### Requirement 3: Per-Product AI Generation Orchestration

**User Story:** As a developer, I want the API to call the AI engine for each valid product and aggregate results, so that every product in the catalog receives enriched content.

#### Acceptance Criteria

1. FOR EACH valid Product parsed from the CSV, THE API SHALL send a generation request to the AI_Engine and collect the result.
2. WHEN the AI_Engine returns a successful response for a product, THE API SHALL include the EnrichedProduct in the final results array.
3. WHEN the AI_Engine returns an error or is unreachable for a product, THE API SHALL include a Fallback entry for that product in the results array and continue processing remaining products.
4. WHEN the AI_Engine service is unreachable, THE API SHALL return HTTP 503 with the message "AI engine unavailable, please try again later".
5. WHEN all products have been processed, THE API SHALL return a CatalogResponse containing the products array, a total count equal to the number of input products, and processing_time_ms reflecting actual wall-clock elapsed time.

---

### Requirement 4: AI Prompt Construction

**User Story:** As a developer, I want the AI engine to build structured prompts from product data, so that the LLM receives consistent, well-formed input for every product.

#### Acceptance Criteria

1. WHEN a generation request is received, THE AI_Engine SHALL construct a prompt that includes the product_name, category, and price values from the request.
2. THE AI_Engine SHALL instruct the LLM to return output exclusively as a JSON object with the keys: description, tags, seo_title, and seo_description.
3. THE AI_Engine SHALL set max_tokens to 300 and temperature to 0.7 in every HF_API request.

---

### Requirement 5: Hugging Face API Integration

**User Story:** As a developer, I want the AI engine to call the Hugging Face Inference API with proper authentication, so that LLM inference is performed securely and reliably.

#### Acceptance Criteria

1. WHEN sending a request to the HF_API, THE AI_Engine SHALL include the HF_API_KEY as a Bearer token in the Authorization header.
2. WHEN the HF_API returns a successful response, THE AI_Engine SHALL extract the text content from the response and pass it to the JSON extraction algorithm.
3. WHEN the HF_API request exceeds 30 seconds, THE AI_Engine SHALL raise a TimeoutError to trigger the retry mechanism.
4. WHEN the HF_API returns HTTP 429, THE AI_Engine SHALL raise a RateLimitError to trigger the retry mechanism with a 2-second delay.
5. WHEN the HF_API returns any other HTTP error, THE AI_Engine SHALL raise an ApiError with the HTTP status code and message.

---

### Requirement 6: Retry and Fallback Logic

**User Story:** As a developer, I want the AI engine to retry failed requests and return fallback content on persistent failure, so that every product always receives an output entry.

#### Acceptance Criteria

1. WHEN a TimeoutError occurs, THE AI_Engine SHALL wait 1 second before retrying the HF_API request.
2. WHEN a RateLimitError occurs, THE AI_Engine SHALL wait 2 seconds before retrying the HF_API request.
3. WHEN AI output validation fails, THE AI_Engine SHALL retry the HF_API request with the same prompt.
4. THE AI_Engine SHALL make at most 3 total calls to the HF_API per product (1 initial attempt plus up to 2 retries).
5. WHEN all retry attempts are exhausted without a valid response, THE AI_Engine SHALL return a Fallback object containing all four required fields (description, tags, seo_title, seo_description) with non-empty placeholder values.

---

### Requirement 7: JSON Extraction from LLM Response

**User Story:** As a developer, I want the AI engine to robustly extract JSON from LLM responses, so that structured output is reliably obtained regardless of response formatting.

#### Acceptance Criteria

1. WHEN the raw LLM response text is valid JSON, THE AI_Engine SHALL parse it directly.
2. WHEN direct JSON parsing fails, THE AI_Engine SHALL attempt to extract the substring between the first `{` and the last `}` and parse that as JSON.
3. WHEN brace extraction also fails and the response contains a markdown code block delimited by ` ```json `, THE AI_Engine SHALL extract the content of that block and parse it as JSON.
4. WHEN all three extraction strategies fail, THE AI_Engine SHALL return null to trigger the retry or fallback mechanism.

---

### Requirement 8: Output Validation

**User Story:** As a developer, I want the AI engine to validate that all required fields are present in the LLM output, so that incomplete responses are detected and retried.

#### Acceptance Criteria

1. WHEN validating AI output, THE AI_Engine SHALL verify that the parsed JSON contains all four keys: description, tags, seo_title, and seo_description.
2. WHEN any required key is absent from the parsed JSON, THE AI_Engine SHALL treat the response as invalid and trigger a retry.
3. WHEN any required key is present but has an empty string value, THE AI_Engine SHALL treat the response as invalid and trigger a retry.
4. IF the parsed JSON is null or not a dictionary, THEN THE AI_Engine SHALL treat the response as invalid and trigger a retry.

---

### Requirement 9: Output Cleaning and Constraint Enforcement

**User Story:** As a developer, I want the AI engine to clean and enforce constraints on AI output, so that all enriched fields conform to the specified format and length limits.

#### Acceptance Criteria

1. WHEN cleaning the description field, THE AI_Engine SHALL strip special symbols, normalize whitespace, and truncate to at most 150 words.
2. WHEN cleaning the tags field, THE AI_Engine SHALL split by comma, trim each tag, remove empty entries, and retain at most 8 tags.
3. WHEN cleaning the seo_title field, THE AI_Engine SHALL strip special symbols, trim whitespace, and truncate to at most 60 characters.
4. WHEN cleaning the seo_description field, THE AI_Engine SHALL strip special symbols, trim whitespace, and truncate to at most 160 characters.
5. WHEN seo_title is truncated to fit within 60 characters, THE AI_Engine SHALL append "..." to indicate truncation.
6. WHEN seo_description is truncated to fit within 160 characters, THE AI_Engine SHALL append "..." to indicate truncation.

---

### Requirement 10: Results Display and CSV Export

**User Story:** As an e-commerce manager, I want to view enriched products in a table and download the results as a CSV, so that I can review and use the generated catalog content.

#### Acceptance Criteria

1. WHEN the API returns a CatalogResponse, THE Frontend SHALL render all EnrichedProducts in a scrollable results table showing all seven fields (product_name, category, price, description, tags, seo_title, seo_description).
2. WHEN results are displayed, THE Frontend SHALL provide a download button that exports the enriched data as a CSV file.
3. WHEN the download button is clicked, THE Frontend SHALL generate and download a CSV file containing all EnrichedProduct rows.
4. WHEN processing completes, THE Frontend SHALL display the total number of products processed and the processing time.

---

### Requirement 11: Security — API Key Protection

**User Story:** As a security engineer, I want the HF_API_KEY to be handled securely throughout the system, so that the secret is never exposed in code, logs, or HTTP responses.

#### Acceptance Criteria

1. THE AI_Engine SHALL load HF_API_KEY exclusively from the environment variable named `HF_API_KEY` and never from hardcoded values.
2. THE AI_Engine SHALL never include the HF_API_KEY value in any HTTP response body, response header, or log output.
3. THE API SHALL never include the HF_API_KEY value in any HTTP response body, response header, or log output.
4. WHERE the application is deployed, THE system SHALL require the `.env` file to be listed in `.gitignore` to prevent accidental commits of the secret.

---

### Requirement 12: Error Responses

**User Story:** As a developer, I want all error conditions to return structured, descriptive HTTP responses, so that clients can handle failures gracefully.

#### Acceptance Criteria

1. WHEN the CSV is invalid or missing required columns, THE API SHALL return HTTP 400 with a JSON body containing an `error` field describing the problem.
2. WHEN the CSV contains no data rows, THE API SHALL return HTTP 400 with `{"error": "CSV file contains no product rows"}`.
3. WHEN the AI_Engine service is unreachable, THE API SHALL return HTTP 503 with `{"error": "AI engine unavailable, please try again later"}`.
4. WHEN the Frontend receives an error response from the API, THE Frontend SHALL display the error message to the user in a visible, non-blocking notification.
5. IF an unhandled exception occurs in the AI_Engine during a generation request, THEN THE AI_Engine SHALL return a Fallback response rather than propagating the exception to the API.
