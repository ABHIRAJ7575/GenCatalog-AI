from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from models import ProductRequest, EnrichedProduct
from generator import generate_product_content

app = FastAPI(title="GenCatalog AI Engine")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/generate", response_model=EnrichedProduct)
async def generate(product: ProductRequest) -> EnrichedProduct:
    """
    Generate enriched product content from a ProductRequest.

    Accepts: { product_name, category, price }
    Returns: { description, tags, seo_title, seo_description }

    Requirements: 5.1–5.5, 6.1–6.5, 12.5
    """
    return await generate_product_content(product)
