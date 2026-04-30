"""
AI content generation with single-attempt + partial recovery.

Pipeline:
  build_prompt → call_hugging_face → extract_json → merge_with_fallback → clean_output

Partial recovery: if the LLM returns some valid fields, keep them and fill
missing ones from the product-derived fallback. The response is ALWAYS returned
immediately — no blocking retries.

Requirements: 5.1–5.5, 6.1–6.5, 11.1, 11.2, 12.5
"""

import logging
from typing import Callable, Awaitable

from models import ProductRequest, EnrichedProduct
from prompt_builder import build_prompt
from hf_client import call_hugging_face
from json_extractor import extract_json
from cleaner import clean_output
from exceptions import RateLimitError, ApiError

logger = logging.getLogger(__name__)

MAX_ATTEMPTS = 1

REQUIRED_KEYS = ["description", "tags", "seo_title", "seo_description"]


def build_fallback(product: ProductRequest) -> EnrichedProduct:
    """
    Build a fallback EnrichedProduct with meaningful values derived from
    the product data, so the frontend always has usable output.

    Requirements: 6.5
    """
    name = product.product_name
    category = product.category
    price = product.price

    description = (
        f"{name} is a quality {category.lower()} product priced at ${price}. "
        f"This item offers great value and is available for purchase now."
    )
    tags = f"{name}, {category}, product, buy, online"
    seo_title = f"{name} - {category}"[:59]
    seo_description = (
        f"Buy {name} in the {category} category for ${price}. "
        f"Quality product with fast shipping."
    )[:160]

    return EnrichedProduct(
        description=description,
        tags=tags,
        seo_title=seo_title,
        seo_description=seo_description,
    )


def _fallback_dict(product: ProductRequest) -> dict:
    """Return the fallback as a plain dict for field-level merging."""
    fb = build_fallback(product)
    return {
        "description": fb.description,
        "tags": fb.tags,
        "seo_title": fb.seo_title,
        "seo_description": fb.seo_description,
    }


def _is_usable(value) -> bool:
    """Return True if a field value is a non-empty string or non-empty list."""
    if isinstance(value, list):
        return len(value) > 0
    return isinstance(value, str) and value.strip() != ""


def merge_with_fallback(json_data: dict | None, product: ProductRequest) -> dict:
    """
    Merge extracted JSON with fallback values.

    For each required key:
    - If the key exists in json_data and has a usable value → keep it
    - Otherwise → use the fallback value

    This means a partial LLM response is never fully discarded.
    """
    fb = _fallback_dict(product)

    if json_data is None:
        logger.warning("No JSON extracted from AI response — using full fallback")
        return fb

    merged = {}
    for key in REQUIRED_KEYS:
        ai_value = json_data.get(key)
        if _is_usable(ai_value):
            merged[key] = ai_value
        else:
            logger.debug("Field '%s' missing or empty in AI response — using fallback value", key)
            merged[key] = fb[key]

    return merged


async def generate_product_content(
    product: ProductRequest,
    hf_caller: Callable[[str], Awaitable[str]] = call_hugging_face,
    timeout_sleep: float = 1.0,
    rate_limit_sleep: float = 2.0,
) -> EnrichedProduct:
    """
    Generate enriched product content.

    Makes exactly MAX_ATTEMPTS (1) call to the HF API.
    Partial recovery fills any missing fields from the product-derived fallback.
    Always returns an EnrichedProduct — never raises.

    Requirements: 5.1–5.5, 6.1–6.5, 11.1, 11.2, 12.5
    """
    prompt = build_prompt(product)

    try:
        raw_response = await hf_caller(prompt)
        logger.debug("Raw AI response for '%s': %s", product.product_name, raw_response[:500])

        json_data = extract_json(raw_response)
        merged = merge_with_fallback(json_data, product)
        cleaned = clean_output(merged)
        return EnrichedProduct(**cleaned)

    except TimeoutError:
        logger.warning("Timeout calling AI for '%s' — using fallback", product.product_name)

    except RateLimitError:
        logger.warning("Rate limited calling AI for '%s' — using fallback", product.product_name)

    except ApiError as e:
        logger.error(
            "API error (HTTP %d) for '%s' — using fallback",
            e.status_code,
            product.product_name,
        )

    except Exception as e:
        logger.error(
            "Unexpected error for '%s' (%s) — using fallback",
            product.product_name,
            type(e).__name__,
        )

    # Any exception path lands here — always return usable output
    logger.warning("Returning fallback for '%s'", product.product_name)
    return build_fallback(product)
