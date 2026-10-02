import fs from 'node:fs'
const rows=fs.readFileSync('evaluation/ai-memory/results.jsonl','utf8').trim().split('\n').map(JSON.parse)
const groups=new Map(), columns=['case_id','group','value','ok','whole_turn_ms','calls','prompt_tokens','cached_prompt_tokens','completion_tokens','finish_reasons','estimated_peak_cny','screen_hits','screen_expected','error']
const csv=[columns.join(',')], escape=value=>`"${String(value??'').replaceAll('"','""')}"`
let proofs=0, validProofs=0
for(const row of rows){
 const usages=row.calls.map(c=>c.response?.usage||{}),sum=key=>usages.reduce((n,u)=>n+(u[key]||0),0),cost=row.calls.reduce((n,c)=>n+(c.estimatedCny??c.reservedCny),0)
 csv.push([row.input.id,row.input.group,row.input.value,row.result.ok,row.result.turnMillis,row.calls.length,sum('prompt_tokens'),sum('prompt_cache_hit_tokens'),sum('completion_tokens'),row.calls.map(c=>c.response?.choices?.[0]?.finish_reason||'unknown').join('|'),cost.toFixed(8),row.automaticScreen.hits.length,row.input.expected.length,row.result.error].map(escape).join(','))
 const key=`${row.input.group}/${row.input.value}`
 if(!groups.has(key))groups.set(key,[]);groups.get(key).push(row)
 for(const candidate of row.result.candidates||[])for(const source of candidate.sources){proofs++;if(row.input.userMessages[Number(source.messageId.slice(1))]?.includes(source.evidence))validProofs++}
}
fs.writeFileSync('evaluation/ai-memory/cases.csv',csv.join('\n')+'\n')
const table=['| 参数 / 取值 | 案例数 | 调用成功 | 关键词筛查 | 整轮耗时范围 ms | 实际输入 / 输出 token 总计 | 高峰价估算 元 |','| --- | ---: | ---: | ---: | --- | --- | ---: |']
for(const [key,records] of groups){const calls=records.flatMap(r=>r.calls);const tokens=key=>calls.reduce((n,c)=>n+(c.response?.usage?.[key]||0),0);table.push(`| ${key} | ${records.length} | ${records.filter(r=>r.result.ok).length}/${records.length} | ${records.reduce((n,r)=>n+r.automaticScreen.hits.length,0)}/${records.reduce((n,r)=>n+r.input.expected.length,0)} | ${Math.min(...records.map(r=>r.result.turnMillis))}–${Math.max(...records.map(r=>r.result.turnMillis))} | ${tokens('prompt_tokens')} / ${tokens('completion_tokens')} | ${calls.reduce((n,c)=>n+(c.estimatedCny??c.reservedCny),0).toFixed(6)} |`)}
fs.writeFileSync('evaluation/ai-memory/comparison.md',table.join('\n')+'\n')
const calls=rows.flatMap(r=>r.calls)
console.log(JSON.stringify({cases:rows.length,calls:calls.length,proofs,validProofs,failures:rows.filter(r=>!r.result.ok).map(r=>r.input.id),promptRange:[Math.min(...calls.map(c=>c.response?.usage?.prompt_tokens??Infinity)),Math.max(...calls.map(c=>c.response?.usage?.prompt_tokens??0))],maxCompletion:Math.max(...calls.map(c=>c.response?.usage?.completion_tokens??0)),maxCallMillis:Math.max(...calls.map(c=>c.elapsedMillis)),maxTurnMillis:Math.max(...rows.map(r=>r.result.turnMillis))}))
