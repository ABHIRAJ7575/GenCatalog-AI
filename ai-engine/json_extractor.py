"""
JSON extraction from raw LLM response text.

Implements four strategies in order:
  1. Direct JSON parse of the full raw text
  2. Brace-bounded substring (first '{' to last '}')
  3. Markdown code block delimited by ```json ... ```
  4. Repair pass: strip trailing commas, then retry strategy 2

Returns the parsed dict on the first successful strategy, or None if all fail.

Requirements: 7.1, 7.2, 7.3, 7.4
"""
import json
import re

# Matches trailing commas before } or ] — a common LLM mistake
_TRAILING_COMMA_RE = re.compile(r",\s*([}\]])")


def _repair_json(text: str) -> str:
    """
    Apply lightweight repairs to malformed JSON text:
    - Remove trailing commas before closing braces/brackets
    """
    return _TRAILING_COMMA_RE.sub(r"\1", text)


def extract_json(raw_text: str) -> dict | None:
    """
    Attempt to extract a JSON object from raw LLM response text.

    Strategy 1 (Req 7.1): Try parsing the entire raw_text as JSON directly.
    Strategy 2 (Req 7.2): Extract the substring between the first '{' and
                           the last '}' and try parsing that.
    Strategy 3 (Req 7.3): Look for a ```json ... ``` markdown code block,
                           extract its content, and try parsing that.
    Strategy 4:            Apply JSON repair (trailing comma removal) to the
                           brace-bounded candidate and retry parsing.

    Returns the parsed dict on success, or None if all strategies fail (Req 7.4).
    """
    if not raw_text:
        return None

    # Strategy 1: Direct JSON parse
    try:
        result = json.loads(raw_text)
        if isinstance(result, dict):
            return result
    except (json.JSONDecodeError, TypeError):
        pass

    # Extract brace-bounded candidate for strategies 2 and 4
    start_idx = raw_text.find("{")
    end_idx = raw_text.rfind("}")
    candidate = None
    if start_idx >= 0 and end_idx > start_idx:
        candidate = raw_text[start_idx : end_idx + 1]

    # Strategy 2: Brace-bounded substring
    if candidate:
        try:
            result = json.loads(candidate)
            if isinstance(result, dict):
                return result
        except (json.JSONDecodeError, TypeError):
            pass

    # Strategy 3: Markdown code block
    if "```json" in raw_text:
        block_start = raw_text.find("```json") + len("```json")
        block_end = raw_text.find("```", block_start)
        if block_end > block_start:
            block_content = raw_text[block_start:block_end].strip()
            try:
                result = json.loads(block_content)
                if isinstance(result, dict):
                    return result
            except (json.JSONDecodeError, TypeError):
                # Also try repair on the markdown block content
                try:
                    result = json.loads(_repair_json(block_content))
                    if isinstance(result, dict):
                        return result
                except (json.JSONDecodeError, TypeError):
                    pass

    # Strategy 4: Repair pass on brace-bounded candidate
    if candidate:
        try:
            result = json.loads(_repair_json(candidate))
            if isinstance(result, dict):
                return result
        except (json.JSONDecodeError, TypeError):
            pass

    # All strategies failed (Req 7.4)
    return None
