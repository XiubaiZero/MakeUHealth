import { existsSync, readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import ts from 'typescript'

// Execute real TS modules; mock only browser and network boundaries.
export function createTypeScriptLoader({ environment = {}, mocks = new Map() } = {}) {
  const cache = new Map()
  const toPath = value => resolve(value instanceof URL ? fileURLToPath(value) : value)
  const overrides = new Map([...mocks].map(([path, value]) => [toPath(path), value]))
  function load(pathOrUrl) {
    const filename = toPath(pathOrUrl)
    if (overrides.has(filename)) return overrides.get(filename)
    if (cache.has(filename)) return cache.get(filename).exports
    const module = { exports: {} }
    cache.set(filename, module)
    const source = readFileSync(filename, 'utf8').replaceAll('import.meta.env', '__environment')
    const compiled = ts.transpileModule(source, {
      compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS },
    }).outputText
    const externalRequire = createRequire(filename)
    const requireDependency = specifier => {
      if (!specifier.startsWith('.')) return externalRequire(specifier)
      const base = resolve(dirname(filename), specifier)
      const dependency = [base, `${base}.ts`, `${base}/index.ts`].find(candidate => existsSync(candidate) && candidate.endsWith('.ts'))
      if (!dependency) throw new Error(`Cannot resolve ${specifier} from ${filename}`)
      return load(dependency)
    }
    new Function('require', 'module', 'exports', '__environment', compiled)(requireDependency, module, module.exports, environment)
    return module.exports
  }
  return load
}
