import fs from 'node:fs'
import http from 'node:http'
import { spawn } from 'node:child_process'
import readline from 'node:readline'
import { cases } from './cases.mjs'

// Run from repository root after test-compile and dependency:build-classpath.
// Credentials stay in this process. The real Java client targets a localhost budget gateway.
if (!process.env.DEEPSEEK_API_KEY) throw new Error('DEEPSEEK_API_KEY is required; never pass it as an argument.')
fs.mkdirSync('.validation', { recursive: true })
const ledgerPath = '.validation/memory-paid-ledger.json', resultsPath = 'evaluation/ai-memory/results.jsonl'
const limitCalls = 120, limitCny = 10, rates = { cacheHit: 0.04, cacheMiss: 2, output: 8 }
const ledger = fs.existsSync(ledgerPath) ? JSON.parse(fs.readFileSync(ledgerPath, 'utf8')) : []
const save = () => fs.writeFileSync(ledgerPath, JSON.stringify(ledger, null, 2))
const exposure = () => ledger.reduce((sum, r) => sum + (r.estimatedCny ?? r.reservedCny), 0)
let currentCase = null
const server = http.createServer(async (request, response) => {
  if (request.method !== 'POST' || request.url !== '/chat/completions') { response.writeHead(404).end(); return }
  try {
    let body = ''; for await (const chunk of request) { body += chunk; if (body.length > 300000) throw new Error('Input too large') }
    const payload = JSON.parse(body)
    if (payload.model !== 'deepseek-flash' || payload.thinking?.type !== 'disabled' || !Number.isInteger(payload.max_tokens) || payload.max_tokens > 8192) throw new Error('Unexpected evaluation model/options')
    const inputUpper = Buffer.byteLength(JSON.stringify(payload.messages), 'utf8') + 1024
    const reservedCny = (inputUpper * rates.cacheMiss + payload.max_tokens * rates.output) / 1e6
    if (ledger.length >= limitCalls || exposure() + reservedCny > limitCny) { response.writeHead(429).end(JSON.stringify({ error: 'Evaluation budget exhausted' })); return }
    const entry = { index: ledger.length + 1, caseId: currentCase, startedAt: new Date().toISOString(), reservedCny, request: payload }
    ledger.push(entry); save() // reserve before sending, including a timed-out request that might still be billed
    const started = performance.now()
    try {
      const upstream = await fetch('https://api.deepseek.com/chat/completions', { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${process.env.DEEPSEEK_API_KEY}` }, body, signal: AbortSignal.timeout(65000) })
      const raw = await upstream.text(), parsed = JSON.parse(raw)
      entry.elapsedMillis = Math.round(performance.now() - started); entry.httpStatus = upstream.status; entry.response = parsed
      const usage = parsed.usage
      if (usage && Number.isFinite(usage.prompt_tokens) && Number.isFinite(usage.completion_tokens)) {
        const hit = usage.prompt_cache_hit_tokens ?? 0, miss = usage.prompt_cache_miss_tokens ?? usage.prompt_tokens - hit
        entry.estimatedCny = (hit * rates.cacheHit + miss * rates.cacheMiss + usage.completion_tokens * rates.output) / 1e6
      }
      save(); response.writeHead(upstream.status, { 'Content-Type': 'application/json' }).end(raw)
    } catch (error) { entry.error = error.name; entry.elapsedMillis = Math.round(performance.now() - started); save(); response.writeHead(502).end(JSON.stringify({ error: 'Evaluation upstream failure' })) }
  } catch { response.writeHead(400).end(JSON.stringify({ error: 'Invalid evaluation input' })) }
})
await new Promise(resolve => server.listen(14179, '127.0.0.1', resolve))
const dependencies = fs.readFileSync('back_end/target/evaluation-classpath.txt', 'utf8').trim()
const java = process.env.JAVA_HOME ? `${process.env.JAVA_HOME}/bin/java.exe` : 'C:/Users/ASUS/.jdks/ms-17.0.17/bin/java.exe'
const childEnv = { ...process.env }; delete childEnv.DEEPSEEK_API_KEY
const child = spawn(java, ['-Dfile.encoding=UTF-8', '-cp', `back_end/target/test-classes;back_end/target/classes;${dependencies}`, 'com.example.ipd_sp_back_end.service.AssistantMemoryEvaluation'], { env: childEnv, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] })
child.stderr.pipe(fs.createWriteStream('.validation/memory-evaluation-java.log'))
const pending = new Map()
readline.createInterface({ input: child.stdout }).on('line', line => {
  if (!line.startsWith('RESULT ')) return
  const value = JSON.parse(line.slice(7)), resolve = pending.get(value.id); if (resolve) { pending.delete(value.id); resolve(value) }
})
child.on('exit', () => { for (const resolve of pending.values()) resolve({ ok: false, error: 'Evaluation JVM exited' }); pending.clear() })
const previous = fs.existsSync(resultsPath) ? fs.readFileSync(resultsPath, 'utf8').trim().split('\n').filter(Boolean).map(s => JSON.parse(s)) : []
const done = new Set(previous.map(r => r.input.id))
try {
  for (const input of cases()) {
    if (done.has(input.id)) continue
    if (ledger.length >= limitCalls || exposure() >= limitCny - 0.15) break
    currentCase = input.id
    const before = ledger.length
    const result = await new Promise(resolve => { pending.set(input.id, resolve); child.stdin.write(JSON.stringify(input) + '\n') })
    // Wait until a timed-out upstream has resolved and is accounted for before another paid request.
    while (ledger.slice(before).some(e => e.httpStatus === undefined && e.error === undefined)) await new Promise(resolve => setTimeout(resolve, 250))
    const calls = ledger.slice(before)
    const text = (result.answer || result.summary || result.candidates?.map(c => c.content).join(' ') || '').toLowerCase()
    const hits = (input.expected || []).filter(atom => text.includes(atom.toLowerCase()))
    const forbidden = (input.forbidden || []).filter(atom => text.includes(atom.toLowerCase()))
    const record = { input, result, calls, automaticScreen: { hits, expected: input.expected || [], forbidden, requiresManualReview: true } }
    fs.appendFileSync(resultsPath, JSON.stringify(record) + '\n')
    console.log(JSON.stringify({ caseId: input.id, ok: result.ok, calls: calls.length, milliseconds: result.turnMillis, hits: `${hits.length}/${input.expected?.length || 0}`, exposureCny: Number(exposure().toFixed(4)) }))
  }
} finally {
  child.stdin.end(); server.close()
  fs.writeFileSync('evaluation/ai-memory/budget.json', JSON.stringify({ startedAt: ledger[0]?.startedAt, completedAt: new Date().toISOString(), model: 'deepseek-flash', thinking: 'disabled', limitCalls, limitCny, calls: ledger.length, estimatedPeakCny: exposure(), unresolvedReservations: ledger.filter(r => r.estimatedCny === undefined).map(r => ({ caseId: r.caseId, reservedCny: r.reservedCny })), ratesCnyPerMillion: rates, pricingSource: 'https://api-docs.deepseek.com/zh-cn/quick_start/pricing/', billing: 'Conservative peak-price estimate from usage, plus unresolved maximum reservations; not an account invoice.' }, null, 2) + '\n')
}
