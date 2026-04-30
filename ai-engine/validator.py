"""
Output validation for AI-generated JSON.

validate_fields checks that all four required keys exist and are non-empty strings.
Requirements: 8.1, 8.2, 8.3, 8.4
"""

REQUIRED_KEYS = ["description", "tags", "seo_title", "seo_description"]


def validate_fields(json_data) -> bool:
    """
    Return True if json_data is a dict containing all four required keys
    with non-empty string values; False otherwise.

    Requirements: 8.1, 8.2, 8.3, 8.4
    """
    if json_data is None:
        return False

    if not isinstance(json_data, dict):
        return False

    for key in REQUIRED_KEYS:
        if key not in json_data:
            return False
        value = json_data[key]
        # tags may be a non-empty list; all other fields must be non-empty strings
        if key == "tags":
            if isinstance(value, list):
                if len(value) == 0:
                    return False
            elif not isinstance(value, str) or value == "":
                return False
        else:
            if not isinstance(value, str) or value == "":
                return False

    return True
