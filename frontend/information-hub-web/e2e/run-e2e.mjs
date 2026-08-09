import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'

import { createServer } from 'vite'

const projectDirectory = fileURLToPath(new URL('..', import.meta.url))
const playwrightCli = fileURLToPath(
  new URL('../node_modules/@playwright/test/cli.js', import.meta.url),
)

const server = await createServer({
  root: projectDirectory,
  logLevel: 'warn',
  server: {
    host: '127.0.0.1',
    port: 4173,
    strictPort: true,
  },
})

let exitCode = 1
try {
  await server.listen()

  const child = spawn(
    process.execPath,
    [
      playwrightCli,
      'test',
      'identity.spec.ts',
      'jobs.spec.ts',
      'recommendations.spec.ts',
      ...process.argv.slice(2),
    ],
    {
      cwd: projectDirectory,
      env: process.env,
      stdio: 'inherit',
    },
  )

  exitCode = await new Promise((resolve, reject) => {
    child.once('error', reject)
    child.once('exit', (code) => resolve(code ?? 1))
  })
} finally {
  await server.close()
}

process.exitCode = exitCode
