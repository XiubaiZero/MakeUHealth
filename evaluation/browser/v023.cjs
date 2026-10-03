const fs=require('fs'),path=require('path'),assert=require('node:assert/strict'),{spawn}=require('child_process');
const root=path.resolve(__dirname,'../..');
const {chromium}=require(path.join(root,'.validation/browser-runtime/node_modules/playwright'));
const records=[],preferences=new Map(),conversations=new Map();let counter=0,prefFailures=false,saveFailures=false,offline=false,holdTurns=false,turns=0,creates=0;
const clone=x=>JSON.parse(JSON.stringify(x));const state=account=>{if(!preferences.has(account))preferences.set(account,{enterSendEnabled:true,revision:0});return preferences.get(account)};
const run=async()=>{
 const vite=spawn(process.execPath,['node_modules/vite/bin/vite.js','--host','127.0.0.1','--port','15173','--strictPort'],{cwd:path.join(root,'health-management-frontend'),windowsHide:true,stdio:['ignore','pipe','pipe']});
 const log=fs.createWriteStream(path.join(root,'.validation/browser-vite.log'));vite.stdout.pipe(log);vite.stderr.pipe(log);
 let browser;
 try{
  for(let i=0;i<100;i++){try{if((await fetch('http://127.0.0.1:15173')).ok)break}catch{}await new Promise(r=>setTimeout(r,100));}
  browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
  async function device(account=1,width=1280){
   const context=await browser.newContext({viewport:{width,height:900}});
   await context.addInitScript(({account})=>{localStorage.setItem('health-management-auth-session',JSON.stringify({accountId:account,account:'test'+account+'@example.test',accountType:'email',token:'synthetic-'+account,loginAt:'2026-10-03'}));localStorage.setItem('health-management-language','zh-CN');localStorage.setItem('smart-assistant-intro-seen-v1','true')},{account});
   await context.route(url=>url.pathname.startsWith('/api/'),async route=>{
    const request=route.request(),url=new URL(request.url()),p=url.pathname.replace(/^\/api/,''),method=request.method();let body=request.postDataJSON(),value,status=200;
    const owner=Number((request.headers().authorization||'').split('-').at(-1))||account;
    if(p==='/assistant/preferences'){
     if(method==='GET'){if(prefFailures)status=503;else value=clone(state(owner));}
     else {if(saveFailures)status=503;else if(body.expectedRevision!==state(owner).revision)status=409;else{value={enterSendEnabled:body.enterSendEnabled,revision:state(owner).revision+1};preferences.set(owner,value);}}
    }else if(p==='/users') value=[{id:owner,age:30,gender:'male',height:175,weight:80}];
    else if(p.includes('/fitness-goals/dashboard'))value={};
    else if(p==='/assistant/conversations'){
     if(offline)status=503;
     else if(method==='GET')value={items:[...conversations.values()].filter(x=>x.account===owner).map(x=>x.conversation),hasMore:false};
     else {creates++;const conversation={id:'chat-'+(++counter),title:null,revision:0,createdAt:'2026-10-03',updatedAt:'2026-10-03'};conversations.set(conversation.id,{account:owner,conversation,messages:[]});value=conversation;}
    }else if(/\/assistant\/conversations\/[^/]+\/messages$/.test(p)){
     if(offline)status=503;else {const item=conversations.get(p.split('/')[3]);value={conversation:item.conversation,messages:item.messages,nextBefore:null,activeTask:null,failedTask:null};}
    }else if(/\/assistant\/conversations\/[^/]+\/turns$/.test(p)){
     turns++;const item=conversations.get(p.split('/')[3]);if(holdTurns)await new Promise(r=>setTimeout(r,1200));
     if(body.editMessageId){const index=item.messages.findIndex(x=>x.id===body.editMessageId);item.messages.splice(index);}
     const sequence=item.messages.length+1;
     const task={id:body.requestId,requestId:body.requestId,questionId:'q-'+body.requestId,answerId:'a-'+body.requestId,status:'completed',error:null};
     item.messages.push({id:task.questionId,role:'user',sequence,content:body.message,language:body.language,source:'user',createdAt:'2026-10-03'},{id:task.answerId,role:'assistant',sequence:sequence+1,content:'模拟健康回答',language:body.language,source:'api',createdAt:'2026-10-03'});item.conversation.revision+=2;value=task;
    }else if(/\/assistant\/conversations\/[^/]+\/memory$/.test(p))value={enabled:true,revision:0,contentRevision:0,hasSummary:false,coveredSequence:0};
    else value=[];
    await route.fulfill({status,contentType:'application/json',body:JSON.stringify(value??{message:'Synthetic failure'})});
   });
   const page=await context.newPage();page.on('pageerror',e=>console.log('PAGE ERROR '+e.message));page.on('console',m=>{if(m.type()==='error')console.log('CONSOLE '+m.text())});await page.goto('http://127.0.0.1:15173/smart-assistant');await page.locator('.chat-input-wrap textarea').waitFor().catch(async e=>{console.log('PAGE URL '+page.url());fs.writeFileSync(path.join(root,'.validation/browser-failed.html'),await page.content());throw e});await page.waitForFunction(()=>!document.querySelector('.sync-notice'));
   return {context,page,input:page.locator('.chat-input-wrap textarea')};
  }
  const a=await device(),b=await device(),other=await device(2);
  const input=a.input;
  await input.fill('  \n　');let before=turns,created=creates;await input.press('Enter');await a.page.getByRole('alertdialog').waitFor();assert.equal(turns,before);assert.equal(creates,created);assert.equal(await input.inputValue(),'  \n　');
  await a.page.keyboard.press('Escape');assert.equal(await input.evaluate(e=>document.activeElement===e),true);records.push('blank Enter warns without creating conversation or message; Escape restores focus');
  await input.fill('');await a.page.locator('.chat-input-wrap button[type=submit]').click();await a.page.getByRole('alertdialog').waitFor();await a.page.getByRole('button',{name:'确定',exact:true}).click();records.push('blank Send button uses the same modal');
  await input.fill('abcXYZdef');await input.evaluate(e=>e.setSelectionRange(3,6));await input.press('Control+Enter');assert.equal(await input.inputValue(),'abc\ndef');assert.equal(await input.evaluate(e=>e.selectionStart),4);records.push('Ctrl Enter replaces selection with newline and restores caret');
  await input.fill('健康计划');before=turns;await input.press('Shift+Enter');assert.equal(await input.inputValue(),'健康计划\n');assert.equal(turns,before);records.push('Shift Enter inserts newline');
  await input.fill('健康计划');holdTurns=true;await input.press('Enter');await input.dispatchEvent('keydown',{key:'Enter',repeat:true});await a.page.waitForFunction(()=>!document.querySelector('.chat-input-wrap textarea')?.disabled);holdTurns=false;assert.equal(turns,before+1);records.push('Enter sends exactly once during repeated keys and pending generation');
  const cdp=await a.context.newCDPSession(a.page);await input.fill('');await input.click();before=turns;
  await cdp.send('Input.imeSetComposition',{text:'健康',selectionStart:2,selectionEnd:2});await input.press('Enter');assert.equal(turns,before);await cdp.send('Input.insertText',{text:'健康'});assert.equal(turns,before);await input.press('Enter');await a.page.waitForFunction(()=>!document.querySelector('.chat-input-wrap textarea')?.disabled);assert.equal(turns,before+1);await cdp.detach();records.push('Chromium native composition lifecycle: confirm does not send, next Enter sends');
  before=turns-1;await input.fill('中文草稿');await input.dispatchEvent('compositionstart');await input.dispatchEvent('keydown',{key:'Enter',keyCode:229,isComposing:true});assert.equal(turns,before+1);await input.dispatchEvent('compositionend');assert.equal(await input.inputValue(),'中文草稿');records.push('IME composition Enter does not submit (synthetic composition events)');
  await a.page.locator('.settings-button').click();const toggle=a.page.getByRole('switch',{name:'开启回车键发送消息'});await toggle.waitFor();assert.equal(await toggle.getAttribute('aria-checked'),'true');await toggle.click();await a.page.waitForFunction(()=>document.querySelector('[role=switch]')?.getAttribute('aria-checked')==='false');await a.page.getByRole('button',{name:'关闭设置'}).click();
  await input.fill('保留草稿');before=turns;await input.press('Enter');assert.equal(await input.inputValue(),'保留草稿\n');assert.equal(turns,before);records.push('disabled setting restores Enter newline');
  await b.page.locator('.settings-button').click();await b.page.waitForFunction(()=>document.querySelector('[role=switch]')?.getAttribute('aria-checked')==='false');await b.page.getByRole('button',{name:'关闭设置'}).click();records.push('two independent browser contexts share confirmed server preference');
  await other.page.locator('.settings-button').click();assert.equal(await other.page.getByRole('switch').getAttribute('aria-checked'),'true');await other.page.getByRole('button',{name:'关闭设置'}).click();records.push('different account retains default enabled');
  await b.page.reload();await b.input.waitFor();await b.page.waitForFunction(()=>document.querySelector('.composer-hint')?.textContent.includes('Enter 换行'));records.push('page reload restores server preference');
  await a.page.locator('.settings-button').click();saveFailures=true;await toggle.click();await a.page.locator('.settings-hint').filter({hasText:'聊天设置保存失败'}).waitFor();assert.equal(await toggle.getAttribute('aria-checked'),'false');saveFailures=false;await a.page.getByRole('button',{name:'关闭设置'}).click();records.push('failed save does not optimistically change keyboard mode');
  preferences.set(1,{enterSendEnabled:true,revision:state(1).revision+1});await b.page.waitForFunction(()=>document.querySelector('.composer-hint')?.textContent.includes('Enter 发送'),{timeout:20000});records.push('visible page detects remote changes through 15-second synchronization');
  await a.page.locator('.settings-button').click();await a.page.getByRole('button',{name:'关闭设置'}).click();await a.page.locator('.user-bubble').first().getByRole('button',{name:'编辑',exact:true}).click();await input.fill('　');before=turns;await input.press('Enter');await a.page.getByRole('alertdialog').waitFor();assert.equal(turns,before);await a.page.getByRole('button',{name:'确定',exact:true}).click();await input.fill('更新健康计划');await input.press('Enter');await a.page.getByText('更新这个问题？',{exact:true}).waitFor();assert.equal(turns,before);await a.page.locator('.confirm-dialog').getByRole('button',{name:'确认',exact:true}).click();await a.page.waitForFunction(()=>!document.querySelector('.chat-input-wrap textarea')?.disabled);assert.equal(turns,before+1);records.push('blank edited question warns first; nonblank Enter retains edit confirmation');
  offline=true;await a.page.locator('.conversation-panel').getByRole('button',{name:'刷新会话',exact:true}).click();await a.page.locator('.sync-notice').waitFor();await input.fill('离线草稿');before=turns;await input.press('Enter');assert.equal(turns,before);assert.equal(await input.inputValue(),'离线草稿');offline=false;await a.page.locator('.conversation-panel').getByRole('button',{name:'刷新会话',exact:true}).click();await a.page.waitForFunction(()=>!document.querySelector('.sync-notice'));records.push('offline Enter retains draft and creates no unsynchronized messages');
  for(const width of [320,390]){
   const mobile=await device(1,width);await mobile.page.locator('.settings-button').click();const dialog=mobile.page.getByRole('dialog');await dialog.waitFor();const box=await dialog.boundingBox();assert.ok(box.x>=0 && box.x+box.width<=width);await mobile.page.screenshot({path:path.join(root,'.validation/v023-settings-'+width+'.png'),animations:'disabled'});await mobile.page.getByRole('button',{name:'关闭设置'}).click();await mobile.input.fill('　');await mobile.input.press('Enter');const alert=mobile.page.getByRole('alertdialog');await alert.waitFor();const bounds=await alert.boundingBox();assert.ok(bounds.x>=0 && bounds.x+bounds.width<=width);await mobile.page.screenshot({path:path.join(root,'.validation/v023-blank-'+width+'.png'),animations:'disabled'});await mobile.context.close();
  }
  records.push('320 and 390 pixel mobile settings and blank warnings fit the viewport');
  prefFailures=true;const fail=await device(3);await fail.page.locator('.composer-hint').filter({hasText:'聊天设置加载失败'}).waitFor();await fail.input.fill('健康测试');before=turns;await fail.input.press('Enter');await fail.page.waitForFunction(()=>!document.querySelector('.chat-input-wrap textarea')?.disabled);assert.equal(turns,before+1);records.push('first preference load failure retains enabled Enter mode');prefFailures=false;
  fs.writeFileSync(path.join(root,'.validation/v023-browser-report.json'),JSON.stringify({passed:true,checks:records,turns,creates,nativeImeManualTest:'not performed; automated coverage uses composition flags'},null,2));console.log(JSON.stringify({passed:true,checks:records.length,turns,creates}));
 }catch(error){fs.writeFileSync(path.join(root,'.validation/v023-browser-report.json'),JSON.stringify({passed:false,checks:records,error:error.message},null,2));throw error}
 finally{if(browser)await browser.close();vite.kill();}
};run().catch(e=>{console.error(e.stack);process.exitCode=1});
