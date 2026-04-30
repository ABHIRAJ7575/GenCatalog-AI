"""
Tests for json_extractor.extract_json

Sub-task 3.1: Unit tests for all three extraction strategies and the all-fail case
Sub-task 3.2: Property test — Property 9 (Validates: Requirements 7.1, 7.2, 7.3)
"""
import json

import pytest
from hypothesis import given, settings
from hypothesis import strategies as st

from json_extractor import extract_json


# ---------------------------------------------------------------------------
# 3.1 Unit tests
# ---------------------------------------------------------------------------

# --- Strategy 1: raw text is valid JSON (Req 7.1) ---

def test_strategy1_simple_object():
    """Direct parse: plain JSON object string."""
    raw = '{"description": "A phone", "tags": "phone", "seo_title": "Phone", "seo_description": "Buy now"}'
    result = extract_json(raw)
    assert result == {
        "description": "A phone",
        "tags": "phone",
        "seo_title": "Phone",
        "seo_description": "Buy now",
    }


def test_strategy1_returns_dict():
    """Direct parse: result is a dict."""
    raw = '{"key": "value"}'
    result = extract_json(raw)
    assert isinstance(result, dict)
    assert result["key"] == "value"


def test_strategy1_nested_object():
    """Direct parse: nested JSON is handled correctly."""
    raw = '{"a": {"b": 1}}'
    result = extract_json(raw)
    assert result == {"a": {"b": 1}}


# --- Strategy 2: JSON embedded in surrounding prose (Req 7.2) ---

def test_strategy2_json_with_leading_text():
    """Brace extraction: JSON preceded by prose."""
    raw = 'Here is the output: {"description": "Nice", "tags": "t", "seo_title": "T", "seo_description": "D"}'
    result = extract_json(raw)
    assert result is not None
    assert result["description"] == "Nice"


def test_strategy2_json_with_trailing_text():
    """Brace extraction: JSON followed by prose."""
    raw = '{"description": "Nice", "tags": "t", "seo_title": "T", "seo_description": "D"} Hope that helps!'
    result = extract_json(raw)
    assert result is not None
    assert result["seo_title"] == "T"


def test_strategy2_json_surrounded_by_prose():
    """Brace extraction: JSON surrounded by prose on both sides."""
    raw = 'Sure! Here you go: {"description": "Desc", "tags": "a, b", "seo_title": "Title", "seo_description": "Meta"} Let me know if you need more.'
    result = extract_json(raw)
    assert result is not None
    assert result["tags"] == "a, b"


def test_strategy2_uses_last_brace():
    """Brace extraction: uses first '{' and last '}' so nested braces work."""
    raw = 'Output: {"key": {"nested": "value"}}'
    result = extract_json(raw)
    assert result == {"key": {"nested": "value"}}


# --- Strategy 3: JSON inside ```json block (Req 7.3) ---

def test_strategy3_markdown_code_block():
    """Markdown extraction: JSON inside ```json ... ``` block."""
    raw = (
        "Here is the result:\n"
        "```json\n"
        '{"description": "Desc", "tags": "x", "seo_title": "T", "seo_description": "S"}\n'
        "```\n"
        "Done."
    )
    result = extract_json(raw)
    assert result is not None
    assert result["description"] == "Desc"


def test_strategy3_markdown_block_with_whitespace():
    """Markdown extraction: extra whitespace inside the block is trimmed."""
    raw = "```json\n  {\"a\": 1}  \n```"
    result = extract_json(raw)
    assert result == {"a": 1}


def test_strategy3_markdown_block_no_surrounding_text():
    """Markdown extraction: block is the entire content."""
    raw = '```json\n{"seo_title": "Hello"}\n```'
    result = extract_json(raw)
    assert result == {"seo_title": "Hello"}


# --- All-fail case returns None (Req 7.4) ---

def test_all_fail_plain_text():
    """All strategies fail on plain non-JSON text → None."""
    result = extract_json("This is just plain text with no JSON at all.")
    assert result is None


def test_all_fail_empty_string():
    """All strategies fail on empty string → None."""
    result = extract_json("")
    assert result is None


def test_all_fail_malformed_json():
    """All strategies fail on malformed JSON → None."""
    result = extract_json("{not valid json}")
    assert result is None


def test_all_fail_incomplete_braces():
    """All strategies fail when braces don't contain valid JSON → None."""
    result = extract_json("{ incomplete")
    assert result is None


def test_all_fail_markdown_block_with_invalid_json():
    """Strategy 3 fails when markdown block contains invalid JSON → None."""
    raw = "```json\nnot json at all\n```"
    result = extract_json(raw)
    assert result is None


# ---------------------------------------------------------------------------
# 3.2 Property test — Property 9: JSON extraction round-trip
# Validates: Requirements 7.1, 7.2, 7.3
# ---------------------------------------------------------------------------

# Strategy for generating the four required keys with non-empty string values
_required_keys_strategy = st.fixed_dictionaries(
    {
        "description": st.text(min_size=1),
        "tags": st.text(min_size=1),
        "seo_title": st.text(min_size=1),
        "seo_description": st.text(min_size=1),
    }
)


@given(data=_required_keys_strategy)
@settings(max_examples=200)
def test_property_round_trip_strategy1(data):
    """
    Property 9: JSON extraction round-trip (Strategy 1 — direct parse)
    Validates: Requirements 7.1, 7.2, 7.3

    For any valid JSON object containing the four required keys, serializing
    it to a string and applying extract_json SHALL produce an equivalent object.
    """
    raw = json.dumps(data)
    result = extract_json(raw)
    assert result == data


@given(data=_required_keys_strategy)
@settings(max_examples=200)
def test_property_round_trip_strategy2(data):
    """
    Property 9: JSON extraction round-trip (Strategy 2 — brace-bounded)
    Validates: Requirements 7.1, 7.2, 7.3

    Wrapping the JSON in surrounding prose forces Strategy 2 to be used
    (Strategy 1 will fail on the full string). The result must equal the
    original dict.
    """
    raw = "Some leading prose: " + json.dumps(data) + " and trailing text."
    result = extract_json(raw)
    assert result == data


@given(data=_required_keys_strategy)
@settings(max_examples=200)
def test_property_round_trip_strategy3(data):
    """
    Property 9: JSON extraction round-trip (Strategy 3 — markdown code block)
    Validates: Requirements 7.1, 7.2, 7.3

    Wrapping the JSON in a markdown code block (with no bare braces outside)
    exercises Strategy 3. The result must equal the original dict.
    """
    raw = "```json\n" + json.dumps(data) + "\n```"
    result = extract_json(raw)
    assert result == data


# ---------------------------------------------------------------------------
# Strategy 4: JSON repair (trailing comma removal)
# ---------------------------------------------------------------------------

def test_strategy4_trailing_comma_in_object():
    """Repair pass: trailing comma before } is removed and JSON parsed."""
    raw = '{"description": "Nice", "tags": "t", "seo_title": "T", "seo_description": "D",}'
    result = extract_json(raw)
    assert result is not None
    assert result["description"] == "Nice"


def test_strategy4_trailing_comma_in_nested():
    """Repair pass: trailing comma in nested structure is removed."""
    raw = 'Here: {"key": "value", "list": ["a", "b",],}'
    result = extract_json(raw)
    assert result is not None
    assert result["key"] == "value"


def test_strategy4_does_not_affect_valid_json():
    """Repair pass does not corrupt already-valid JSON."""
    raw = '{"a": 1, "b": 2}'
    result = extract_json(raw)
    assert result == {"a": 1, "b": 2}
