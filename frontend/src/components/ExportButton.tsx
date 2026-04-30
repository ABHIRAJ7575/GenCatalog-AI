import Papa from 'papaparse'
import type { EnrichedProduct } from '../types'

interface ExportButtonProps {
  products: EnrichedProduct[]
}

export function ExportButton({ products }: ExportButtonProps) {
  const handleExport = () => {
    const csv = Papa.unparse(products)
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'enriched-catalog.csv'
    anchor.style.display = 'none'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    URL.revokeObjectURL(url)
  }

  return (
    <button
      type="button"
      onClick={handleExport}
      className="w-full sm:w-auto inline-flex items-center justify-center gap-1.5 rounded border border-slate-600 bg-slate-800
                 px-4 py-1.5 text-xs font-medium text-slate-200
                 hover:border-cyan-500 hover:bg-slate-700 hover:text-cyan-300
                 focus:outline-none focus:ring-1 focus:ring-cyan-500
                 transition-all duration-150 hover:scale-[1.02] active:scale-[0.99] cursor-pointer"
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
      </svg>
      Export CSV
    </button>
  )
}

export default ExportButton
