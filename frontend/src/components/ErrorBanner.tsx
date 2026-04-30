import { AnimatePresence, motion } from 'framer-motion'

interface ErrorBannerProps {
  error: string | null
  onDismiss?: () => void
}

export function ErrorBanner({ error, onDismiss }: ErrorBannerProps) {
  return (
    <AnimatePresence>
      {error !== null && (
        <motion.div
          role="alert"
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -6 }}
          transition={{ duration: 0.18 }}
          className="flex items-start gap-3 rounded border border-red-700/60 bg-red-950/40
                     px-4 py-3 text-red-300"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="mt-0.5 h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
          </svg>
          <p className="flex-1 text-xs">{error}</p>
          {onDismiss && (
            <button
              type="button"
              onClick={onDismiss}
              aria-label="Dismiss"
              className="ml-auto text-red-500 hover:text-red-300 focus:outline-none text-sm leading-none"
            >
              ×
            </button>
          )}
        </motion.div>
      )}
    </AnimatePresence>
  )
}

export default ErrorBanner
