import { useEffect, useRef, useState } from 'react'
import { motion } from 'framer-motion'

interface StatsCardProps {
  total: number
  processingTimeMs: number
}

function useCountUp(target: number, duration = 800) {
  const [value, setValue] = useState(0)
  const rafRef = useRef<number | null>(null)

  useEffect(() => {
    const start = performance.now()
    const animate = (now: number) => {
      const elapsed = now - start
      const progress = Math.min(elapsed / duration, 1)
      // ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3)
      setValue(eased * target)
      if (progress < 1) rafRef.current = requestAnimationFrame(animate)
    }
    rafRef.current = requestAnimationFrame(animate)
    return () => { if (rafRef.current) cancelAnimationFrame(rafRef.current) }
  }, [target, duration])

  return value
}

interface StatProps {
  label: string
  displayValue: string
  accent?: string
}

function Stat({ label, displayValue, accent = 'text-slate-100' }: StatProps) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className={`text-lg font-semibold tabular-nums ${accent}`}>{displayValue}</span>
      <span className="text-[10px] font-mono text-slate-500 uppercase tracking-wider">{label}</span>
    </div>
  )
}

export function StatsCard({ total, processingTimeMs }: StatsCardProps) {
  const totalSec = processingTimeMs / 1000
  const avgSec = total > 0 ? totalSec / total : 0

  const animatedTotal = useCountUp(total)
  const animatedTotalSec = useCountUp(totalSec)
  const animatedAvgSec = useCountUp(avgSec)

  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="rounded-sm border border-slate-700/60 bg-slate-900 px-4 sm:px-5 py-4"
    >
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-x-6 gap-y-4">
        <Stat
          label="Products"
          displayValue={String(Math.round(animatedTotal))}
          accent="text-cyan-400"
        />
        <Stat
          label="Total time"
          displayValue={`${animatedTotalSec.toFixed(2)}s`}
        />
        <Stat
          label="Avg / product"
          displayValue={`${animatedAvgSec.toFixed(2)}s`}
        />
      </div>
    </motion.div>
  )
}

export default StatsCard
