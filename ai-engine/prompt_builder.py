from models import ProductRequest


def build_prompt(product: ProductRequest) -> str:
    """
    Build a structured prompt for the LLM from a ProductRequest.

    Preconditions:
    - product.product_name is non-empty string
    - product.category is non-empty string
    - product.price is non-empty string

    Postconditions:
    - Returns a string containing all three product fields as substrings
    - Instructs the model to return ONLY a JSON object
    - Specifies all four required output keys: description, tags, seo_title, seo_description
    """
    return (
        "You must return ONLY valid JSON. No explanation, no extra text, no markdown.\n\n"
        f"Product Name: {product.product_name}\n"
        f"Category: {product.category}\n"
        f"Price: ${product.price}\n\n"
        "Return this exact JSON structure with no other text:\n"
        "{\n"
        '  "description": "2-3 sentence product description",\n'
        '  "tags": "tag1, tag2, tag3, tag4, tag5",\n'
        '  "seo_title": "short SEO title under 60 chars",\n'
        '  "seo_description": "meta description under 160 chars"\n'
        "}"
    )
