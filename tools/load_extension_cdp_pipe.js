const { spawn } = require('node:child_process');
const fs = require('node:fs');

function arg(name, fallback = undefined) {
  const i = process.argv.indexOf(name);
  return i >= 0 ? process.argv[i + 1] : fallback;
}

function required(name) {
  const value = arg(name);
  if (!value) throw new Error(`Missing ${name}`);
  return value;
}

const chrome = required('--chrome');
const userDataDir = required('--user-data-dir');
const extension = required('--extension');
const resultPath = arg('--result', 'cdp-extension-result.json');
const timeoutMs = Number(arg('--timeout-ms', '30000'));
const remoteDebuggingPort = arg('--remote-debugging-port');
const keepMs = Number(arg('--keep-ms', '0'));
const phoneUrl = arg('--phone-url');
const openUrl = arg('--open-url');
const commandName = arg('--command');
const overrideHost = arg('--override-host');
const requestPairing = process.argv.includes('--request-pairing');
const pairingWaitMs = Number(arg('--pairing-wait-ms', '120000'));

fs.mkdirSync(userDataDir, { recursive: true });

const child = spawn(chrome, [
  `--user-data-dir=${userDataDir}`,
  '--remote-debugging-pipe',
  '--enable-unsafe-extension-debugging',
  ...(remoteDebuggingPort ? [`--remote-debugging-port=${remoteDebuggingPort}`] : []),
  '--no-first-run',
  '--no-default-browser-check',
  '--disable-sync',
  '--disable-gpu',
], {
  stdio: ['ignore', 'ignore', 'pipe', 'pipe', 'pipe'],
  windowsHide: true,
});

let stderr = '';
child.stdio[2].on('data', (chunk) => {
  stderr += chunk.toString();
  if (stderr.length > 20000) stderr = stderr.slice(-20000);
});

const writePipe = child.stdio[3];
const readPipe = child.stdio[4];
let buffer = '';
let nextId = 1;
const pending = new Map();

readPipe.on('data', (chunk) => {
  buffer += chunk.toString('utf8');
  let separator;
  while ((separator = buffer.indexOf('\0')) >= 0) {
    const payload = buffer.slice(0, separator);
    buffer = buffer.slice(separator + 1);
    let message;
    try {
      message = JSON.parse(payload);
    } catch (error) {
      fail(error);
      return;
    }
    if (message.id && pending.has(message.id)) {
      const { resolve, reject } = pending.get(message.id);
      pending.delete(message.id);
      if (message.error) reject(new Error(JSON.stringify(message.error)));
      else resolve(message.result);
    }
  }
});

function send(method, params = {}, sessionId = undefined) {
  return new Promise((resolve, reject) => {
    const id = nextId++;
    pending.set(id, { resolve, reject });
    writePipe.write(JSON.stringify({ id, method, params, ...(sessionId ? { sessionId } : {}) }) + '\0');
  });
}

function fail(error) {
  const message = error instanceof Error ? error.message : String(error);
  const output = { ok: false, error: message, stderr: stderr.slice(-12000) };
  fs.writeFileSync(resultPath, JSON.stringify(output, null, 2));
  try { child.kill(); } catch {}
  console.error(JSON.stringify(output));
  process.exitCode = 1;
}

const effectiveTimeoutMs = Math.max(timeoutMs, pairingWaitMs + keepMs + 10000);
const timer = setTimeout(() => fail(new Error(`Timed out after ${effectiveTimeoutMs}ms`)), effectiveTimeoutMs);

(async () => {
  try {
    const loaded = await send('Extensions.loadUnpacked', { path: extension });
    const targets = await send('Target.getTargets');
    const extensionTargets = targets.targetInfos.filter((target) =>
      target.url.startsWith(`chrome-extension://${loaded.id}/`));
    let popupProbe;
    let popupEvalResponse;
    let commandProbe;
    let openedTarget;
    if (openUrl) {
      openedTarget = await send('Target.createTarget', { url: openUrl });
    }
    if (phoneUrl) {
      const popup = await send('Target.createTarget', {
        url: `chrome-extension://${loaded.id}/popup.html`,
      });
      await new Promise((resolve) => setTimeout(resolve, 2000));
      const attached = await send('Target.attachToTarget', {
        targetId: popup.targetId,
        flatten: true,
      });
      const checkUrl = `${phoneUrl.replace(/\/$/, '')}/device-info`;
      const expression = `(async()=>{await new Promise(r=>setTimeout(r,1500));const input=document.querySelector('#pairing-host');if(!input)return {checkUrl:${JSON.stringify(checkUrl)},url:location.href,title:document.title,body:(document.body?.innerText||'').slice(0,500),domReady:Boolean(document.documentElement)};const host=${JSON.stringify(new URL(phoneUrl).hostname)};const port=${JSON.stringify(new URL(phoneUrl).port || '8888')};input.value=host;input.dispatchEvent(new Event('input',{bubbles:true}));const portInput=document.querySelector('#pairing-port');portInput.value=port;portInput.dispatchEvent(new Event('input',{bubbles:true}));document.querySelector('#check-phone').click();await new Promise(r=>setTimeout(r,1200));if(${requestPairing ? 'true' : 'false'}){document.querySelector('#request-pairing').click();await new Promise(r=>setTimeout(r,1000));let storage={};const deadline=Date.now()+${pairingWaitMs};while(Date.now()<deadline){storage=await new Promise(resolve=>chrome.storage.local.get(['devices','selectedDeviceId'],resolve));const matched=Array.isArray(storage.devices)&&storage.devices.some(d=>d.host===host&&String(d.port||'')===port&&Boolean(d.token));const status=document.querySelector('#pairing-status')?.textContent||'';if(matched||/accepted|成功|已配对/i.test(status))break;await new Promise(r=>setTimeout(r,1000));}}const storage=await new Promise(resolve=>chrome.storage.local.get(['devices','selectedDeviceId'],resolve));const devices=Array.isArray(storage.devices)?storage.devices:[];return {checkUrl:${JSON.stringify(checkUrl)},url:location.href,result:document.querySelector('#pairing-status')?.textContent||'',pageResult:document.querySelector('#result')?.textContent||'',title:document.title,pairedDeviceCount:devices.length,selectedDeviceId:storage.selectedDeviceId||'',targetPaired:devices.some(d=>d.host===host&&String(d.port||'')===port&&Boolean(d.token))};})()`;
      const evaluated = await send('Runtime.evaluate', {
        expression,
        awaitPromise: true,
        returnByValue: true,
      }, attached.sessionId);
      popupEvalResponse = evaluated;
      popupProbe = evaluated.result?.value ?? evaluated.result;
      if (overrideHost || commandName) {
        const commandExpression = `(async()=>{let storage=await new Promise(resolve=>chrome.storage.local.get(['devices','selectedDeviceId'],resolve));const selectedId=storage.selectedDeviceId||(Array.isArray(storage.devices)&&storage.devices[0]?.id)||'';if(${JSON.stringify(overrideHost || '')}){const nextDevices=(Array.isArray(storage.devices)?storage.devices:[]).map(d=>d.id===selectedId?{...d,host:${JSON.stringify(overrideHost || '')}}:d);await new Promise(resolve=>chrome.storage.local.set({devices:nextDevices},resolve));await new Promise(r=>setTimeout(r,100));}if(${JSON.stringify(commandName || '')}){await window.handleButtonClick(${JSON.stringify(commandName || '')});await new Promise(r=>setTimeout(r,1800));}const after=await new Promise(resolve=>chrome.storage.local.get(['devices','selectedDeviceId'],resolve));const selected=Array.isArray(after.devices)?after.devices.find(d=>d.id===(after.selectedDeviceId||selectedId)):null;return {overrideHost:${JSON.stringify(overrideHost || '')},command:${JSON.stringify(commandName || '')},result:document.querySelector('#result')?.textContent||'',selectedDeviceId:after.selectedDeviceId||selectedId,selectedHost:selected?.host||'',lastSuccessAt:selected?.lastSuccessAt||''};})()`;
        const commandEvaluated = await send('Runtime.evaluate', {
          expression: commandExpression,
          awaitPromise: true,
          returnByValue: true,
        }, attached.sessionId);
        commandProbe = commandEvaluated.result?.value ?? commandEvaluated.result;
      }
    }
    const output = {
      ok: true,
      extensionId: loaded.id,
      extension,
      extensionTargets,
      openedTarget,
      popupProbe,
      popupEvalResponse,
      commandProbe,
      stderr: stderr.slice(-12000),
    };
    fs.writeFileSync(resultPath, JSON.stringify(output, null, 2));
    console.log(JSON.stringify(output));
    if (keepMs > 0) await new Promise((resolve) => setTimeout(resolve, keepMs));
    clearTimeout(timer);
    child.kill();
  } catch (error) {
    clearTimeout(timer);
    fail(error);
  }
})();

child.on('error', (error) => {
  clearTimeout(timer);
  fail(error);
});
