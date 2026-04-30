import axios from 'axios'
import * as XLSX from 'xlsx'
import type { CatalogResponse, EnrichedProduct, Product } from './types'

const baseUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

/** Timeout per product request in milliseconds */
const PRODUCT_TIMEOUT_MS = 10_000

// ─── Flexible header mapping ──────────────────────────────────────────────────

const normalize = (str: string) => str.toLowerCase().replace(/[\s_]/g, '')

function mapHeaders(headers: string[]): Record<string, string> {
  const mapping: Record<string, string> = {}

  headers.forEach((h) => {
    const key = normalize(h)

    if (['name', 'product', 'productname', 'title'].includes(key)) {
      mapping.product_name = h
    }
    if (['category', 'cat', 'maincate', 'maincategory', 'type'].includes(key)) {
      mapping.category = h
    }
    if (['price', 'amount', 'actualprice', 'cost', 'rate'].includes(key)) {
      mapping.price = h
    }
  })

  return mapping
}

// ─── CSV / XLSX parsing ───────────────────────────────────────────────────────

function parseRows(headers: string[], dataRows: string[][]): Product[] {
  const mapped = mapHeaders(headers)

  if (!mapped.product_name || !mapped.category || !mapped.price) {
    const missing = [
      !mapped.product_name && 'product name (name / product / title)',
      !mapped.category && 'category (category / cat / type)',
      !mapped.price && 'price (price / amount / cost)',
    ].filter(Boolean)
    throw new Error(`CSV missing required columns: ${missing.join(', ')}`)
  }

  const products: Product[] = []
  for (const cols of dataRows) {
    const nameIdx = headers.indexOf(mapped.product_name)
    const catIdx = headers.indexOf(mapped.category)
    const priceIdx = headers.indexOf(mapped.price)

    const name = (cols[nameIdx] ?? '').trim()
    const cat = (cols[catIdx] ?? '').trim()
    const rawPrice = (cols[priceIdx] ?? '').trim()
    const price = String(parseFloat(rawPrice) || rawPrice)

    if (name && cat && rawPrice) {
      products.push({ product_name: name, category: cat, price })
    }
  }

  if (products.length === 0) {
    throw new Error('CSV file contains no valid product rows')
  }

  return products
}

/**
 * Parse a CSV or XLSX File in the browser and return an array of Product rows.
 * Supports flexible column names (name, product, title → product_name, etc.)
 */
export function parseCsvFile(file: File): Promise<Product[]> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    const isXlsx =
      file.name.endsWith('.xlsx') ||
      file.name.endsWith('.xls') ||
      file.type.includes('spreadsheet') ||
      file.type.includes('excel')

    if (isXlsx) {
      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target?.result as ArrayBuffer)
          const workbook = XLSX.read(data, { type: 'array' })
          const sheet = workbook.Sheets[workbook.SheetNames[0]]
          const rows: string[][] = XLSX.utils.sheet_to_json(sheet, { header: 1 })

          if (rows.length < 2) {
            reject(new Error('File contains no product rows'))
            return
          }

          const headers = (rows[0] as string[]).map((h) => String(h ?? '').trim())
          const dataRows = rows.slice(1).map((r) => (r as string[]).map((c) => String(c ?? '').trim()))

          resolve(parseRows(headers, dataRows))
        } catch (err) {
          reject(err)
        }
      }
      reader.onerror = () => reject(new Error('Failed to read file'))
      reader.readAsArrayBuffer(file)
    } else {
      reader.onload = (e) => {
        try {
          const text = (e.target?.result as string) ?? ''
          const lines = text.split(/\r?\n/).filter((l) => l.trim() !== '')

          if (lines.length < 2) {
            reject(new Error('CSV file contains no product rows'))
            return
          }

          const headers = lines[0].split(',').map((h) => h.trim())
          const dataRows = lines.slice(1).map((l) => l.split(',').map((c) => c.trim()))

          resolve(parseRows(headers, dataRows))
        } catch (err) {
          reject(err)
        }
      }
      reader.onerror = () => reject(new Error('Failed to read CSV file'))
      reader.readAsText(file)
    }
  })
}

// ─── Backend endpoints ────────────────────────────────────────────────────────

/**
 * Send the full CSV to the backend and get back a CatalogResponse.
 * Used as a fallback / single-shot path.
 */
export async function processCatalog(file: File): Promise<CatalogResponse> {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await axios.post<CatalogResponse>(
      `${baseUrl}/process-catalog`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    return response.data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.data?.error) {
      throw new Error(error.response.data.error)
    }
    throw error
  }
}

/**
 * Enrich a single product via the AI engine endpoint.
 * Times out after PRODUCT_TIMEOUT_MS and returns a client-side fallback on failure.
 */
export async function enrichProduct(product: Product): Promise<EnrichedProduct> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), PRODUCT_TIMEOUT_MS)

  try {
    const response = await axios.post<EnrichedProduct>(
      `${baseUrl}/enrich`,
      product,
      {
        headers: { 'Content-Type': 'application/json' },
        signal: controller.signal,
      }
    )
    clearTimeout(timer)
    return response.data
  } catch (err) {
    clearTimeout(timer)
    return buildClientFallback(product)
  }
}

/** Build a client-side fallback EnrichedProduct when the API call fails or times out. */
function buildClientFallback(product: Product): EnrichedProduct {
  const { product_name, category, price } = product
  return {
    product_name,
    category,
    price,
    description:
      `${product_name} is a quality ${category.toLowerCase()} product priced at ${price}. ` +
      'This item offers great value and is available for purchase now.',
    tags: `${product_name}, ${category}, product, buy, online`,
    seo_title: `${product_name} - ${category}`.slice(0, 59),
    seo_description:
      `Buy ${product_name} in the ${category} category for ${price}. Quality product with fast shipping.`.slice(
        0,
        160
      ),
  }
}
