# Implementation Plan: GenCatalog AI

## Overview

Incremental build across three tiers: Python AI engine first (core logic + PBT), then Java Spring Boot orchestration layer, then React frontend. Each phase wires into the previous before moving on.

## Tasks

- [x] 1. Project scaffolding and configuration
  - Create `/frontend`, `/backend`, `/ai-engine` root directories
  - Create `ai-engine/requirements.txt` with: fastapi, uvicorn, httpx, pydantic, python-dotenv, pytest, hypothesis
  - Create `backend/pom.xml` with Spring Boot 3, spring-boot-starter-web, spring-boot-starter-webflux, spring-boot-starter-validation, opencsv, jqwik dependencies
  - Bootstrap React app in `/frontend` using Vite + TypeScript template; install tailwindcss, framer-motion, react-dropzone, papaparse, axios
  - Create root `.env` with `HF_API_KEY=`, `AI_ENGINE_URL=http://localhost:8000`, `VITE_API_URL=http://localhost:8080`
  - Verify `.env` is present in `.gitignore`
  - _Requirements: 11.4_

- [x] 2. Python AI engine — core structure and prompt builder
  - Create `ai-engine/main.py` with FastAPI app, CORS middleware allowing `http://localhost:5173`, and a `POST /generate` route stub
  - Create `ai-engine/models.py` with Pydantic models: `ProductRequest` (product_name, category, price) and `EnrichedProduct` (description, tags, seo_title, seo_description)
  - Create `ai-engine/prompt_builder.py` with `build_prompt(product: ProductRequest) -> str` that embeds all three fields and instructs the LLM to return only a JSON object with the four required keys
  - _Requirements: 4.1, 4.2, 4.3_

  - [x] 2.1 Write unit tests for `build_prompt`
    - Assert all three product field values appear as substrings in the returned prompt string
    - Assert the prompt contains the four required output key names
    - _Requirements: 4.1, 4.2_

  - [x] 2.2 Write property test for `build_prompt` — Property 12
    - **Property 12: Prompt contains all product fields**
    - **Validates: Requirements 4.1**
    - Use `hypothesis` `@given` with strategies for non-empty strings for all three fields; assert each value is a substring of the returned prompt

- [x] 3. Python AI engine — JSON extraction
  - Create `ai-engine/json_extractor.py` with `extract_json(raw_text: str) -> dict | None` implementing all three extraction strategies: direct parse, brace-bounded substring, markdown code block
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 3.1 Write unit tests for `extract_json`
    - Test Strategy 1: raw text is valid JSON
    - Test Strategy 2: JSON embedded in surrounding prose
    - Test Strategy 3: JSON inside ` ```json ` block
    - Test all-fail case returns `None`
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 3.2 Write property test for `extract_json` — Property 9
    - **Property 9: JSON extraction round-trip**
    - **Validates: Requirements 7.1, 7.2, 7.3**
    - Use `hypothesis` to generate dicts with the four required keys; serialize to string, apply `extract_json`, assert result equals original dict

- [x] 4. Python AI engine — output validation and cleaning
  - Create `ai-engine/validator.py` with `validate_fields(json_data) -> bool` checking all four required keys exist and are non-empty strings
  - Create `ai-engine/cleaner.py` with `clean_output(json_data: dict) -> dict` implementing: description strip + normalize + 150-word truncation; tags split/trim/dedupe/cap-at-8; seo_title strip/trim/60-char truncation with "..."; seo_description strip/trim/160-char truncation with "..."
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [x] 4.1 Write unit tests for `validate_fields`
    - Test: all keys present and non-empty → True
    - Test: missing key → False
    - Test: key present with empty string → False
    - Test: input is None → False
    - Test: input is not a dict → False
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 4.2 Write unit tests for `clean_output`
    - Test description truncation at 150 words
    - Test tags capped at 8, empty entries removed
    - Test seo_title truncated to 60 chars with "..."
    - Test seo_description truncated to 160 chars with "..."
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [x] 4.3 Write property tests for `clean_output` — Properties 2, 3, 4, 5
    - **Property 2: Description word count constraint** — `len(result["description"].split()) <= 150`
    - **Property 3: Tag count constraint** — `len([t for t in result["tags"].split(",") if t.strip()]) <= 8`
    - **Property 4: SEO title length constraint** — `len(result["seo_title"]) <= 60`
    - **Property 5: SEO description length constraint** — `len(result["seo_description"]) <= 160`
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4**
    - Use `hypothesis` to generate arbitrary string values for each field; assert constraints hold after cleaning

- [x] 5. Python AI engine — Hugging Face client and retry logic
  - Create `ai-engine/hf_client.py` with `call_hugging_face(prompt: str) -> str` using `httpx.AsyncClient`; load `HF_API_KEY` from env via `python-dotenv`; set 30s timeout; raise `TimeoutError`, `RateLimitError` (429), or `ApiError` for other HTTP errors
  - Create `ai-engine/exceptions.py` defining `RateLimitError` and `ApiError`
  - Implement `generate_product_content(product: ProductRequest) -> EnrichedProduct` in `ai-engine/generator.py` with retry loop (max 3 total calls): call `call_hugging_face` → `extract_json` → `validate_fields` → `clean_output`; on `TimeoutError` wait 1s, on `RateLimitError` wait 2s; return fallback after exhausting retries
  - Implement `build_fallback(product: ProductRequest) -> EnrichedProduct` returning placeholder values with non-empty strings for all four fields; `seo_title` uses `product_name[:59]`
  - Wire `generate_product_content` into the `POST /generate` route in `main.py`
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5, 11.1, 11.2, 12.5_

  - [x] 5.1 Write unit tests for retry and fallback logic
    - Mock `call_hugging_face` to raise `TimeoutError` twice then succeed; assert exactly 3 calls made and valid result returned
    - Mock to always raise errors; assert fallback returned and call count ≤ 3
    - Mock to return invalid JSON twice then valid; assert valid result returned
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 5.2 Write property test for retry bound — Property 6
    - **Property 6: Retry attempt bound**
    - **Validates: Requirements 6.4**
    - Use `hypothesis` to generate arbitrary failure sequences; instrument call counter; assert total HF API calls ≤ 3

  - [x] 5.3 Write property test for fallback completeness — Property 8
    - **Property 8: Fallback completeness**
    - **Validates: Requirements 6.5**
    - Use `hypothesis` to generate arbitrary `ProductRequest` values; call `build_fallback`; assert all four fields are non-empty strings

- [x] 6. Checkpoint — Python AI engine
  - Ensure all Python unit tests and property tests pass: `cd ai-engine && pytest`
  - Verify `POST /generate` returns a valid `EnrichedProduct` JSON when called with a sample product
  - Ask the user if any questions arise before proceeding.

- [x] 7. Java Spring Boot — project structure and CSV parser
  - Create `backend/src/main/java/com/gencatalog/` package structure with sub-packages: `controller`, `service`, `model`, `client`, `exception`
  - Create `model/Product.java` and `model/EnrichedProduct.java` POJOs with all required fields and getters/setters
  - Create `model/CatalogResponse.java` with `products`, `total`, and `processing_time_ms` fields
  - Create `exception/CsvParseException.java` and `exception/AiEngineException.java`
  - Create `service/CsvParserService.java` implementing `parseCsv(MultipartFile file) -> List<Product>`: validate headers (product_name, category, price), skip invalid rows with log warning, throw `CsvParseException` on missing headers or empty file
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 7.1 Write JUnit 5 tests for `CsvParserService`
    - Test: valid CSV with 3 rows → list of 3 Products
    - Test: CSV missing required column → `CsvParseException`
    - Test: CSV with headers but no data rows → `CsvParseException`
    - Test: CSV row missing a field value → row skipped, others returned
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 7.2 Write jqwik property test for CSV parse completeness — Property 10
    - **Property 10: CSV parse completeness**
    - **Validates: Requirements 2.1**
    - Use `@Property` with `@ForAll` to generate lists of valid Product data; serialize to CSV string; parse with `CsvParserService`; assert each product_name, category, price is preserved without mutation

- [x] 8. Java Spring Boot — AI engine client and catalog service
  - Create `client/AiEngineClient.java` using Spring `WebClient`; read `AI_ENGINE_URL` from `application.properties`; implement `callAiEngine(Product product) -> EnrichedProduct`; throw `AiEngineException` on HTTP error or timeout; never return null
  - Create `service/CatalogService.java` implementing `processCatalog(MultipartFile file) -> CatalogResponse`: call `parseCsv`, iterate products, call `callAiEngine` per product, catch `AiEngineException` and insert fallback entry, record wall-clock time, return `CatalogResponse`
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 8.1 Write JUnit 5 + Mockito tests for `CatalogService`
    - Mock `AiEngineClient` to return success for all products; assert result count equals input count
    - Mock `AiEngineClient` to throw `AiEngineException` for one product; assert fallback entry present and other products enriched
    - Assert `processing_time_ms` is non-negative
    - _Requirements: 3.1, 3.2, 3.3, 3.5_

  - [x] 8.2 Write JUnit 5 + Mockito tests for `AiEngineClient`
    - Mock `WebClient` to return 503; assert `AiEngineException` thrown
    - Mock `WebClient` to return valid JSON; assert `EnrichedProduct` returned with all fields populated
    - _Requirements: 3.4_

  - [x] 8.3 Write jqwik property test for complete output coverage — Property 1
    - **Property 1: Complete output coverage**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - Use `@Property` with `@ForAll` to generate lists of valid Products; mock `AiEngineClient` to alternate success/failure; assert `CatalogResponse.products.size()` equals input list size

- [x] 9. Java Spring Boot — REST controller and error handling
  - Create `controller/CatalogController.java` with `POST /process-catalog` accepting `multipart/form-data`; delegate to `CatalogService`; handle `CsvParseException` → HTTP 400 JSON error; handle AI engine unreachable → HTTP 503 JSON error
  - Create `GlobalExceptionHandler.java` using `@RestControllerAdvice` to return `{"error": "..."}` JSON for all mapped exceptions
  - Configure CORS in `WebConfig.java` to allow `http://localhost:5173`
  - Add `application.properties` with `server.port=8080`, `ai.engine.url=${AI_ENGINE_URL:http://localhost:8000}`
  - _Requirements: 2.2, 2.3, 2.5, 3.4, 12.1, 12.2, 12.3_

  - [x] 9.1 Write Spring MockMvc tests for `CatalogController`
    - Test: valid CSV multipart upload → 200 with CatalogResponse JSON
    - Test: non-CSV content-type → 400
    - Test: CSV missing columns → 400 with error field
    - Test: empty CSV → 400 with "CSV file contains no product rows"
    - _Requirements: 2.2, 2.3, 2.5, 12.1, 12.2_

- [x] 10. Checkpoint — Java backend
  - Ensure all Java tests pass: `cd backend && mvn test`
  - Verify `POST /process-catalog` with a sample CSV returns a valid `CatalogResponse`
  - Ask the user if any questions arise before proceeding.

- [x] 11. React frontend — project setup and types
  - Configure Tailwind CSS in `frontend/tailwind.config.ts` and `frontend/src/index.css` with dark theme base (`bg-gray-950`, `text-white`)
  - Create `frontend/src/types.ts` exporting `Product`, `EnrichedProduct`, and `CatalogResponse` TypeScript interfaces matching the design document
  - Create `frontend/src/api.ts` with `processCatalog(file: File): Promise<CatalogResponse>` using `axios` to `POST /process-catalog` as `multipart/form-data` to `VITE_API_URL`
  - _Requirements: 1.1, 10.1_

- [x] 12. React frontend — drag & drop upload component
  - Create `frontend/src/components/UploadZone.tsx` using `react-dropzone`; accept only `.csv` files; show idle state with drag-and-drop instructions and file picker button; reject non-CSV files and display an inline error message; on valid file selection call the `onFileAccepted` prop
  - Apply Tailwind dark theme styles and `framer-motion` fade-in animation on mount
  - _Requirements: 1.1, 1.2, 1.4_

- [x] 13. React frontend — progress bar and processing state
  - Create `frontend/src/components/ProgressBar.tsx` accepting a `progress: number` (0–100) prop; render an animated bar using `framer-motion` width transition
  - In `frontend/src/App.tsx` manage `UploadState`; on file accepted call `processCatalog`, set `isProcessing: true`, simulate incremental progress updates during the request, set `progress: 100` on completion
  - Display `ProgressBar` while `isProcessing` is true
  - _Requirements: 1.3_

- [x] 14. React frontend — results table and CSV export
  - Create `frontend/src/components/ResultsTable.tsx` accepting `products: EnrichedProduct[]`; render a scrollable table with all seven columns; apply dark theme Tailwind styles with hover row highlight
  - Create `frontend/src/components/ExportButton.tsx` using `papaparse` to serialize `EnrichedProduct[]` to CSV and trigger a browser download via a Blob URL
  - In `App.tsx` render `ResultsTable` and `ExportButton` when results are available; display total product count and `processing_time_ms` formatted as seconds
  - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 15. React frontend — error display
  - Create `frontend/src/components/ErrorBanner.tsx` accepting an `error: string | null` prop; render a visible, non-blocking notification banner when error is non-null; apply `framer-motion` slide-in animation
  - In `App.tsx` set error state from API error responses and pass to `ErrorBanner`
  - _Requirements: 12.4_

- [x] 16. Final checkpoint — full stack integration
  - Ensure all Python tests pass: `cd ai-engine && pytest`
  - Ensure all Java tests pass: `cd backend && mvn test`
  - Ensure React app builds without errors: `cd frontend && npm run build`
  - Ask the user if any questions arise before proceeding.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use `hypothesis` (Python) and `jqwik` (Java) as specified in the design
- Checkpoints at tasks 6, 10, and 16 ensure incremental validation across tiers
