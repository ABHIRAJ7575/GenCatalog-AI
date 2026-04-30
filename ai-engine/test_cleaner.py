"""
Tests for cleaner.clean_output

Sub-task 4.2: Unit tests
Sub-task 4.3: Property tests — Properties 2, 3, 4, 5
             Validates: Requirements 9.1, 9.2, 9.3, 9.4
"""

import pytest
from hypothesis import given, settings
from hypothesis import strategies as st

from cleaner import clean_output


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_input(description="A product.", tags="phone", seo_title="Title", seo_description="Desc"):
    return {
        "description": description,
        "tags": tags,
        "seo_title": seo_title,
        "seo_description": seo_description,
    }


# ---------------------------------------------------------------------------
# 4.2 Unit tests
# ---------------------------------------------------------------------------

# --- description truncation at 150 words (Req 9.1) ---

def test_description_under_150_words_unchanged():
    """Description with fewer than 150 words is not truncated."""
    desc = " ".join(["word"] * 100)
    result = clean_output(_make_input(description=desc))
    assert len(result["description"].split()) == 100


def test_description_exactly_150_words_unchanged():
    """Description with exactly 150 words is not truncated."""
    desc = " ".join(["word"] * 150)
    result = clean_output(_make_input(description=desc))
    assert len(result["description"].split()) == 150


def test_description_truncated_at_150_words():
    """Description with 200 words is truncated to 150 words."""
    desc = " ".join([f"word{i}" for i in range(200)])
    result = clean_output(_make_input(description=desc))
    words = result["description"].split()
    assert len(words) == 150
    # First 150 words are preserved
    assert words[0] == "word0"
    assert words[149] == "word149"


def test_description_strips_special_symbols():
    """Special symbols are removed from description."""
    desc = "Hello @world! #test $price"
    result = clean_output(_make_input(description=desc))
    assert "@" not in result["description"]
    assert "#" not in result["description"]
    assert "$" not in result["description"]


def test_description_normalizes_whitespace():
    """Multiple spaces and newlines are collapsed to single spaces."""
    desc = "Hello   world\n\nfoo\tbar"
    result = clean_output(_make_input(description=desc))
    assert "  " not in result["description"]
    assert "\n" not in result["description"]
    assert "\t" not in result["description"]


# --- tags capped at 8, empty entries removed (Req 9.2) ---

def test_tags_under_8_unchanged():
    """Fewer than 8 tags are all retained."""
    tags = "phone, tech, mobile, gadget"
    result = clean_output(_make_input(tags=tags))
    tag_list = [t for t in result["tags"].split(",") if t.strip()]
    assert len(tag_list) == 4


def test_tags_exactly_8_unchanged():
    """Exactly 8 tags are all retained."""
    tags = "a, b, c, d, e, f, g, h"
    result = clean_output(_make_input(tags=tags))
    tag_list = [t for t in result["tags"].split(",") if t.strip()]
    assert len(tag_list) == 8


def test_tags_capped_at_8():
    """More than 8 tags are capped to the first 8."""
    tags = "a, b, c, d, e, f, g, h, i, j"
    result = clean_output(_make_input(tags=tags))
    tag_list = [t for t in result["tags"].split(",") if t.strip()]
    assert len(tag_list) == 8


def test_tags_empty_entries_removed():
    """Empty entries (from double commas or trailing commas) are removed."""
    tags = "phone,,tech,,,"
    result = clean_output(_make_input(tags=tags))
    tag_list = [t for t in result["tags"].split(",") if t.strip()]
    assert "" not in tag_list
    assert len(tag_list) == 2


def test_tags_whitespace_trimmed():
    """Each tag is trimmed of surrounding whitespace before joining."""
    tags = "  phone  ,  tech  ,  mobile  "
    result = clean_output(_make_input(tags=tags))
    # The cleaner trims each tag; after joining with ", " the individual
    # tag values (stripped from the joined string) should equal their trimmed form.
    tag_list = [t.strip() for t in result["tags"].split(",") if t.strip()]
    assert tag_list == ["phone", "tech", "mobile"]


def test_tags_empty_string_produces_empty_list():
    """An empty tags string produces an empty tags output."""
    result = clean_output(_make_input(tags=""))
    tag_list = [t for t in result["tags"].split(",") if t.strip()]
    assert len(tag_list) == 0


# --- seo_title truncated to 60 chars with "..." (Req 9.3, 9.5) ---

def test_seo_title_under_60_chars_unchanged():
    """SEO title under 60 characters is not truncated."""
    title = "Short Title"
    result = clean_output(_make_input(seo_title=title))
    assert result["seo_title"] == "Short Title"


def test_seo_title_exactly_60_chars_unchanged():
    """SEO title of exactly 60 characters is not truncated."""
    title = "A" * 60
    result = clean_output(_make_input(seo_title=title))
    assert len(result["seo_title"]) == 60
    assert not result["seo_title"].endswith("...")


def test_seo_title_truncated_to_60_chars_with_ellipsis():
    """SEO title over 60 characters is truncated to 60 chars total with '...'."""
    title = "A" * 100
    result = clean_output(_make_input(seo_title=title))
    assert len(result["seo_title"]) == 60
    assert result["seo_title"].endswith("...")


def test_seo_title_truncation_preserves_first_57_chars():
    """The first 57 characters are preserved before the '...'."""
    title = "B" * 100
    result = clean_output(_make_input(seo_title=title))
    assert result["seo_title"] == "B" * 57 + "..."


def test_seo_title_strips_special_symbols():
    """Special symbols are removed from seo_title."""
    title = "Buy @iPhone #15 $Pro!"
    result = clean_output(_make_input(seo_title=title))
    assert "@" not in result["seo_title"]
    assert "#" not in result["seo_title"]
    assert "$" not in result["seo_title"]


# --- seo_description truncated to 160 chars with "..." (Req 9.4, 9.6) ---

def test_seo_description_under_160_chars_unchanged():
    """SEO description under 160 characters is not truncated."""
    desc = "Short description."
    result = clean_output(_make_input(seo_description=desc))
    assert result["seo_description"] == "Short description."


def test_seo_description_exactly_160_chars_unchanged():
    """SEO description of exactly 160 characters is not truncated."""
    desc = "A" * 160
    result = clean_output(_make_input(seo_description=desc))
    assert len(result["seo_description"]) == 160
    assert not result["seo_description"].endswith("...")


def test_seo_description_truncated_to_160_chars_with_ellipsis():
    """SEO description over 160 characters is truncated to 160 chars total with '...'."""
    desc = "A" * 200
    result = clean_output(_make_input(seo_description=desc))
    assert len(result["seo_description"]) == 160
    assert result["seo_description"].endswith("...")


def test_seo_description_truncation_preserves_first_157_chars():
    """The first 157 characters are preserved before the '...'."""
    desc = "C" * 200
    result = clean_output(_make_input(seo_description=desc))
    assert result["seo_description"] == "C" * 157 + "..."


def test_seo_description_strips_special_symbols():
    """Special symbols are removed from seo_description."""
    desc = "Buy @iPhone #15 $Pro now!"
    result = clean_output(_make_input(seo_description=desc))
    assert "@" not in result["seo_description"]
    assert "#" not in result["seo_description"]
    assert "$" not in result["seo_description"]


# ---------------------------------------------------------------------------
# 4.3 Property tests — Properties 2, 3, 4, 5
# Validates: Requirements 9.1, 9.2, 9.3, 9.4
# ---------------------------------------------------------------------------

# Strategy: generate arbitrary strings (including empty) for each field.
_text_strategy = st.text()


@given(
    description=_text_strategy,
    tags=_text_strategy,
    seo_title=_text_strategy,
    seo_description=_text_strategy,
)
@settings(max_examples=300)
def test_property_2_description_word_count_constraint(description, tags, seo_title, seo_description):
    """
    Property 2: Description word count constraint
    Validates: Requirements 9.1

    For any input, the cleaned description SHALL have at most 150 words.
    """
    data = {
        "description": description,
        "tags": tags,
        "seo_title": seo_title,
        "seo_description": seo_description,
    }
    result = clean_output(data)
    assert len(result["description"].split()) <= 150


@given(
    description=_text_strategy,
    tags=_text_strategy,
    seo_title=_text_strategy,
    seo_description=_text_strategy,
)
@settings(max_examples=300)
def test_property_3_tag_count_constraint(description, tags, seo_title, seo_description):
    """
    Property 3: Tag count constraint
    Validates: Requirements 9.2

    For any input, the number of non-empty comma-separated tags SHALL be at most 8.
    """
    data = {
        "description": description,
        "tags": tags,
        "seo_title": seo_title,
        "seo_description": seo_description,
    }
    result = clean_output(data)
    non_empty_tags = [t for t in result["tags"].split(",") if t.strip()]
    assert len(non_empty_tags) <= 8


@given(
    description=_text_strategy,
    tags=_text_strategy,
    seo_title=_text_strategy,
    seo_description=_text_strategy,
)
@settings(max_examples=300)
def test_property_4_seo_title_length_constraint(description, tags, seo_title, seo_description):
    """
    Property 4: SEO title length constraint
    Validates: Requirements 9.3

    For any input, the cleaned seo_title SHALL have at most 60 characters.
    """
    data = {
        "description": description,
        "tags": tags,
        "seo_title": seo_title,
        "seo_description": seo_description,
    }
    result = clean_output(data)
    assert len(result["seo_title"]) <= 60


@given(
    description=_text_strategy,
    tags=_text_strategy,
    seo_title=_text_strategy,
    seo_description=_text_strategy,
)
@settings(max_examples=300)
def test_property_5_seo_description_length_constraint(description, tags, seo_title, seo_description):
    """
    Property 5: SEO description length constraint
    Validates: Requirements 9.4

    For any input, the cleaned seo_description SHALL have at most 160 characters.
    """
    data = {
        "description": description,
        "tags": tags,
        "seo_title": seo_title,
        "seo_description": seo_description,
    }
    result = clean_output(data)
    assert len(result["seo_description"]) <= 160
