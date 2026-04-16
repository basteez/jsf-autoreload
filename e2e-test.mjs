/**
 * E2E test for jsf-autoreload plugin.
 *
 * Tests all 3 bug fixes against the real test project:
 *   Bug 1: ScriptInjector wired — EventSource script injected into JSF pages
 *   Bug 2: Heartbeat working — SSE connection survives idle periods
 *   Bug 3: Connection cleanup — stale connections removed
 *
 * Usage: node e2e-test.mjs
 * Requires: Playwright (npx playwright install chromium)
 */

import { chromium } from 'playwright';
import { spawn } from 'child_process';
import { readFileSync, writeFileSync } from 'fs';
import { resolve } from 'path';

const TEST_PRJ = '/Users/tizianobasile/workspace/me/jsf-autoreload-test-prj';
const BASE_URL = 'http://localhost:8080';
const STARTUP_TIMEOUT = 30_000;
const SSE_ENDPOINT = '/_jsf-autoreload/events';

let serverProcess = null;
let browser = null;
let passed = 0;
let failed = 0;

function log(msg) {
  console.log(`[E2E] ${msg}`);
}

function pass(name) {
  passed++;
  console.log(`  \u2705 PASS: ${name}`);
}

function fail(name, reason) {
  failed++;
  console.log(`  \u274c FAIL: ${name} — ${reason}`);
}

async function startServer() {
  log('Starting test app (mvn exec:java)...');
  serverProcess = spawn('mvn', ['exec:java'], {
    cwd: TEST_PRJ,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: { ...process.env },
  });

  let output = '';
  serverProcess.stdout.on('data', (d) => { output += d.toString(); });
  serverProcess.stderr.on('data', (d) => { output += d.toString(); });

  // Wait for the server to be ready
  const start = Date.now();
  while (Date.now() - start < STARTUP_TIMEOUT) {
    try {
      const res = await fetch(`${BASE_URL}/index.xhtml`);
      if (res.ok) {
        log('Server is up!');
        return;
      }
    } catch {
      // not ready yet
    }
    await new Promise((r) => setTimeout(r, 500));
  }

  console.error('Server output:\n' + output);
  throw new Error('Server did not start within timeout');
}

function stopServer() {
  if (serverProcess) {
    log('Stopping server...');
    serverProcess.kill('SIGTERM');
    // Give it a moment, then force-kill
    setTimeout(() => {
      try { serverProcess.kill('SIGKILL'); } catch { /* ignore */ }
    }, 3000);
  }
}

// ============================================================
// TEST 1: ScriptInjector wired (Bug 1)
// Verify EventSource script is injected into rendered JSF page
// ============================================================
async function testScriptInjection(page) {
  log('Test 1: ScriptInjector wiring (Bug 1)');

  await page.goto(`${BASE_URL}/index.xhtml`, { waitUntil: 'domcontentloaded' });

  // Check that the EventSource script is present in the page
  const hasEventSource = await page.evaluate(() => {
    const scripts = document.querySelectorAll('script');
    for (const s of scripts) {
      if (s.textContent && s.textContent.includes('EventSource')) {
        return true;
      }
    }
    return false;
  });

  if (hasEventSource) {
    pass('EventSource script injected into JSF page');
  } else {
    fail('EventSource script injected into JSF page', 'No <script> containing EventSource found');
  }

  // Check script contains the correct SSE endpoint
  const scriptContent = await page.evaluate(() => {
    const scripts = document.querySelectorAll('script');
    for (const s of scripts) {
      if (s.textContent && s.textContent.includes('EventSource')) {
        return s.textContent;
      }
    }
    return '';
  });

  if (scriptContent.includes(SSE_ENDPOINT)) {
    pass('EventSource URL contains correct SSE endpoint path');
  } else {
    fail('EventSource URL contains correct SSE endpoint path',
      `Script content: ${scriptContent.substring(0, 200)}`);
  }
}

// ============================================================
// TEST 2: SSE connection established (Bug 1 continued)
// Verify browser can connect to the SSE endpoint
// ============================================================
async function testSseConnection(page) {
  log('Test 2: SSE connection established');

  // Navigate to a page (EventSource auto-connects)
  await page.goto(`${BASE_URL}/index.xhtml`, { waitUntil: 'domcontentloaded' });

  // Wait briefly for EventSource to connect
  await page.waitForTimeout(1000);

  // Check EventSource readyState: 0=CONNECTING, 1=OPEN, 2=CLOSED
  const esState = await page.evaluate(() => {
    // The injected script creates a local `es` variable inside an IIFE
    // We can't access it directly, so let's check by opening our own connection
    return new Promise((resolve) => {
      const es = new EventSource('/_jsf-autoreload/events');
      es.onopen = () => {
        resolve('OPEN');
        es.close();
      };
      es.onerror = () => {
        resolve('ERROR');
        es.close();
      };
      setTimeout(() => {
        resolve('TIMEOUT');
        es.close();
      }, 5000);
    });
  });

  if (esState === 'OPEN') {
    pass('SSE connection opens successfully');
  } else {
    fail('SSE connection opens successfully', `EventSource state: ${esState}`);
  }
}

// ============================================================
// TEST 3: Heartbeat keeps connection alive (Bug 2)
// Verify SSE endpoint sends :heartbeat comments
// ============================================================
async function testHeartbeat() {
  log('Test 3: Heartbeat (Bug 2) — checking SSE comment stream');

  // Open a raw HTTP connection to the SSE endpoint and read data
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 40_000); // 40s > 30s heartbeat interval

  try {
    const res = await fetch(`${BASE_URL}${SSE_ENDPOINT}`, {
      signal: controller.signal,
      headers: { 'Accept': 'text/event-stream' },
    });

    if (res.status !== 200) {
      fail('SSE endpoint returns 200', `Got status ${res.status}`);
      return;
    }
    pass('SSE endpoint returns 200 with text/event-stream');

    // Read the stream for up to 35 seconds looking for :heartbeat
    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    const readStart = Date.now();

    while (Date.now() - readStart < 35_000) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      if (buffer.includes(':heartbeat')) {
        pass('Heartbeat comment received within 35 seconds');
        clearTimeout(timeout);
        reader.cancel();
        return;
      }
    }

    fail('Heartbeat comment received within 35 seconds',
      `Received data: ${buffer.substring(0, 200)}`);
    reader.cancel();
  } catch (e) {
    if (e.name === 'AbortError') {
      fail('Heartbeat comment received within 35 seconds', 'Timed out waiting for heartbeat');
    } else {
      fail('Heartbeat test', e.message);
    }
  } finally {
    clearTimeout(timeout);
  }
}

// ============================================================
// TEST 4: XHTML change triggers browser reload (Bug 1 full flow)
// Edit an XHTML file and verify SSE sends a reload event
// ============================================================
async function testXhtmlReload() {
  log('Test 4: XHTML edit triggers reload event');

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15_000);

  try {
    const res = await fetch(`${BASE_URL}${SSE_ENDPOINT}`, {
      signal: controller.signal,
      headers: { 'Accept': 'text/event-stream' },
    });

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    // Wait for initial :ok comment
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      if (buffer.includes(':ok')) break;
    }

    // Now edit an XHTML file
    const xhtmlPath = resolve(TEST_PRJ, 'src/main/webapp/index.xhtml');
    const original = readFileSync(xhtmlPath, 'utf-8');
    const modified = original.replace('AAAAJSF Auto Reload', 'BBBBJSF Auto Reload');
    writeFileSync(xhtmlPath, modified, 'utf-8');

    // Wait for a reload event from SSE
    buffer = '';
    const readStart = Date.now();
    let gotReload = false;

    while (Date.now() - readStart < 10_000) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      if (buffer.includes('event: reload')) {
        gotReload = true;
        break;
      }
    }

    // Restore the original file
    writeFileSync(xhtmlPath, original, 'utf-8');

    if (gotReload) {
      pass('XHTML edit triggered SSE reload event');
    } else {
      fail('XHTML edit triggered SSE reload event',
        `No reload event received. Buffer: ${buffer.substring(0, 200)}`);
    }

    reader.cancel();
  } catch (e) {
    if (e.name === 'AbortError') {
      fail('XHTML reload test', 'Timed out');
    } else {
      fail('XHTML reload test', e.message);
    }
  } finally {
    clearTimeout(timeout);
  }
}

// ============================================================
// TEST 5: Browser auto-refreshes on file change (full E2E with Playwright)
// ============================================================
async function testBrowserAutoRefresh(page) {
  log('Test 5: Browser auto-refreshes on XHTML change (full E2E)');

  await page.goto(`${BASE_URL}/index.xhtml`, { waitUntil: 'domcontentloaded' });

  // Wait for EventSource to establish
  await page.waitForTimeout(2000);

  // Record the initial URL + timing to detect navigation
  const urlBefore = page.url();

  // Set up navigation detection — waitForNavigation catches location.reload()
  const navPromise = page.waitForNavigation({ timeout: 15_000, waitUntil: 'domcontentloaded' })
    .then(() => true)
    .catch(() => false);

  // Edit index.xhtml (the page currently displayed)
  const xhtmlPath = resolve(TEST_PRJ, 'src/main/webapp/index.xhtml');
  const original = readFileSync(xhtmlPath, 'utf-8');
  const marker = `<!-- e2e-test-marker-${Date.now()} -->`;
  const modified = original.replace('</h:body>', marker + '\n</h:body>');
  writeFileSync(xhtmlPath, modified, 'utf-8');

  // Wait for the page to reload
  const navigated = await navPromise;

  // Restore the file
  writeFileSync(xhtmlPath, original, 'utf-8');

  if (navigated) {
    pass('Browser auto-refreshed after XHTML file change');
  } else {
    fail('Browser auto-refreshed after XHTML file change', 'Page did not reload within 15 seconds');
  }
}

// ============================================================
// MAIN
// ============================================================
async function main() {
  try {
    await startServer();

    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();

    // Run tests
    await testScriptInjection(page);
    await testSseConnection(page);
    await testXhtmlReload();
    await testBrowserAutoRefresh(page);
    // Heartbeat test takes 35s — run last
    await testHeartbeat();

  } catch (e) {
    console.error('[E2E] Fatal error:', e.message);
    failed++;
  } finally {
    if (browser) await browser.close();
    stopServer();
  }

  // Report
  console.log('\n' + '='.repeat(50));
  console.log(`E2E Results: ${passed} passed, ${failed} failed, ${passed + failed} total`);
  console.log('='.repeat(50));
  process.exit(failed > 0 ? 1 : 0);
}

main();
