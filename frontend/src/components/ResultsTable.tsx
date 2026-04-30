import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import type { EnrichedProduct } from '../types'

interface ResultsTableProps {
  products: EnrichedProduct[]
}

function cleanField(value: string): string {
  return value.replace(/^["']+|["']+$/g, '').trim()
}

function truncate(value: string, maxLen: number): string {
  const clean = cleanField(value)
  return clean.length > maxLen ? clean.slice(0, maxLen - 1) + '…' : clean
}

function normalizeTags(raw: string): string {
  return raw.split(',').map((t) => cleanField(t)).filter(Boolean).join(', ')
}

const COLS = [
  { label: 'Product', key: 'product_name' as const, maxW: 'max-w-[180px]', chars: 32, hide: '' },
  { label: 'Category', key: 'category' as const, maxW: '', chars: 20, hide: '' },
  { label: 'Price', key: 'price' as const, maxW: '', chars: 12, hide: '' },
  { label: 'Description', key: 'description' as const, maxW: 'max-w-[220px]', chars: 72, hide: 'hidden md:table-cell' },
  { label: 'Tags', key: 'tags' as const, maxW: 'max-w-[160px]', chars: 36, hide: 'hidden lg:table-cell' },
  { label: 'SEO Title', key: 'seo_title' as const, maxW: 'max-w-[160px]', chars: 52, hide: 'hidden lg:table-cell' },
  { label: 'SEO Desc', key: 'seo_description' as const, maxW: 'max-w-[180px]', chars: 52, hide: 'hidden xl:table-cell' },
]

function ExpandedRow({ product }: { product: EnrichedProduct }) {
  return (
    <motion.tr
      initial={{ opacity: 0, y: -4 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -4 }}
      transition={{ duration: 0.18, ease: 'easeOut' }}
    >
      <td
        colSpan={8}
        className="bg-slate-800/60 backdrop-blur-sm px-6 py-5 border-b border-slate-700/40"
      >
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-3 text-xs">
          {/* Description */}
          <div>
            <p className="mb-1.5 font-mono text-cyan-500 uppercase tracking-widest text-[10px]">
              Description
            </p>
            <p className="text-slate-300 leading-relaxed">{cleanField(product.description)}</p>
          </div>

          {/* Tags */}
          <div>
            <p className="mb-1.5 font-mono text-cyan-500 uppercase tracking-widest text-[10px]">
              Tags
            </p>
            <div className="flex flex-wrap gap-1">
              {normalizeTags(product.tags)
                .split(', ')
                .filter(Boolean)
                .map((tag) => (
                  <span
                    key={tag}
                    className="rounded-sm border border-slate-600 bg-slate-700/50 px-2 py-0.5 text-slate-300"
                  >
                    {tag}
                  </span>
                ))}
            </div>
          </div>

          {/* SEO */}
          <div className="space-y-3">
            <div>
              <p className="mb-1 font-mono text-cyan-500 uppercase tracking-widest text-[10px]">
                SEO Title
              </p>
              <p className="text-slate-300">{cleanField(product.seo_title)}</p>
            </div>
            <div>
              <p className="mb-1 font-mono text-cyan-500 uppercase tracking-widest text-[10px]">
                SEO Description
              </p>
              <p className="text-slate-300">{cleanField(product.seo_description)}</p>
            </div>
          </div>
        </div>
      </td>
    </motion.tr>
  )
}

export function ResultsTable({ products }: ResultsTableProps) {
  const [expandedIdx, setExpandedIdx] = useState<number | null>(null)
  const toggle = (idx: number) => setExpandedIdx((prev) => (prev === idx ? null : idx))

  return (
    <div className="rounded border border-slate-700/60 overflow-hidden">
      <div className="overflow-x-auto overflow-y-auto max-h-[540px] custom-scrollbar">
        <table className="min-w-full text-xs">
          {/* Sticky header */}
          <thead className="sticky top-0 z-10 bg-slate-800 border-b border-slate-700">
            <tr>
              {COLS.map((col) => (
                <th
                  key={col.key}
                  scope="col"
                  className={`px-4 py-2.5 text-left font-mono text-[10px] uppercase tracking-widest text-slate-400 whitespace-nowrap ${col.hide}`}
                >
                  {col.label}
                </th>
              ))}
              <th scope="col" className="px-3 py-2.5 w-6" />
            </tr>
          </thead>

          <tbody>
            <AnimatePresence initial={false}>
              {products.map((product, idx) => (
                <>
                  <motion.tr
                    key={`row-${idx}`}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ duration: 0.15, delay: Math.min(idx * 0.015, 0.15) }}
                    onClick={() => toggle(idx)}
                    className={[
                      'cursor-pointer border-b border-slate-700/40',
                      'hover:bg-slate-700/40 transition-colors duration-100',
                      idx % 2 === 0 ? 'bg-slate-900/80' : 'bg-slate-800/30',
                      expandedIdx === idx ? 'bg-slate-700/50' : '',
                    ].join(' ')}
                  >
                    {/* Product Name */}
                    <td className={`px-4 py-2.5 text-slate-200 font-medium ${COLS[0].maxW} ${COLS[0].hide}`}>
                      <span title={cleanField(product.product_name)} className="block truncate">
                        {truncate(product.product_name, COLS[0].chars)}
                      </span>
                    </td>

                    {/* Category */}
                    <td className={`px-4 py-2.5 whitespace-nowrap ${COLS[1].hide}`}>
                      <span className="rounded-sm border border-slate-600/60 bg-slate-800 px-2 py-0.5 text-slate-400">
                        {cleanField(product.category)}
                      </span>
                    </td>

                    {/* Price */}
                    <td className={`px-4 py-2.5 whitespace-nowrap font-mono text-cyan-400 ${COLS[2].hide}`}>
                      ₹{cleanField(product.price)}
                    </td>

                    {/* Description */}
                    <td className={`px-4 py-2.5 text-slate-400 ${COLS[3].maxW} ${COLS[3].hide}`} title={cleanField(product.description)}>
                      <span className="block truncate">{truncate(product.description, COLS[3].chars)}</span>
                    </td>

                    {/* Tags */}
                    <td className={`px-4 py-2.5 text-slate-500 ${COLS[4].maxW} ${COLS[4].hide}`}>
                      <span className="block truncate font-mono">
                        {truncate(normalizeTags(product.tags), COLS[4].chars)}
                      </span>
                    </td>

                    {/* SEO Title */}
                    <td className={`px-4 py-2.5 text-slate-400 ${COLS[5].maxW} ${COLS[5].hide}`} title={cleanField(product.seo_title)}>
                      <span className="block truncate">{truncate(product.seo_title, COLS[5].chars)}</span>
                    </td>

                    {/* SEO Desc */}
                    <td className={`px-4 py-2.5 text-slate-500 ${COLS[6].maxW} ${COLS[6].hide}`} title={cleanField(product.seo_description)}>
                      <span className="block truncate">{truncate(product.seo_description, COLS[6].chars)}</span>
                    </td>

                    {/* Expand chevron */}
                    <td className="px-3 py-2.5 text-slate-600 text-[10px] whitespace-nowrap">
                      {expandedIdx === idx ? '▲' : '▼'}
                    </td>
                  </motion.tr>

                  <AnimatePresence>
                    {expandedIdx === idx && (
                      <ExpandedRow key={`expand-${idx}`} product={product} />
                    )}
                  </AnimatePresence>
                </>
              ))}
            </AnimatePresence>
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default ResultsTable
