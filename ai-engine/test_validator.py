"""
Tests for validator.validate_fields

Sub-task 4.1: Unit tests
Requirements: 8.1, 8.2, 8.3, 8.4
"""

import pytest

from validator import validate_fields


# ---------------------------------------------------------------------------
# 4.1 Unit tests
# ---------------------------------------------------------------------------

VALID_DATA = {
    "description": "A great product.",
    "tags": "phone, tech",
    "seo_title": "Buy Now",
    "seo_description": "The best product on the market.",
}


# --- All keys present and non-empty → True (Req 8.1) ---

def test_all_keys_present_and_non_empty_returns_true():
    """All four required keys present with non-empty strings → True."""
    assert validate_fields(VALID_DATA) is True


def test_all_keys_minimal_non_empty_strings():
    """Single-character values are still non-empty → True."""
    data = {
        "description": "x",
        "tags": "y",
        "seo_title": "z",
        "seo_description": "w",
    }
    assert validate_fields(data) is True


def test_extra_keys_still_valid():
    """Extra keys beyond the required four do not invalidate the result."""
    data = dict(VALID_DATA)
    data["extra_key"] = "extra_value"
    assert validate_fields(data) is True


# --- Missing key → False (Req 8.2) ---

def test_missing_description_returns_false():
    data = {k: v for k, v in VALID_DATA.items() if k != "description"}
    assert validate_fields(data) is False


def test_missing_tags_returns_false():
    data = {k: v for k, v in VALID_DATA.items() if k != "tags"}
    assert validate_fields(data) is False


def test_missing_seo_title_returns_false():
    data = {k: v for k, v in VALID_DATA.items() if k != "seo_title"}
    assert validate_fields(data) is False


def test_missing_seo_description_returns_false():
    data = {k: v for k, v in VALID_DATA.items() if k != "seo_description"}
    assert validate_fields(data) is False


def test_empty_dict_returns_false():
    """An empty dict is missing all required keys → False."""
    assert validate_fields({}) is False


# --- Key present with empty string → False (Req 8.3) ---

def test_empty_description_returns_false():
    data = dict(VALID_DATA)
    data["description"] = ""
    assert validate_fields(data) is False


def test_empty_tags_returns_false():
    data = dict(VALID_DATA)
    data["tags"] = ""
    assert validate_fields(data) is False


def test_empty_seo_title_returns_false():
    data = dict(VALID_DATA)
    data["seo_title"] = ""
    assert validate_fields(data) is False


def test_empty_seo_description_returns_false():
    data = dict(VALID_DATA)
    data["seo_description"] = ""
    assert validate_fields(data) is False


def test_non_string_value_returns_false():
    """A non-string value (e.g. None) for a required key → False."""
    data = dict(VALID_DATA)
    data["description"] = None
    assert validate_fields(data) is False


def test_integer_value_returns_false():
    """An integer value for a required key → False."""
    data = dict(VALID_DATA)
    data["tags"] = 42
    assert validate_fields(data) is False


# --- Input is None → False (Req 8.4) ---

def test_none_input_returns_false():
    """None input → False."""
    assert validate_fields(None) is False


# --- Input is not a dict → False (Req 8.4) ---

def test_list_input_returns_false():
    """A list input → False."""
    assert validate_fields(["description", "tags", "seo_title", "seo_description"]) is False


def test_string_input_returns_false():
    """A string input → False."""
    assert validate_fields('{"description": "x"}') is False


def test_integer_input_returns_false():
    """An integer input → False."""
    assert validate_fields(123) is False


def test_tuple_input_returns_false():
    """A tuple input → False."""
    assert validate_fields(("description", "tags", "seo_title", "seo_description")) is False
