"""
Tests for generator.py — retry logic, fallback, and property-based tests.

Sub-task 5.1: Unit tests for retry and fallback logic
Sub-task 5.2: Property 6 — Retry attempt bound (Validates: Requirements 6.4)
Sub-task 5.3: Property 8 — Fallback completeness (Validates: Requirements 6.5)
"""

import asyncio
import json
import pytest
from unittest.mock import AsyncMock, patch

from hypothesis import given, settings
from hypothesis import strategies as st

from models import ProductRequest, EnrichedProduct
from exceptions import RateLimitError, ApiError
from generator import generate_product_content, build_fallback, merge_with_fallback, MAX_ATTEMPTS


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def make_product(name="Test Product", category="Electronics", price="99.99") -> ProductRequest:
    return ProductRequest(product_name=name, category=category, price=price)


def valid_json_response() -> str:
    """Return a JSON string that passes validate_fields and clean_output."""
    return json.dumps({
        "description": "A great product that everyone will love and enjoy using every day.",
        "tags": "electronics, gadget, tech",
        "seo_title": "Test Product - Best Electronics",
        "seo_description": "Buy Test Product online. Great quality and fast shipping.",
    })


def invalid_json_response() -> str:
    """Return a string that fails JSON extraction."""
    return "This is not JSON at all."


def missing_field_json_response() -> str:
    """Return JSON missing required fields — fails validate_fields."""
    return json.dumps({"description": "Only one field"})


# ---------------------------------------------------------------------------
# Sub-task 5.1: Unit tests for retry and fallback logic
# ---------------------------------------------------------------------------

class TestRetryOnTimeout:
    """With MAX_ATTEMPTS=1, a single timeout immediately returns fallback."""

    def test_timeout_returns_fallback(self):
        """Single timeout → fallback returned, exactly 1 call made."""
        call_count = 0

        async def mock_hf(prompt: str) -> str:
            nonlocal call_count
            call_count += 1
            raise TimeoutError("Simulated timeout")

        product = make_product()
        result = asyncio.run(
            generate_product_content(product, hf_caller=mock_hf, timeout_sleep=0)
        )

        assert call_count == MAX_ATTEMPTS
        assert isinstance(result, EnrichedProduct)
        # Fallback uses product data — description contains product name or category
        assert result.description != ""
        assert result.seo_title != ""


class TestAlwaysFailFallback:
    """Mock to always raise errors; assert fallback returned and call count ≤ MAX_ATTEMPTS."""

    def test_always_timeout_returns_fallback(self):
        call_count = 0

        async def mock_hf(prompt: str) -> str:
            nonlocal call_count
            call_count += 1
            raise TimeoutError("Always timeout")

        product = make_product()
        result = asyncio.run(
            generate_product_content(product, hf_caller=mock_hf, timeout_sleep=0)
        )

        assert call_count <= MAX_ATTEMPTS
        assert isinstance(result, EnrichedProduct)
        assert result.description != ""
        assert result.seo_title != ""

    def test_always_rate_limit_returns_fallback(self):
        call_count = 0

        async def mock_hf(prompt: str) -> str:
            nonlocal call_count
            call_count += 1
            raise RateLimitError("Always rate limited")

        product = make_product()
        result = asyncio.run(
            generate_product_content(product, hf_caller=mock_hf, rate_limit_sleep=0)
        )

        assert call_count <= MAX_ATTEMPTS
        assert isinstance(result, EnrichedProduct)
        assert result.description != ""

    def test_always_api_error_returns_fallback(self):
        call_count = 0

        async def mock_hf(prompt: str) -> str:
            nonlocal call_count
            call_count += 1
            raise ApiError(500, "Internal server error")

        product = make_product()
        result = asyncio.run(generate_product_content(product, hf_caller=mock_hf))

        assert call_count <= MAX_ATTEMPTS
        assert isinstance(result, EnrichedProduct)
        assert result.description != ""


class TestInvalidJsonRetry:
    """With MAX_ATTEMPTS=1, invalid/partial JSON triggers partial recovery."""

    def test_invalid_json_returns_fallback_content(self):
        """Completely unparseable response → full fallback values used."""
        call_count = 0

        async def mock_hf(prompt: str) -> str:
            nonlocal call_count
            call_count += 1
            return invalid_json_response()

        product = make_product()
        result = asyncio.run(generate_product_content(product, hf_caller=mock_hf))

        assert call_count == MAX_ATTEMPTS
        assert isinstance(result, EnrichedProduct)
        assert result.description != ""

    def test_partial_json_fills_missing_fields(self):
        """Partial JSON (only description present) → missing fields filled from fallback."""
        async def mock_hf(prompt: str) -> str:
            return json.dumps({"description": "A great phone with amazing features."})

        product = make_product()
        result = asyncio.run(generate_product_content(product, hf_caller=mock_hf))

        assert isinstance(result, EnrichedProduct)
        # AI description is kept
        assert "great phone" in result.description
        # Missing fields are filled from fallback (contain product data)
        assert result.tags != ""
        assert result.seo_title != ""
        assert result.seo_description != ""

    def test_always_invalid_json_returns_fallback(self):
        call_count = 0

        async def mock_hf(prompt: str) -> str:
            nonlocal call_count
            call_count += 1
            return invalid_json_response()

        product = make_product()
        result = asyncio.run(generate_product_content(product, hf_caller=mock_hf))

        assert call_count == MAX_ATTEMPTS
        assert result.description != ""

    def test_valid_json_returns_enriched_product(self):
        """Valid JSON on the single attempt returns a proper EnrichedProduct."""
        async def mock_hf(prompt: str) -> str:
            return valid_json_response()

        product = make_product()
        result = asyncio.run(generate_product_content(product, hf_caller=mock_hf))

        assert isinstance(result, EnrichedProduct)
        assert "great product" in result.description


# ---------------------------------------------------------------------------
# Tests for merge_with_fallback (partial recovery)
# ---------------------------------------------------------------------------

class TestMergeWithFallback:
    """Test the partial recovery logic in merge_with_fallback."""

    def test_none_json_returns_full_fallback(self):
        product = make_product()
        result = merge_with_fallback(None, product)
        assert result["description"] != ""
        assert result["tags"] != ""
        assert result["seo_title"] != ""
        assert result["seo_description"] != ""

    def test_all_fields_present_keeps_ai_values(self):
        product = make_product()
        ai_data = {
            "description": "AI description here.",
            "tags": "ai, tag, list",
            "seo_title": "AI SEO Title",
            "seo_description": "AI SEO description text.",
        }
        result = merge_with_fallback(ai_data, product)
        assert result["description"] == "AI description here."
        assert result["tags"] == "ai, tag, list"
        assert result["seo_title"] == "AI SEO Title"
        assert result["seo_description"] == "AI SEO description text."

    def test_missing_field_filled_from_fallback(self):
        product = make_product()
        ai_data = {"description": "Good description from AI."}
        result = merge_with_fallback(ai_data, product)
        # AI description kept
        assert result["description"] == "Good description from AI."
        # Missing fields come from fallback (contain product name)
        assert "Test Product" in result["tags"] or result["tags"] != ""
        assert result["seo_title"] != ""
        assert result["seo_description"] != ""

    def test_empty_string_field_replaced_by_fallback(self):
        product = make_product()
        ai_data = {
            "description": "Good description.",
            "tags": "",          # empty → replaced
            "seo_title": "Title",
            "seo_description": "Desc.",
        }
        result = merge_with_fallback(ai_data, product)
        assert result["tags"] != ""  # fallback value used

    def test_list_tags_kept_as_usable(self):
        product = make_product()
        ai_data = {
            "description": "Good description.",
            "tags": ["phone", "tech", "mobile"],
            "seo_title": "Title",
            "seo_description": "Desc.",
        }
        result = merge_with_fallback(ai_data, product)
        # List is usable — should be kept
        assert result["tags"] == ["phone", "tech", "mobile"]


class TestFallbackValues:
    """Test that build_fallback returns meaningful values derived from product data."""

    def test_fallback_description_contains_product_name(self):
        product = make_product()
        fallback = build_fallback(product)
        assert "Test Product" in fallback.description

    def test_fallback_description_is_non_empty(self):
        product = make_product()
        fallback = build_fallback(product)
        assert fallback.description != ""

    def test_fallback_tags_contains_product_name(self):
        product = make_product()
        fallback = build_fallback(product)
        assert "Test Product" in fallback.tags

    def test_fallback_seo_title_contains_product_name(self):
        product = make_product(name="My Awesome Product")
        fallback = build_fallback(product)
        assert "My Awesome Product" in fallback.seo_title

    def test_fallback_seo_title_within_60_chars(self):
        long_name = "A" * 100
        product = make_product(name=long_name)
        fallback = build_fallback(product)
        assert len(fallback.seo_title) <= 60

    def test_fallback_seo_description_is_non_empty(self):
        product = make_product()
        fallback = build_fallback(product)
        assert fallback.seo_description != ""

    def test_fallback_all_fields_non_empty(self):
        product = make_product()
        fallback = build_fallback(product)
        assert fallback.description != ""
        assert fallback.tags != ""
        assert fallback.seo_title != ""
        assert fallback.seo_description != ""


# ---------------------------------------------------------------------------
# Sub-task 5.2: Property 6 — Retry attempt bound
# Validates: Requirements 6.4
# ---------------------------------------------------------------------------

# Strategies for generating failure sequences
_failure_types = st.sampled_from(["timeout", "rate_limit", "api_error", "invalid_json", "missing_fields"])


@given(failure_sequence=st.lists(_failure_types, min_size=0, max_size=10))
@settings(max_examples=100, deadline=None)
def test_property_6_retry_attempt_bound(failure_sequence):
    """
    Property 6: Retry attempt bound

    For any arbitrary failure sequence, the total number of calls made to the
    HF API SHALL be at most 3 (MAX_ATTEMPTS), regardless of the failure mode.

    deadline=None because timeout retries include asyncio.sleep(1s/2s) by design.
    Sleep durations are set to 0 to keep tests fast while still exercising the logic.

    Validates: Requirements 6.4
    """
    call_count = 0

    async def mock_hf(prompt: str) -> str:
        nonlocal call_count
        call_count += 1

        # Determine what to do on this call
        idx = call_count - 1
        if idx < len(failure_sequence):
            failure = failure_sequence[idx]
            if failure == "timeout":
                raise TimeoutError("Simulated timeout")
            elif failure == "rate_limit":
                raise RateLimitError("Simulated rate limit")
            elif failure == "api_error":
                raise ApiError(500, "Simulated API error")
            elif failure == "invalid_json":
                return "not valid json"
            elif failure == "missing_fields":
                return json.dumps({"description": "only one field"})

        # If we've exhausted the failure sequence, return valid JSON
        return valid_json_response()

    product = make_product()
    # Use zero sleep durations so the property test runs fast
    asyncio.run(
        generate_product_content(
            product,
            hf_caller=mock_hf,
            timeout_sleep=0,
            rate_limit_sleep=0,
        )
    )

    assert call_count <= MAX_ATTEMPTS, (
        f"Expected at most {MAX_ATTEMPTS} calls, but got {call_count} "
        f"for failure sequence: {failure_sequence}"
    )


# ---------------------------------------------------------------------------
# Sub-task 5.3: Property 8 — Fallback completeness
# Validates: Requirements 6.5
# ---------------------------------------------------------------------------

_non_empty_text = st.text(min_size=1, max_size=200).filter(lambda s: s.strip() != "")


@given(
    product_name=_non_empty_text,
    category=_non_empty_text,
    price=_non_empty_text,
)
@settings(max_examples=100)
def test_property_8_fallback_completeness(product_name, category, price):
    """
    Property 8: Fallback completeness

    For any arbitrary ProductRequest, build_fallback SHALL return an
    EnrichedProduct where all four fields are non-empty strings.

    Validates: Requirements 6.5
    """
    product = ProductRequest(
        product_name=product_name,
        category=category,
        price=price,
    )
    fallback = build_fallback(product)

    assert isinstance(fallback, EnrichedProduct)

    # All four fields must be non-empty strings
    assert isinstance(fallback.description, str) and fallback.description != "", (
        f"description is empty for product_name={product_name!r}"
    )
    assert isinstance(fallback.tags, str) and fallback.tags != "", (
        f"tags is empty for product_name={product_name!r}"
    )
    assert isinstance(fallback.seo_title, str) and fallback.seo_title != "", (
        f"seo_title is empty for product_name={product_name!r}"
    )
    assert isinstance(fallback.seo_description, str) and fallback.seo_description != "", (
        f"seo_description is empty for product_name={product_name!r}"
    )
