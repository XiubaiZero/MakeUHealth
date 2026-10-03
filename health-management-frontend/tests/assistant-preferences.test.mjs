import assert from 'node:assert/strict'
import test from 'node:test'
import { createTypeScriptLoader } from './helpers/loadTypescript.mjs'
const { createPreferencesState } = createTypeScriptLoader()(new URL('../src/features/assistant/preferences/state.ts', import.meta.url))
const deferred = () => { let resolve, reject; const promise = new Promise((a,b) => { resolve=a; reject=b }); return { promise, resolve, reject } }
function server() {
  let value = { enterSendEnabled: true, revision: 0 }
  return { async get() { return { ...value } }, async update(enabled, expected) { if(expected !== value.revision) throw Object.assign(new Error('conflict'), { status: 409 }); value = { enterSendEnabled: enabled, revision: value.revision+1 }; return { ...value } } }
}
test('defaults enabled; changing one device is visible to the other and survives a new session', async () => {
  const api = server(), a = createPreferencesState(api, () => 'one'), b = createPreferencesState(api, () => 'one')
  assert.equal(a.enterSendEnabled.value, true)
  await Promise.all([a.refresh(), b.refresh()]); await a.save(false); await b.refresh()
  assert.equal(b.enterSendEnabled.value, false)
  const relogin = createPreferencesState(api, () => 'one'); await relogin.refresh(); assert.equal(relogin.enterSendEnabled.value, false)
})
test('first load failure retains enabled default; later failure retains confirmed disabled value', async () => {
  let fail = true
  const state = createPreferencesState({ get: async () => { if(fail) throw Error('offline'); return { enterSendEnabled:false, revision:1 } } }, () => 'one')
  await state.refresh(); assert.equal(state.enterSendEnabled.value,true); assert.match(state.error.value,/Failed to load/)
  fail=false; await state.refresh(); fail=true; await state.refresh(); assert.equal(state.enterSendEnabled.value,false)
})
test('failed and pending saves do not change keyboard mode; conflicts refresh and warn', async () => {
  const api=server(), a=createPreferencesState(api,()=> 'one'), b=createPreferencesState(api,()=> 'one')
  await a.refresh(); await b.refresh(); await a.save(false); await b.save(true)
  assert.equal(b.enterSendEnabled.value,false);assert.match(b.error.value,/another device/)
  const pending=deferred(), state=createPreferencesState({ get:api.get,update:()=>pending.promise },()=> 'one')
  await state.refresh();const save=state.save(true);assert.equal(state.enterSendEnabled.value,false)
  pending.reject(Error('offline'));await save;assert.equal(state.enterSendEnabled.value,false);assert.match(state.error.value,/Failed to save/)
})
test('late load and save responses cannot change a different account or a signed-out session', async () => {
  let scope='A';const pending=deferred()
  const state=createPreferencesState({ get:()=>pending.promise },()=>scope)
  const load=state.refresh();scope='B';state.checkScope();pending.resolve({enterSendEnabled:false,revision:2});await load
  assert.equal(state.enterSendEnabled.value,true);assert.equal(state.revision.value,null)
  const update=deferred(), second=createPreferencesState({get:async()=>({enterSendEnabled:true,revision:0}),update:()=>update.promise},()=>scope)
  await second.refresh();const save=second.save(false);scope='guest';second.reset();update.resolve({enterSendEnabled:false,revision:1});await save
  assert.equal(second.enterSendEnabled.value,true);assert.equal(second.revision.value,null)
})
test('an older GET cannot override a successful save; guest never accesses preferences', async () => {
  const api=server(), pending=deferred();let stale=false
  const state=createPreferencesState({ get:()=>stale?pending.promise:api.get(),update:api.update },()=> 'one')
  await state.refresh();stale=true;const load=state.refresh();await state.save(false);pending.resolve({enterSendEnabled:true,revision:0});await load
  assert.equal(state.enterSendEnabled.value,false);assert.equal(state.loading.value,false)
  const guest=createPreferencesState({get:async()=>assert.fail('guest GET'),update:async()=>assert.fail('guest PATCH')},()=> 'guest');await guest.refresh();await guest.save(false)
})
test('real API wrapper uses authenticated client and server revision contract', async () => {
  const calls=[];const load=createTypeScriptLoader({mocks:new Map([[new URL('../src/api/client.ts',import.meta.url),{default:{get:async path=>{calls.push(['GET',path]);return {data:{enterSendEnabled:true,revision:0}}},patch:async(path,body)=>{calls.push(['PATCH',path,body]);return {data:{enterSendEnabled:false,revision:1}}}}}]])})
  const {preferencesApi}=load(new URL('../src/features/assistant/preferences/api.ts',import.meta.url));await preferencesApi.get();await preferencesApi.update(false,0)
  assert.deepEqual(calls,[['GET','/assistant/preferences'],['PATCH','/assistant/preferences',{enterSendEnabled:false,expectedRevision:0}]])
})
