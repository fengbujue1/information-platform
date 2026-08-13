import { spawn } from 'node:child_process'
import { randomBytes } from 'node:crypto'
import { createServer as createHttpServer } from 'node:http'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { createServer as createViteServer } from 'vite'

const webDirectory = fileURLToPath(new URL('..', import.meta.url))
const backendDirectory = fileURLToPath(
  new URL('../../../backend/information-hub/', import.meta.url),
)
const playwrightCli = fileURLToPath(
  new URL('../node_modules/@playwright/test/cli.js', import.meta.url),
)
const backendJar = join(
  backendDirectory,
  'target',
  'information-hub-0.0.1-SNAPSHOT.jar',
)
const javaExecutable = process.env.JAVA_HOME
  ? join(
      process.env.JAVA_HOME,
      'bin',
      process.platform === 'win32' ? 'java.exe' : 'java',
    )
  : 'java'

const databaseUrl = requiredEnvironment('INFORMATION_HUB_E2E_DB_URL')
const databaseUsername = requiredEnvironment(
  'INFORMATION_HUB_E2E_DB_USERNAME',
)
const databasePassword = requiredEnvironment(
  'INFORMATION_HUB_E2E_DB_PASSWORD',
)
const e2eUsername = requiredEnvironment('INFORMATION_HUB_E2E_USERNAME')
const e2ePassword = requiredEnvironment('INFORMATION_HUB_E2E_PASSWORD')
const collectorToken = requiredEnvironment(
  'INFORMATION_HUB_E2E_COLLECTOR_TOKEN',
)
const previewSecret = requiredEnvironment(
  'INFORMATION_HUB_E2E_PREVIEW_HMAC_SECRET',
)

validateTestDatabase(databaseUrl)
if (Buffer.byteLength(previewSecret, 'utf8') < 32) {
  throw new Error(
    'INFORMATION_HUB_E2E_PREVIEW_HMAC_SECRET must contain at least 32 UTF-8 bytes',
  )
}

const providerApiKey = randomBytes(32).toString('base64url')
const provider = await startFakeProvider(providerApiKey)
const backendPort = await reservePort()
const backendUrl = `http://127.0.0.1:${backendPort}`
const backendOutput = []
const securityState = {
  providerSecretLeaked: false,
  rawPayloadLeaked: false,
}
let backend
let vite
let exitCode = 1

try {
  backend = startBackend({
    backendPort,
    providerBaseUrl: `http://127.0.0.1:${provider.port}/v1`,
    providerApiKey,
  })
  captureBackendOutput(
    backend,
    backendOutput,
    providerApiKey,
    securityState,
  )
  await waitForBackend(backendUrl, backend)
  await verifyCollectorAuthentication(backendUrl)

  vite = await createViteServer({
    root: webDirectory,
    logLevel: 'warn',
    server: {
      host: '127.0.0.1',
      port: 4173,
      strictPort: true,
      proxy: {
        '/api': {
          target: backendUrl,
          changeOrigin: true,
        },
      },
    },
  })
  await vite.listen()

  const playwright = spawn(
    process.execPath,
    [playwrightCli, 'test', 'phase3.spec.ts', '--workers=1'],
    {
      cwd: webDirectory,
      env: {
        ...process.env,
        INFORMATION_HUB_E2E_USERNAME: e2eUsername,
        INFORMATION_HUB_E2E_BACKEND_URL: backendUrl,
        INFORMATION_HUB_E2E_PASSWORD: e2ePassword,
        INFORMATION_HUB_E2E_COLLECTOR_TOKEN: collectorToken,
        INFORMATION_HUB_E2E_FORBIDDEN_PROVIDER_KEY: providerApiKey,
      },
      stdio: 'inherit',
      windowsHide: true,
    },
  )
  exitCode = await childExitCode(playwright)
} catch (error) {
  process.stderr.write(`${formatError(error)}\n`)
  if (backendOutput.length > 0) {
    process.stderr.write('Last backend output:\n')
    process.stderr.write(`${backendOutput.slice(-80).join('')}\n`)
  }
  exitCode = 1
} finally {
  if (vite) await vite.close()
  if (backend) await stopChild(backend)
  await provider.close()
}

if (securityState.providerSecretLeaked) {
  process.stderr.write('Provider API Key appeared in backend output.\n')
  exitCode = 1
}
if (securityState.rawPayloadLeaked) {
  process.stderr.write('rawPayload marker appeared in backend output.\n')
  exitCode = 1
}

process.exitCode = exitCode

function startBackend({ backendPort, providerBaseUrl, providerApiKey }) {
  return spawn(
    javaExecutable,
    [
      '-jar',
      backendJar,
      '--spring.config.location=classpath:/application.yml',
    ],
    {
      cwd: backendDirectory,
      env: {
        ...process.env,
        SERVER_PORT: String(backendPort),
        INFORMATION_HUB_DB_URL: databaseUrl,
        INFORMATION_HUB_DB_USERNAME: databaseUsername,
        INFORMATION_HUB_DB_PASSWORD: databasePassword,
        INFORMATION_HUB_FLYWAY_ENABLED: 'true',
        INFORMATION_HUB_BOOTSTRAP_USERNAME: e2eUsername,
        INFORMATION_HUB_BOOTSTRAP_PASSWORD: e2ePassword,
        INFORMATION_HUB_BOOTSTRAP_DISPLAY_NAME: 'Phase 3 E2E',
        INFORMATION_HUB_BOOTSTRAP_TIMEZONE: 'Asia/Shanghai',
        INFORMATION_HUB_COLLECTOR_TOKEN: collectorToken,
        INFORMATION_HUB_PREVIEW_HMAC_SECRET: previewSecret,
        INFORMATION_HUB_AI_PREVIEW_MAX_WINDOW_DAYS: '30',
        INFORMATION_HUB_AI_PREVIEW_MAX_CANDIDATES: '100',
        INFORMATION_HUB_AI_PREVIEW_MAX_ESTIMATED_TOKENS: '500000',
        INFORMATION_HUB_AI_ENABLED: 'true',
        INFORMATION_HUB_AI_BASE_URL: providerBaseUrl,
        INFORMATION_HUB_AI_API_KEY: providerApiKey,
        INFORMATION_HUB_AI_MODEL: 'phase3-e2e-model',
        INFORMATION_HUB_AI_TIMEOUT: '5s',
        INFORMATION_HUB_AI_MAX_OUTPUT_TOKENS: '5000',
        INFORMATION_HUB_AI_BATCH_WORKER_ENABLED: 'true',
        INFORMATION_HUB_AI_BATCH_WORKER_INITIAL_DELAY: '200ms',
        INFORMATION_HUB_AI_BATCH_WORKER_FIXED_DELAY: '200ms',
        INFORMATION_HUB_AI_SCHEDULE_DISPATCHER_ENABLED: 'true',
        INFORMATION_HUB_AI_SCHEDULE_DISPATCHER_INITIAL_DELAY: '200ms',
        INFORMATION_HUB_AI_SCHEDULE_DISPATCHER_FIXED_DELAY: '200ms',
        INFORMATION_HUB_AI_SCHEDULE_MISFIRE_GRACE: '5m',
      },
      stdio: ['ignore', 'pipe', 'pipe'],
      windowsHide: true,
    },
  )
}

function captureBackendOutput(
  child,
  output,
  providerSecret,
  securityState,
) {
  for (const stream of [child.stdout, child.stderr]) {
    stream.setEncoding('utf8')
    stream.on('data', (chunk) => {
      const safeChunk = chunk.replaceAll(providerSecret, '<redacted>')
      securityState.providerSecretLeaked ||= chunk.includes(providerSecret)
      securityState.rawPayloadLeaked ||=
        chunk.includes('PHASE3_E2E_PRIVATE_RAW_MARKER')
      output.push(safeChunk)
      if (output.length > 500) output.shift()
      process.stdout.write(safeChunk)
    })
  }
}

async function waitForBackend(baseUrl, child) {
  const deadline = Date.now() + 60_000
  let lastError
  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      throw new Error(`Information Hub exited with code ${child.exitCode}`)
    }
    try {
      const response = await fetch(`${baseUrl}/api/v1/auth/csrf`)
      if (response.ok) return
      lastError = new Error(`HTTP ${response.status}`)
    } catch (error) {
      lastError = error
    }

    await delay(300)
  }
  throw new Error(
    `Information Hub did not become ready: ${formatError(lastError)}`,
  )
}


async function verifyCollectorAuthentication(baseUrl) {
  const response = await fetch(`${baseUrl}/api/v1/collector/items`, {
    method: 'POST',
    headers: {
      authorization: `Bearer ${collectorToken}`,
      'content-type': 'application/json',
    },
    body: '{}',
  })
  if (response.status === 401 || response.status === 503) {
    let code = 'UNKNOWN'
    try {
      code = (await response.json()).code ?? code
    } catch {
      // Only the stable status is required for configuration diagnostics.
    }
    throw new Error(`Collector authentication preflight failed: ${code}`)
  }
}
async function startFakeProvider(expectedApiKey) {
  let requestCount = 0
  const server = createHttpServer(async (request, response) => {
    if (
      request.method !== 'POST' ||
      request.url !== '/v1/chat/completions'
    ) {
      respondJson(response, 404, { error: 'not found' })
      return
    }
    if (request.headers.authorization !== `Bearer ${expectedApiKey}`) {
      respondJson(response, 401, { error: 'unauthorized' })
      return
    }

    let body
    try {
      body = JSON.parse(await readBody(request))
	  if (body.max_tokens !== 5000) {
	  respondJson(response, 422, {
		error: `unexpected max_tokens: ${body.max_tokens}`,
	  })
	  return
	  }
    } catch {
      respondJson(response, 400, { error: 'invalid json' })
      return
    }
    requestCount += 1
    const serializedMessages = JSON.stringify(body.messages ?? [])
    if (serializedMessages.includes('PHASE3_E2E_PRIVATE_RAW_MARKER')) {
      respondJson(response, 422, { error: 'raw payload leaked to provider' })
      return
    }
    const failure = serializedMessages.includes('E2E_FORCE_INVALID_OUTPUT')
    const usage = failure
      ? { prompt_tokens: 11, completion_tokens: 7, total_tokens: 18 }
      : { prompt_tokens: 120, completion_tokens: 30, total_tokens: 150 }
    const content = failure
      ? JSON.stringify({ schemaVersion: 1 })
      : JSON.stringify({
          schemaVersion: 1,
          relevanceScore: 88,
          confidence: 0.91,
          summary: 'Synthetic Phase 3 E2E analysis.',
          positiveSignals: ['Java 21', 'Spring Boot', 'MySQL'],
          negativeSignals: [],
          attentionPoints: ['Verify details with the source.'],
          matchedPreferences: ['Java backend'],
          unmatchedPreferences: [],
        })

    respondJson(response, 200, {
      id: `phase3-e2e-request-${requestCount}`,
      model: 'phase3-e2e-model',
      choices: [
        {
          message: { role: 'assistant', content },
          finish_reason: 'stop',
        },
      ],
      usage,
    })
  })
  await new Promise((resolve, reject) => {
    server.once('error', reject)
    server.listen(0, '127.0.0.1', resolve)
  })
  const address = server.address()
  if (!address || typeof address === 'string') {
    throw new Error('Unable to determine Fake Provider port')
  }
  return {
    port: address.port,
    close: () =>
      new Promise((resolve, reject) => {
        server.close((error) => (error ? reject(error) : resolve()))
      }),
  }
}

async function readBody(request) {
  const chunks = []
  let size = 0
  for await (const chunk of request) {
    size += chunk.length
    if (size > 1024 * 1024) {
      request.destroy()
      throw new Error('Fake Provider request exceeded 1 MiB')
    }
    chunks.push(chunk)
  }
  return Buffer.concat(chunks).toString('utf8')
}

function respondJson(response, status, body) {
  const payload = JSON.stringify(body)
  response.writeHead(status, {
    'content-type': 'application/json',
    'content-length': Buffer.byteLength(payload),
  })
  response.end(payload)
}

async function reservePort() {
  const server = createHttpServer()
  await new Promise((resolve, reject) => {
    server.once('error', reject)
    server.listen(0, '127.0.0.1', resolve)
  })
  const address = server.address()
  if (!address || typeof address === 'string') {
    throw new Error('Unable to reserve backend port')
  }
  const port = address.port
  await new Promise((resolve, reject) => {
    server.close((error) => (error ? reject(error) : resolve()))
  })
  return port
}

async function stopChild(child) {
  if (child.exitCode !== null) return
  child.kill('SIGTERM')
  await Promise.race([childExitCode(child), delay(5_000)])
  if (child.exitCode === null) child.kill('SIGKILL')
}

function childExitCode(child) {
  return new Promise((resolve, reject) => {
    if (child.exitCode !== null) {
      resolve(child.exitCode)
      return
    }
    child.once('error', reject)
    child.once('exit', (code) => resolve(code ?? 1))
  })
}

function validateTestDatabase(url) {
  let databaseName
  try {
    const parsed = new URL(url.replace(/^jdbc:/, ''))
    databaseName = parsed.pathname.replace(/^\/+/, '').toLowerCase()
  } catch {
    throw new Error('INFORMATION_HUB_E2E_DB_URL must be a valid JDBC URL')
  }
  if (
    !databaseName ||
    (!databaseName.includes('test') && !databaseName.includes('e2e')) ||
    databaseName.includes('dev')
  ) {
    throw new Error(
      'Phase 3 E2E database name must contain test/e2e and must not contain dev',
    )
  }
}

function requiredEnvironment(name) {
  const value = process.env[name]
  if (!value?.trim()) {
    throw new Error(`${name} is required for Phase 3 E2E`)
  }
  return value
}

function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}

function formatError(error) {
  return error instanceof Error ? error.message : String(error)
}
