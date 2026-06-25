const POSTCODE_SCRIPT_URL = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'

export interface PostcodeSelection {
  fullAddress: string
  roadAddress?: string | null
  jibunAddress?: string | null
  sido: string
  sigungu: string
  legalDong: string
  zonecode?: string | null
  buildingName?: string | null
}

interface DaumPostcodeData {
  address: string
  addressType: 'R' | 'J'
  userSelectedType: 'R' | 'J'
  roadAddress: string
  jibunAddress: string
  sido: string
  sigungu: string
  bname: string
  bname1?: string
  bname2?: string
  zonecode: string
  buildingName: string
}

interface DaumPostcodeOptions {
  oncomplete: (data: DaumPostcodeData) => void
  onclose?: (state: string) => void
}

interface DaumPostcodeInstance {
  open: () => void
}

declare global {
  interface Window {
    daum?: {
      Postcode: new (options: DaumPostcodeOptions) => DaumPostcodeInstance
    }
  }
}

let postcodeScriptPromise: Promise<void> | null = null

export class PostcodeError extends Error {
  constructor(readonly code: 'LOAD_FAILED' | 'CLOSED' | 'UNAVAILABLE') {
    super(code)
  }
}

export function loadPostcodeService(): Promise<void> {
  if (window.daum?.Postcode) {
    return Promise.resolve()
  }

  if (!postcodeScriptPromise) {
    postcodeScriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = POSTCODE_SCRIPT_URL
      script.async = true
      script.onload = () => resolve()
      script.onerror = () => {
        postcodeScriptPromise = null
        reject(new PostcodeError('LOAD_FAILED'))
      }
      document.head.appendChild(script)
    })
  }

  return postcodeScriptPromise
}

export async function openPostcodeSearch(): Promise<PostcodeSelection> {
  await loadPostcodeService()

  const Postcode = window.daum?.Postcode
  if (!Postcode) {
    throw new PostcodeError('UNAVAILABLE')
  }

  return new Promise((resolve, reject) => {
    let completed = false
    const popup = new Postcode({
      oncomplete(data) {
        completed = true
        resolve(normalizePostcodeSelection(data))
      },
      onclose() {
        if (!completed) {
          reject(new PostcodeError('CLOSED'))
        }
      }
    })

    popup.open()
  })
}

function normalizePostcodeSelection(data: DaumPostcodeData): PostcodeSelection {
  const roadAddress = emptyToNull(data.roadAddress)
  const jibunAddress = emptyToNull(data.jibunAddress)
  const fullAddress =
    (data.userSelectedType === 'J' ? jibunAddress : roadAddress) ??
    emptyToNull(data.address) ??
    roadAddress ??
    jibunAddress ??
    ''

  return {
    fullAddress,
    roadAddress,
    jibunAddress,
    sido: data.sido,
    sigungu: data.sigungu,
    legalDong: data.bname2 || data.bname1 || data.bname,
    zonecode: emptyToNull(data.zonecode),
    buildingName: emptyToNull(data.buildingName)
  }
}

function emptyToNull(value: string | undefined) {
  return value && value.trim().length > 0 ? value.trim() : null
}
