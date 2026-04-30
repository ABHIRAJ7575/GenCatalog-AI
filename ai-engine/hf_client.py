"""
Hugging Face API client for LLM inference.

call_hugging_face sends a prompt to the HF Inference API and returns the raw response text.

Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 11.1
"""

import os
import httpx
from dotenv import load_dotenv
from exceptions import RateLimitError, ApiError

# Load environment variables from .env file
load_dotenv()

HF_API_URL = "https://router.huggingface.co/v1/chat/completions"
HF_MODEL = "meta-llama/Meta-Llama-3-8B-Instruct"
HF_API_KEY = os.getenv("HF_API_KEY")


async def call_hugging_face(prompt: str) -> str:
    """
    Call the Hugging Face Inference API with the given prompt.

    Preconditions:
    - HF_API_KEY environment variable is set
    - prompt is non-empty string

    Postconditions:
    - Returns raw response text from LLM on success
    - Raises TimeoutError if request exceeds 30s (Req 5.3)
    - Raises RateLimitError on HTTP 429 (Req 5.4)
    - Raises ApiError on other HTTP errors (Req 5.5)

    Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 11.1
    """
    if not HF_API_KEY:
        raise ValueError("HF_API_KEY environment variable is not set")

    headers = {
        "Authorization": f"Bearer {HF_API_KEY}",
        "Content-Type": "application/json",
    }

    payload = {
        "model": HF_MODEL,
        "messages": [{"role": "user", "content": prompt}],
        "max_tokens": 300,
        "temperature": 0.7,
    }

    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            response = await client.post(HF_API_URL, json=payload, headers=headers)

            # Check for rate limit (Req 5.4)
            if response.status_code == 429:
                raise RateLimitError("Rate limit exceeded")

            # Check for other HTTP errors (Req 5.5)
            if response.status_code != 200:
                raise ApiError(
                    response.status_code,
                    response.text or "Unknown error",
                )

            # Extract the response text (Req 5.2)
            response_json = response.json()
            content = response_json.get("choices", [{}])[0].get("message", {}).get("content", "")
            return content

        except httpx.TimeoutException as e:
            # Req 5.3: Raise TimeoutError for timeout
            raise TimeoutError("Request to Hugging Face API timed out") from e
