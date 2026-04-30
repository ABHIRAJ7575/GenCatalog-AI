import { useCallback, useState } from 'react'
import { useDropzone, FileRejection } from 'react-dropzone'
import { motion } from 'framer-motion'

interface UploadZoneProps {
  onFileAccepted: (file: File) => void
  disabled?: boolean
}

function downloadDemo() {
  const anchor = document.createElement('a')
  anchor.href = '/demo-phones.csv'
  anchor.download = 'demo-phones.csv'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
}

export function UploadZone({ onFileAccepted, disabled = false }: UploadZoneProps) {
  const [rejectionError, setRejectionError] = useState<string | null>(null)

  const onDrop = useCallback(
    (acceptedFiles: File[], fileRejections: FileRejection[]) => {
      setRejectionError(null)
      if (fileRejections.length > 0) {
        const ext = fileRejections[0].file.name.includes('.')
          ? '.' + fileRejections[0].file.name.split('.').pop()
          : 'unknown'
        setRejectionError(`Invalid file type: ${ext}. Only .csv accepted.`)
        return
      }
      if (acceptedFiles.length > 0) onFileAccepted(acceptedFiles[0])
    },
    [onFileAccepted]
  )

  const { getRootProps, getInputProps, isDragActive, open } = useDropzone({
    onDrop,
    accept: { 'text/csv': ['.csv'] },
    maxFiles: 1,
    disabled,
    noClick: true,
    noKeyboard: true,
  })

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.3 }}
      className="w-full"
    >
      {/* Cyan left-accent line gives the card a defined identity */}
      <div className="flex shadow-[0_1px_3px_rgba(0,0,0,0.4),0_0_0_1px_rgba(255,255,255,0.03)]">
        <div className="w-0.5 rounded-l bg-cyan-500/60 shrink-0" />

        <div
          {...getRootProps()}
          className={[
            'flex-1 flex flex-col items-center justify-center gap-4',
            'border border-l-0 px-8 py-9',
            'bg-slate-900 transition-colors duration-150',
            isDragActive
              ? 'border-cyan-500/80 bg-slate-800'
              : 'border-slate-700 hover:border-slate-500',
            disabled ? 'cursor-not-allowed opacity-40' : 'cursor-default',
          ].join(' ')}
        >
          <input {...getInputProps()} />

          {/* Icon */}
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5} aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
          </svg>

          <div className="text-center space-y-1">
            <p className="text-sm font-medium text-slate-200">
              {isDragActive ? 'Drop CSV file here' : 'Drop a CSV file or browse'}
            </p>
            <p className="text-xs text-slate-500 font-mono">
              Required columns: product_name · category · price
            </p>
          </div>

          <div className="flex items-center gap-2 flex-wrap justify-center">
            {/* Primary action — stronger fill */}
            <button
              type="button"
              onClick={open}
              disabled={disabled}
              className="rounded border border-slate-500 bg-slate-700 px-4 py-1.5 text-xs font-semibold
                         text-slate-100 hover:border-cyan-500 hover:bg-slate-600 hover:text-cyan-300
                         focus:outline-none focus:ring-1 focus:ring-cyan-500
                         transition-all duration-150 hover:scale-[1.02] active:scale-[0.99]
                         disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100 cursor-pointer"
            >
              Browse file
            </button>

            {/* Secondary action — outline only */}
            <button
              type="button"
              onClick={downloadDemo}
              disabled={disabled}
              className="rounded border border-slate-600 bg-transparent px-4 py-1.5 text-xs font-medium
                         text-slate-400 hover:border-blue-500 hover:text-blue-300
                         focus:outline-none focus:ring-1 focus:ring-blue-500
                         transition-all duration-150 hover:scale-[1.02] active:scale-[0.99]
                         disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100 cursor-pointer"
            >
              Try demo dataset
            </button>
          </div>
        </div>
      </div>

      {rejectionError && (
        <p role="alert" className="mt-2 text-xs text-red-400 font-mono pl-3">
          {rejectionError}
        </p>
      )}
    </motion.div>
  )
}

export default UploadZone
