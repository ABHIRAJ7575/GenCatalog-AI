"""
Output cleaning and constraint enforcement for AI-generated JSON.

clean_output applies the following transformations:
  - description: strip special symbols + normalize whitespace + 150-word truncation
  - tags: split by comma / trim each / remove empty entries / cap at 8
  - seo_title: strip special symbols + trim + 60-char truncation with "..."
  - seo_description: strip special symbols + trim + 160-char truncation with "..."

Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6
"""

import re


# Characters allowed after stripping: letters, digits, spaces, commas,
# periods, hyphens, apostrophes, exclamation marks, question marks, colons,
# semicolons, and parentheses.
_ALLOWED_CHARS_RE = re.compile(r"[^a-zA-Z0-9 ,.\-'!?:;()\n\r\t]")

# Collapse any run of whitespace (spaces, tabs, newlines) into a single space.
_WHITESPACE_RE = re.compile(r"\s+")


def _strip_symbols(text: str) -> str:
    """Remove characters that are not letters, digits, or basic punctuation."""
    return _ALLOWED_CHARS_RE.sub("", text)


def _normalize_whitespace(text: str) -> str:
    """Collapse runs of whitespace into a single space."""
    return _WHITESPACE_RE.sub(" ", text).strip()


def clean_output(json_data: dict) -> dict:
    """
    Clean and enforce constraints on AI-generated output fields.

    Returns a new dict with cleaned values for all four required keys.
    Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6
    """
    cleaned: dict = {}

    # --- description (Req 9.1) ---
    desc = _strip_symbols(json_data.get("description", ""))
    desc = _normalize_whitespace(desc)
    words = desc.split()
    if len(words) > 150:
        desc = " ".join(words[:150])
    cleaned["description"] = desc

    # --- tags (Req 9.2) ---
    raw_tags = json_data.get("tags", "")
    # Handle both list (["tag1", "tag2"]) and comma-string ("tag1, tag2") formats
    if isinstance(raw_tags, list):
        tag_list = [str(t).strip() for t in raw_tags]
    else:
        tag_list = [t.strip() for t in str(raw_tags).split(",")]
    tag_list = [t for t in tag_list if t]
    if len(tag_list) > 8:
        tag_list = tag_list[:8]
    cleaned["tags"] = ", ".join(tag_list)

    # --- seo_title (Req 9.3, 9.5) ---
    title = _strip_symbols(json_data.get("seo_title", ""))
    title = title.strip()
    if len(title) > 60:
        title = title[:57] + "..."
    cleaned["seo_title"] = title

    # --- seo_description (Req 9.4, 9.6) ---
    seodesc = _strip_symbols(json_data.get("seo_description", ""))
    seodesc = seodesc.strip()
    if len(seodesc) > 160:
        seodesc = seodesc[:157] + "..."
    cleaned["seo_description"] = seodesc

    return cleaned
