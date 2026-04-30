"""
Custom exceptions for the AI engine.

Requirements: 5.4, 5.5
"""


class RateLimitError(Exception):
    """Raised when the Hugging Face API returns HTTP 429 (Too Many Requests)."""

    def __init__(self, message: str = "Rate limit exceeded"):
        super().__init__(message)


class ApiError(Exception):
    """Raised when the Hugging Face API returns any non-429 HTTP error."""

    def __init__(self, status_code: int, message: str = ""):
        self.status_code = status_code
        super().__init__(f"API error {status_code}: {message}")
