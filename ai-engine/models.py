from pydantic import BaseModel


class ProductRequest(BaseModel):
    product_name: str
    category: str
    price: str


class EnrichedProduct(BaseModel):
    description: str
    tags: str
    seo_title: str
    seo_description: str
