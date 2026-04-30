"""
Tests for prompt_builder.build_prompt

Sub-task 2.1: Unit tests
Sub-task 2.2: Property test — Property 12 (Validates: Requirements 4.1)
"""
import pytest
from hypothesis import given, settings
from hypothesis import strategies as st

from models import ProductRequest
from prompt_builder import build_prompt


# ---------------------------------------------------------------------------
# 2.1 Unit tests
# ---------------------------------------------------------------------------

def test_prompt_contains_product_name():
    product = ProductRequest(product_name="iPhone 15 Pro", category="Smartphones", price="999.99")
    prompt = build_prompt(product)
    assert "iPhone 15 Pro" in prompt


def test_prompt_contains_category():
    product = ProductRequest(product_name="Widget", category="Electronics", price="49.99")
    prompt = build_prompt(product)
    assert "Electronics" in prompt


def test_prompt_contains_price():
    product = ProductRequest(product_name="Widget", category="Electronics", price="49.99")
    prompt = build_prompt(product)
    assert "49.99" in prompt


def test_prompt_contains_all_three_fields():
    product = ProductRequest(product_name="Samsung TV", category="TVs", price="1299.00")
    prompt = build_prompt(product)
    assert "Samsung TV" in prompt
    assert "TVs" in prompt
    assert "1299.00" in prompt


def test_prompt_mentions_description_key():
    product = ProductRequest(product_name="A", category="B", price="1.00")
    prompt = build_prompt(product)
    assert "description" in prompt


def test_prompt_mentions_tags_key():
    product = ProductRequest(product_name="A", category="B", price="1.00")
    prompt = build_prompt(product)
    assert "tags" in prompt


def test_prompt_mentions_seo_title_key():
    product = ProductRequest(product_name="A", category="B", price="1.00")
    prompt = build_prompt(product)
    assert "seo_title" in prompt


def test_prompt_mentions_seo_description_key():
    product = ProductRequest(product_name="A", category="B", price="1.00")
    prompt = build_prompt(product)
    assert "seo_description" in prompt


def test_prompt_instructs_json_only():
    """Prompt should instruct the LLM to return ONLY a JSON object."""
    product = ProductRequest(product_name="A", category="B", price="1.00")
    prompt = build_prompt(product)
    assert "JSON" in prompt


def test_prompt_returns_string():
    product = ProductRequest(product_name="A", category="B", price="1.00")
    assert isinstance(build_prompt(product), str)


# ---------------------------------------------------------------------------
# 2.2 Property test — Property 12: Prompt contains all product fields
# Validates: Requirements 4.1
# ---------------------------------------------------------------------------

@given(
    product_name=st.text(min_size=1),
    category=st.text(min_size=1),
    price=st.text(min_size=1),
)
@settings(max_examples=200)
def test_property_prompt_contains_all_fields(product_name, category, price):
    """
    Property 12: Prompt contains all product fields
    Validates: Requirements 4.1

    For any Product with non-empty product_name, category, and price,
    the prompt constructed by the AI_Engine SHALL contain all three field
    values as substrings.
    """
    product = ProductRequest(product_name=product_name, category=category, price=price)
    prompt = build_prompt(product)
    assert product_name in prompt
    assert category in prompt
    assert price in prompt
