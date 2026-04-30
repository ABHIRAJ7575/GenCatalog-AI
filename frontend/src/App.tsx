import { useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { UploadZone } from './components/UploadZone'
import { ProgressBar } from './components/ProgressBar'
import { ResultsTable } from './components/ResultsTable'
import { ExportButton } from './components/ExportButton'
import { ErrorBanner } from './components/ErrorBanner'
import { StatsCard } from './components/StatsCard'
import { parseCsvFile, processCatalog, enrichProduct } from './api'
import type { EnrichedProduct } from './types'

interface ProcessingState {
  isProcessing: boolean
  progress: number
  total: number
  completed: number
  fallbackCount: number
  error: string | null
}

const initialState: ProcessingState = {
  isProcessing: false,
  progress: 0,
  total: 0,
  completed: 0,
  fallbackCount: 0,
  error: null,
}

const FALLBACK_MARKER = 'offers great value and is available'

type SystemStatus = 'idle' | 'processing' | 'done'

const STATUS_TEXT: Record<SystemStatus, string> = {
  idle: 'system idle — awaiting input',
  processing: 'processing pipeline — generating outputs',
  done: 'pipeline complete — results ready',
}

function App() {
  const [state, setState] = useState<ProcessingState>(initialState)
  const [products, setProducts] = useState<EnrichedProduct[]>([])
  const [processingTimeMs, setProcessingTimeMs] = useState<number>(0)

  const handleFileAccepted = async (file: File) => {
    setProducts([])
    setProcessingTimeMs(0)
    const startedAt = Date.now()
    setState({ ...initialState, isProcessing: true })

    let rows
    try {
      rows = await parseCsvFile(file)
    } catch {
      try {
        const data = await processCatalog(file)
        setProducts(data.products)
        setProcessingTimeMs(data.processing_time_ms)
        setState((prev) => ({
          ...prev,
          isProcessing: false,
          progress: 100,
          total: data.total,
          completed: data.total,
        }))
      } catch (backendErr) {
        const message =
          backendErr instanceof Error ? backendErr.message : 'An unexpected error occurred.'
        setState((prev) => ({ ...prev, isProcessing: false, error: message }))
      }
      return
    }

    const total = rows.length
    setState((prev) => ({ ...prev, total }))

    let completed = 0
    const promises = rows.map((product) =>
      enrichProduct(product).then((enriched) => {
        completed += 1
        const progress = Math.round((completed / total) * 100)
        const isFallback = enriched.description.includes(FALLBACK_MARKER)
        setProducts((prev) => [...prev, enriched])
        setState((prev) => ({
          ...prev,
          completed,
          progress,
          fallbackCount: prev.fallbackCount + (isFallback ? 1 : 0),
        }))
        return enriched
      })
    )

    await Promise.allSettled(promises)
    setProcessingTimeMs(Date.now() - startedAt)
    setState((prev) => ({ ...prev, isProcessing: false, progress: 100, completed: total }))
  }

  const { isProcessing, progress, total, completed, error } = state
  const hasResults = products.length > 0
  const isDone = !isProcessing && hasResults

  const systemStatus: SystemStatus = isProcessing ? 'processing' : isDone ? 'done' : 'idle'

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">

      {/* ── Top bar ── */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur-sm px-4 sm:px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="font-mono text-sm font-semibold tracking-[-0.02em] text-slate-100">
            GenCatalog<span className="text-cyan-400 font-bold tracking-[-0.01em]">AI</span>
          </span>
          <span className="hidden sm:inline-block h-4 w-px bg-slate-700" />
          <span className="hidden sm:inline text-xs text-slate-500 tracking-wide">
            LLM-driven enrichment with deterministic fallback.
          </span>
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 rounded-sm border border-slate-700 bg-slate-800 px-2 py-0.5 text-[10px] font-mono text-slate-400">
            <span className="h-1.5 w-1.5 rounded-full bg-cyan-400 animate-[pulse_3s_ease-in-out_infinite] inline-block" />
            <span className="hidden sm:inline">LLM + structured output</span>
            <span className="sm:hidden">LLM</span>
          </span>
        </div>
      </header>

      {/* ── Main content ── */}
      <main className="flex-1 mx-auto w-full max-w-6xl px-4 sm:px-6 py-6 sm:py-8 flex flex-col gap-5 sm:gap-6">

        {/* Upload — stacked on mobile, side-by-side on md+ */}
        <div className="flex flex-col md:flex-row gap-6 md:gap-10 md:items-stretch">

          {/* LEFT — upload card */}
          <div className="flex-1 w-full min-w-0 rounded-sm border border-slate-800 bg-slate-900/50 shadow-[0_2px_12px_rgba(0,0,0,0.35)] transition-colors duration-150 hover:border-slate-700">
            <UploadZone onFileAccepted={handleFileAccepted} disabled={isProcessing} />
          </div>

          {/* RIGHT — system identity panel */}
          <div className="w-full md:max-w-sm rounded-sm border border-slate-800 bg-slate-900/50 shadow-[0_2px_12px_rgba(0,0,0,0.35)] px-5 sm:px-6 py-5 sm:py-6 flex flex-col gap-4 sm:gap-5">

            {/* Top row: micro label + live signal */}
            <div className="flex items-center justify-between">
              <span className="font-mono text-[9px] tracking-[0.22em] uppercase text-slate-600">
                Catalog Enrichment Engine
              </span>
              <span className="flex items-center gap-1.5 font-mono text-[9px] text-slate-600">
                <span className="h-1.5 w-1.5 rounded-full bg-cyan-500 animate-[pulse_3s_ease-in-out_infinite] inline-block" />
                pipeline ready
              </span>
            </div>

            {/* Product name */}
            <div className="leading-none">
              <h1 className="text-3xl sm:text-[2.1rem] font-semibold tracking-[-0.02em] text-slate-100">
                GenCatalog
                <span className="text-cyan-400 font-bold tracking-[-0.01em]">AI</span>
              </h1>
            </div>

            {/* Description */}
            <p className="text-[13px] text-slate-500 leading-[1.65] tracking-[0.01em]">
              Transforms raw catalog data into structured outputs.<br />
              LLM-driven enrichment with deterministic fallback.
            </p>

            {/* Divider */}
            <div className="border-t border-slate-800" />

            {/* System capability chips */}
            <div className="flex flex-wrap gap-1.5">
              {['LLM Processing', 'JSON Structuring', 'Fallback Safe', 'Batch Enrichment'].map((label) => (
                <span
                  key={label}
                  className="inline-block rounded-[2px] border border-slate-800 px-2 py-0.5 font-mono text-[9px] tracking-[0.08em] text-slate-600"
                >
                  {label}
                </span>
              ))}
            </div>

          </div>
        </div>

        {/* Progress */}
        <AnimatePresence>
          {isProcessing && (
            <motion.section
              key="progress"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="space-y-1"
            >
              <ProgressBar progress={progress} isProcessing={isProcessing} />
              <p className="text-right text-[10px] font-mono text-slate-600">
                {completed} / {total} records
              </p>
            </motion.section>
          )}
        </AnimatePresence>

        {/* Error */}
        <AnimatePresence>
          <ErrorBanner
            error={error}
            onDismiss={() => setState((prev) => ({ ...prev, error: null }))}
          />
        </AnimatePresence>

        {/* System status line — always visible, transitions between states */}
        <AnimatePresence mode="wait">
          {(!hasResults || isProcessing || isDone) && (
            <motion.p
              key={systemStatus}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.4 }}
              className="text-[11px] font-mono tracking-[0.12em] text-slate-700"
              style={{ opacity: systemStatus === 'idle' ? 0.6 : 0.8 }}
            >
              {STATUS_TEXT[systemStatus]}
            </motion.p>
          )}
        </AnimatePresence>

        {/* Results */}
        <AnimatePresence>
          {hasResults && (
            <motion.section
              key="results"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3 }}
              className="flex flex-col gap-4"
            >
              {/* Stats */}
              {isDone && processingTimeMs > 0 && (
                <StatsCard total={total} processingTimeMs={processingTimeMs} />
              )}

              {/* Table toolbar */}
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                <p className="text-xs font-mono text-slate-500">
                  {products.length}
                  {total > 0 && products.length < total ? ` / ${total}` : ''} records
                  {isProcessing && (
                    <span className="ml-2 text-cyan-500 animate-pulse">● streaming</span>
                  )}
                </p>
                {isDone && (
                  <div className="w-full sm:w-auto">
                    <ExportButton products={products} />
                  </div>
                )}
              </div>

              <ResultsTable products={products} />
            </motion.section>
          )}
        </AnimatePresence>

      </main>
    </div>
  )
}

export default App
