import { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'

interface ProgressBarProps {
  progress: number
  isProcessing: boolean
}

const MESSAGES = [
  'Parsing product catalog…',
  'Calling LLM pipeline…',
  'Extracting JSON from responses…',
  'Validating and cleaning output…',
  'Aggregating enriched records…',
]

export function ProgressBar({ progress, isProcessing }: ProgressBarProps) {
  const [msgIdx, setMsgIdx] = useState(0)

  useEffect(() => {
    if (!isProcessing) return
    const id = setInterval(() => setMsgIdx((i) => (i + 1) % MESSAGES.length), 2400)
    return () => clearInterval(id)
  }, [isProcessing])

  return (
    <div className="w-full space-y-1.5">
      <div className="flex items-center justify-between">
        <AnimatePresence mode="wait">
          <motion.span
            key={msgIdx}
            initial={{ opacity: 0, y: 3 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -3 }}
            transition={{ duration: 0.25 }}
            className="text-xs text-slate-400 font-mono"
          >
            {MESSAGES[msgIdx]}
          </motion.span>
        </AnimatePresence>
        <span className="text-xs text-slate-500 tabular-nums">{Math.round(progress)}%</span>
      </div>

      <div className="h-1 w-full overflow-hidden rounded-full bg-slate-800">
        <motion.div
          className="h-full rounded-full bg-cyan-500"
          animate={{ width: `${progress}%` }}
          transition={{ type: 'tween', duration: 0.3, ease: 'easeOut' }}
        />
      </div>
    </div>
  )
}

export default ProgressBar
