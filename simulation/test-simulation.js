/**
 * Arena.ai Simulation Test Suite
 *
 * Runs a comprehensive test against the simulation server to verify:
 *   1. Login flow (sign-up, sign-in, sign-out)
 *   2. History loading
 *   3. Modality auto-detection
 *   4. Chat streaming (battle mode, direct mode, agent mode)
 *   5. Image generation modality
 *   6. WebDev modality
 *   7. Voting
 *   8. Conversation navigation
 *   9. Stop streaming
 *  10. Error handling
 *
 * Run with: node simulation/test-simulation.js
 *
 * The test suite starts the server itself, runs all tests, then shuts down.
 */
const http = require('http');
const { spawn } = require('child_process');

const PORT = 8788;  // Use a different port for tests
const BASE = `http://localhost:${PORT}`;

let serverProcess;
let testsPassed = 0;
let testsFailed = 0;
const testResults = [];

async function startServer() {
  return new Promise((resolve, reject) => {
    serverProcess = spawn('node', ['simulation/server.js'], {
      cwd: '/home/z/my-project/arena0077',
      env: { ...process.env, PORT: PORT.toString() },
      stdio: ['ignore', 'pipe', 'pipe']
    });

    serverProcess.stdout.on('data', (data) => {
      const msg = data.toString();
      if (msg.includes('Listening')) {
        resolve();
      }
    });

    serverProcess.stderr.on('data', (data) => {
      console.error('Server error:', data.toString());
    });

    setTimeout(() => reject(new Error('Server failed to start')), 5000);
  });
}

function stopServer() {
  if (serverProcess) {
    serverProcess.kill('SIGTERM');
  }
}

// ============================ HTTP helpers ============================

function request(method, path, body = null, headers = {}) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const req = http.request(`${BASE}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers,
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {})
      }
    }, (res) => {
      let body = '';
      res.on('data', c => body += c);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, headers: res.headers, body: JSON.parse(body) });
        } catch {
          resolve({ status: res.statusCode, headers: res.headers, body });
        }
      });
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

function streamRequest(method, path, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const req = http.request(`${BASE}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers,
        'Content-Length': Buffer.byteLength(data)
      }
    }, (res) => {
      const events = [];
      let buffer = '';

      res.on('data', (chunk) => {
        buffer += chunk.toString();
        const lines = buffer.split('\n');
        buffer = lines.pop();
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim();
            if (data === '[DONE]') continue;
            try { events.push(JSON.parse(data)); }
            catch { /* skip non-JSON */ }
          }
        }
      });

      res.on('end', () => resolve({ status: res.statusCode, events }));
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

// ============================ Test framework ============================

async function test(name, fn) {
  try {
    await fn();
    testsPassed++;
    testResults.push({ name, status: 'PASS' });
    console.log(`  ✓ ${name}`);
  } catch (err) {
    testsFailed++;
    testResults.push({ name, status: 'FAIL', error: err.message });
    console.log(`  ✗ ${name}`);
    console.log(`    Error: ${err.message}`);
  }
}

function assert(condition, message) {
  if (!condition) throw new Error(message || 'Assertion failed');
}

function assertEqual(a, b, message) {
  if (a !== b) throw new Error(`${message || 'Not equal'}: ${a} !== ${b}`);
}

// ============================ Tests ============================

let authToken = null;
let conversationId = null;

async function testSignUp() {
  const res = await request('POST', '/nextjs-api/sign-up', {
    recaptchaToken: '',
    provisionalUserId: 'test-001'
  });
  assertEqual(res.status, 200, 'Sign-up should return 200');
  assert(res.body.id, 'User should have id');
  assert(res.body.is_anonymous, 'Should be anonymous');
}

async function testSignIn() {
  const res = await request('POST', '/nextjs-api/sign-in/email', {
    email: 'Ai9900@bjedu.tech',
    password: 'Ai9900@bjedu.tech'
  });
  assertEqual(res.status, 200, 'Sign-in should return 200');
  assertEqual(res.body.email, 'Ai9900@bjedu.tech', 'Email should match');

  // Extract token from Set-Cookie
  const cookies = res.headers['set-cookie'] || [];
  const authCookie = cookies.find(c => c.startsWith('arena-auth-prod-v1='));
  assert(authCookie, 'Should set arena-auth-prod-v1 cookie');

  // Parse the token
  const match = authCookie.match(/base64-([^;]+)/);
  if (match) {
    const decoded = JSON.parse(Buffer.from(match[1], 'base64').toString());
    authToken = decoded.access_token;
  }
  assert(authToken, 'Should extract auth token');
}

async function testGetMe() {
  const res = await request('GET', '/api/me', null, {
    Authorization: `Bearer ${authToken}`
  });
  assertEqual(res.status, 200, 'Get me should return 200');
  assertEqual(res.body.email, 'Ai9900@bjedu.tech', 'Email should match');
}

async function testHistory() {
  const res = await request('GET', '/api/history/unified?limit=20&includeArchived=false', null, {
    Authorization: `Bearer ${authToken}`
  });
  assertEqual(res.status, 200, 'History should return 200');
  assert(res.body.items && res.body.items.length > 0, 'Should have history items');
  assert(res.body.items.length <= 20, 'Should respect limit');
  console.log(`    Found ${res.body.items.length} conversations`);
}

async function testAutoModality() {
  const tests = [
    { prompt: 'What is the weather?', expected: 'chat' },
    { prompt: 'Generate an image of a cat', expected: 'image' },
    { prompt: 'Create a landing page', expected: 'webdev' }
  ];

  for (const t of tests) {
    const res = await request('POST', '/nextjs-api/auto-modality', { prompt: t.prompt });
    assertEqual(res.status, 200, `Auto-modality should return 200 for "${t.prompt}"`);
    assertEqual(res.body.modality, t.expected, `Modality for "${t.prompt}"`);
  }
}

async function testCreateEvaluationBattle() {
  const res = await streamRequest('POST', '/nextjs-api/stream/create-evaluation', {
    prompt: 'Hello, what is 2+2?',
    modality: 'chat',
    mode: 'battle'
  });

  assertEqual(res.status, 200, 'Create evaluation should return 200');
  assert(res.events.length > 0, 'Should receive stream events');

  // Find conversation_created event
  const convEvent = res.events.find(e => e.type === 'conversation_created');
  assert(convEvent, 'Should have conversation_created event');
  conversationId = convEvent.id;
  assert(conversationId, 'Should have conversation id');

  // Should have message_start events for both models
  const startEvents = res.events.filter(e => e.type === 'message_start');
  assertEqual(startEvents.length, 2, 'Battle mode should start 2 model messages');

  // Should have text_delta events
  const deltas = res.events.filter(e => e.type === 'text_delta');
  assert(deltas.length > 0, 'Should have text_delta events');

  // Should have message_complete events
  const completeEvents = res.events.filter(e => e.type === 'message_complete');
  assertEqual(completeEvents.length, 2, 'Both models should complete');

  // Check that we got actual content
  const modelAResponse = completeEvents.find(e => e.modelLabel === 'A')?.finalContent;
  const modelBResponse = completeEvents.find(e => e.modelLabel === 'B')?.finalContent;
  assert(modelAResponse && modelAResponse.length > 0, 'Model A should have content');
  assert(modelBResponse && modelBResponse.length > 0, 'Model B should have content');

  console.log(`    Model A: "${modelAResponse}"`);
  console.log(`    Model B: "${modelBResponse}"`);
  console.log(`    Conversation ID: ${conversationId}`);
}

async function testPostToEvaluation() {
  const res = await streamRequest(
    'POST',
    `/nextjs-api/stream/post-to-evaluation/${conversationId}`,
    { prompt: 'What about 3+3?', modality: 'chat' }
  );

  assertEqual(res.status, 200, 'Post to evaluation should return 200');
  const completeEvents = res.events.filter(e => e.type === 'message_complete');
  assertEqual(completeEvents.length, 2, 'Should get 2 responses in battle mode');
}

async function testDirectMode() {
  const res = await streamRequest('POST', '/nextjs-api/stream/create-evaluation', {
    prompt: 'Hello',
    modality: 'chat',
    mode: 'direct'
  });

  assertEqual(res.status, 200, 'Direct mode should return 200');
  const startEvents = res.events.filter(e => e.type === 'message_start');
  assertEqual(startEvents.length, 1, 'Direct mode should have only 1 model');
}

async function testImageModality() {
  const res = await streamRequest('POST', '/nextjs-api/stream/create-evaluation', {
    prompt: 'Generate an image of a sunset over mountains',
    modality: 'image',
    mode: 'battle'
  });

  assertEqual(res.status, 200, 'Image modality should return 200');
  const imgEvent = res.events.find(e => e.type === 'image_generated');
  assert(imgEvent, 'Should have image_generated event');
  assert(imgEvent.imageUrl, 'Should have image URL');
  console.log(`    Image URL: ${imgEvent.imageUrl}`);
}

async function testWebDevModality() {
  const res = await streamRequest('POST', '/nextjs-api/stream/create-evaluation', {
    prompt: 'Create a sleek, modern landing page for a SaaS product',
    modality: 'webdev',
    mode: 'battle'
  });

  assertEqual(res.status, 200, 'WebDev modality should return 200');
  const previewEvent = res.events.find(e => e.type === 'webdev_preview');
  assert(previewEvent, 'Should have webdev_preview event');
  assert(previewEvent.previewUrl, 'Should have preview URL');
  console.log(`    Preview URL: ${previewEvent.previewUrl}`);
}

async function testAgentMode() {
  const res = await streamRequest('POST', '/nextjs-api/stream/create-evaluation', {
    prompt: 'Research the latest AI news and summarize',
    modality: 'chat',
    mode: 'agent'
  });

  assertEqual(res.status, 200, 'Agent mode should return 200');
  const agentSteps = res.events.filter(e => e.type === 'agent_step');
  assert(agentSteps.length > 0, 'Should have agent_step events');
  console.log(`    Agent executed ${agentSteps.length} steps:`);
  agentSteps.forEach(s => console.log(`      ${s.stepNumber}. ${s.action} → ${s.result}`));
}

async function testStopStreaming() {
  // First create a conversation to get IDs
  const createRes = await streamRequest('POST', '/nextjs-api/stream/create-evaluation', {
    prompt: 'Tell me a long story',
    modality: 'chat',
    mode: 'battle'
  });
  const convId = createRes.events.find(e => e.type === 'conversation_created')?.id;
  const msgId = createRes.events.find(e => e.type === 'message_start')?.messageId;

  const res = await request(
    'POST',
    `/nextjs-api/stream/stop/${convId}/messages/${msgId}`,
    { stoppedAt: new Date().toISOString() }
  );
  assertEqual(res.status, 200, 'Stop should return 200');
  assertEqual(res.body.ok, true, 'Stop should return ok');
}

async function testVote() {
  const res = await request('POST', '/api/vote', {
    value: 'model_a',
    messageAId: 'msg-a-001',
    messageBId: 'msg-b-001',
    evaluationSessionId: conversationId
  });
  assertEqual(res.status, 200, 'Vote should return 200');
  assertEqual(res.body.value, 'model_a', 'Vote value should match');
}

async function testRerun() {
  const res = await streamRequest(
    'POST',
    `/nextjs-api/stream/rerun/550e8400-e29b-41d4-a716-446655440000`,
    { prompt: 'rerun' }
  );
  assertEqual(res.status, 200, 'Rerun should return 200');
}

async function testSignOut() {
  const res = await request('POST', '/nextjs-api/sign-out', {}, {
    Authorization: `Bearer ${authToken}`
  });
  assertEqual(res.status, 200, 'Sign-out should return 200');
}

async function testUnauthorizedAccess() {
  const res = await request('GET', '/api/me');
  assertEqual(res.status, 401, 'Should return 401 without auth');
}

async function testLeaderboard() {
  const res = await request('GET', '/api/leaderboard?category=overall');
  assertEqual(res.status, 200, 'Leaderboard should return 200');
  assert(res.body.entries && res.body.entries.length > 0, 'Should have leaderboard entries');
  console.log(`    Top model: ${res.body.entries[0].model.name} (${res.body.entries[0].arenaScore})`);
}

// ============================ Main ============================

async function main() {
  console.log('\n╔══════════════════════════════════════════════════════════╗');
  console.log('║   Arena0077 Simulation Test Suite                        ║');
  console.log('╚══════════════════════════════════════════════════════════╝\n');

  console.log('Starting simulation server...');
  await startServer();
  console.log('Server started on port', PORT);
  await new Promise(r => setTimeout(r, 500));

  console.log('\n--- Auth Tests ---');
  await test('Sign-up (anonymous user creation)', testSignUp);
  await test('Sign-in with email + password', testSignIn);
  await test('Get current user (/api/me)', testGetMe);
  await test('Unauthorized access returns 401', testUnauthorizedAccess);

  console.log('\n--- History Tests ---');
  await test('Load conversation history', testHistory);

  console.log('\n--- Modality Detection ---');
  await test('Auto-modality detection', testAutoModality);

  console.log('\n--- Chat Streaming Tests ---');
  await test('Create evaluation (battle mode)', testCreateEvaluationBattle);
  await test('Post to evaluation (followup)', testPostToEvaluation);
  await test('Direct chat mode (single model)', testDirectMode);
  await test('Stop streaming', testStopStreaming);
  await test('Rerun message', testRerun);

  console.log('\n--- Special Modality Tests ---');
  await test('Image generation modality', testImageModality);
  await test('WebDev modality (Design to Code)', testWebDevModality);
  await test('Agent mode (multi-step)', testAgentMode);

  console.log('\n--- Voting ---');
  await test('Vote for model', testVote);

  console.log('\n--- Leaderboard ---');
  await test('Load leaderboard', testLeaderboard);

  console.log('\n--- Cleanup ---');
  await test('Sign out', testSignOut);

  // Summary
  console.log('\n╔══════════════════════════════════════════════════════════╗');
  console.log(`║   Results: ${testsPassed} passed, ${testsFailed} failed                          ║`);
  console.log('╚══════════════════════════════════════════════════════════╝');

  if (testsFailed > 0) {
    console.log('\nFailed tests:');
    testResults.filter(r => r.status === 'FAIL').forEach(r => {
      console.log(`  - ${r.name}: ${r.error}`);
    });
  }

  stopServer();
  process.exit(testsFailed > 0 ? 1 : 0);
}

main().catch((err) => {
  console.error('Fatal error:', err);
  stopServer();
  process.exit(1);
});
