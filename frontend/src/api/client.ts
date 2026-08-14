const BASE_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  readonly status: number
  readonly code: string | null

  constructor(status: number, detail: string, code: string | null) {
    super(detail)
    this.status = status
    this.code = code
  }
}

interface RequestOptions {
  method?: string
  body?: unknown
  token?: string | null
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {}
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (options.token) headers['Authorization'] = `Bearer ${options.token}`

  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    })
  } catch {
    throw new ApiError(0, 'Não foi possível conectar ao servidor. Verifique se a API está no ar.', null)
  }

  if (!response.ok) {
    let detail = `Erro ${response.status}`
    let code: string | null = null
    try {
      const problem = await response.json()
      if (typeof problem.detail === 'string') detail = problem.detail
      if (typeof problem.code === 'string') code = problem.code
      if (problem.errors && typeof problem.errors === 'object') {
        detail = Object.entries(problem.errors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join('; ')
      }
    } catch {
      // non-JSON error body: keep the generic detail
    }
    throw new ApiError(response.status, detail, code)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
