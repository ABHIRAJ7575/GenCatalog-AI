export interface Product {
  product_name: string
  category: string
  price: string
}

export interface EnrichedProduct {
  product_name: string
  category: string
  price: string
  description: string
  tags: string
  seo_title: string
  seo_description: string
}

export interface CatalogResponse {
  products: EnrichedProduct[]
  total: number
  processing_time_ms: number
}
