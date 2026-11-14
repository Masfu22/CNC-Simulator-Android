// The browser pieces of the browser platform, kept apart so the platform itself runs in tests.

interface AndroidCncBridge {
  saveTextFile: (name: string, text: string, mimeType: string) => void
}

function androidBridge(): AndroidCncBridge | undefined {
  return (window as Window & { AndroidCnc?: AndroidCncBridge }).AndroidCnc
}

export function download(name: string, text: string, type: string): void {
  // Android WebView cannot reliably hand a Blob URL to DownloadManager. The native bridge
  // writes the same generated text into Android's Downloads/CNC Simulator directory.
  const native = androidBridge()
  if (native) {
    native.saveTextFile(name, text, type)
    return
  }
  const a = document.createElement('a')
  a.href = URL.createObjectURL(new Blob([text], { type }))
  a.download = name
  a.click()
  URL.revokeObjectURL(a.href)
}

/** The system file picker. Must be called from a click or key handler, as browsers require. */
export function pickFile(): Promise<{ name: string; text: string } | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.onchange = () => {
      const f = input.files?.[0]
      if (f) void f.text().then((text) => resolve({ name: f.name, text }))
      else resolve(null)
    }
    input.oncancel = () => resolve(null)
    input.click()
  })
}
