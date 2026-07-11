/**
 * Arena.ai Simulation Server
 *
 * A Node.js server that mocks the arena.ai API surface so we can test
 * the Android app's networking layer, WebView bridge, and event handling
 * WITHOUT needing the real arena.ai backend.
 *
 * The simulator implements:
 *   - POST /nextjs-api/sign-up            (anonymous user creation)
 *   - POST /nextjs-api/sign-in/email       (email + password login)
 *   - POST /nextjs-api/sign-out
 *   - GET  /api/me                         (current user)
 *   - GET  /api/history/unified            (conversation list)
 *   - POST /nextjs-api/auto-modality       (modality auto-detection)
 *   - POST /nextjs-api/stream/create-evaluation  (NEW chat - SSE stream)
 *   - POST /nextjs-api/stream/post-to-evaluation/:id  (followup - SSE stream)
 *   - POST /nextjs-api/stream/stop/:id/messages/:msgId  (stop)
 *   - POST /api/vote                       (vote for model)
 *   - GET  /leaderboard                    (HTML leaderboard)
 *
 * Run with: node simulation/server.js
 *   Default port: 8787
 *
 * The Android app can be pointed at this server by overriding
 * BuildConfig.ARENA_BASE_URL in debug builds.
 */
const http = require('http');
const crypto = require('crypto');
const url = require('url');

const PORT = process.env.PORT || 8787;

// ============================ In-memory state ============================

const users = new Map();        // email -> user
const sessions = new Map();     // sessionToken -> user
const conversations = new Map(); // id -> conversation
const messages = new Map();     // conversationId -> Message[]
const votes = new Map();        // conversationId -> vote

// Pre-seed a test user matching the real arena.ai credentials
const TEST_USER = {
  id: 'cb3f7163-622e-4f8a-b9c3-84f0d6acc976',
  aud: 'authenticated',
  role: 'authenticated',
  email: 'Ai9900@bjedu.tech',
  email_confirmed_at: '2026-01-19T09:24:11.978647Z',
  confirmed_at: '2026-01-19T09:24:11.978647Z',
  last_sign_in_at: new Date().toISOString(),
  app_metadata: { provider: 'email', providers: ['email'] },
  user_metadata: {
    domain_url: 'https://lmarena.ai',
    email: 'Ai9900@bjedu.tech',
    email_verified: true,
    full_name: 'A',
    id: '019bd58f-849d-75c2-a614-10b608b7d4b5',
    phone_verified: false,
    should_link_history: true,
    sub: 'cb3f7163-622e-4f8a-b9c3-84f0d6acc976'
  },
  is_anonymous: false,
  created_at: '2026-01-19T09:22:49.666901Z',
  updated_at: new Date().toISOString()
};
users.set(TEST_USER.email, { ...TEST_USER, password: 'Ai9900@bjedu.tech' });

// Pre-seed a few conversations
[
  { id: '019f4e80-4e12-74a8-87d3-5ff45ffecdd1', title: 'What is 2+2?', modality: 'chat', mode: 'battle', created_at: '2026-07-10T22:00:00Z' },
  { id: '019f4e80-50a1-74a8-87d3-5ff45ffecdd2', title: 'Create a landing page', modality: 'webdev', mode: 'battle', created_at: '2026-07-10T21:30:00Z' },
  { id: '019f4e80-58c0-74a8-87d3-5ff45ffecdd3', title: 'Generate a 1m wide image', modality: 'image', mode: 'battle', created_at: '2026-07-10T20:15:00Z' },
  { id: '019f4e80-60d0-74a8-87d3-5ff45ffecdd4', title: 'Build a dashboard', modality: 'webdev', mode: 'battle', created_at: '2026-07-10T19:45:00Z' },
  { id: '019f4e80-68e0-74a8-87d3-5ff45ffecdd5', title: 'Talk like a doctor', modality: 'chat', mode: 'direct', created_at: '2026-07-10T18:20:00Z' },
  { id: '019f4e80-70f0-74a8-87d3-5ff45ffecdd6', title: 'Convert MHTML to PDF', modality: 'chat', mode: 'direct', created_at: '2026-07-10T17:10:00Z' }
].forEach(c => {
  conversations.set(c.id, c);
  messages.set(c.id, []);
});

// ============================ Helpers ============================

function uuid() {
  return crypto.randomUUID();
}

function cors(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Cookie, X-Client-Platform, X-Client-Version');
  res.setHeader('Access-Control-Allow-Credentials', 'true');
}

function readBody(req) {
  return new Promise((resolve) => {
    let data = '';
    req.on('data', chunk => data += chunk);
    req.on('end', () => {
      try { resolve(JSON.parse(data || '{}')); }
      catch { resolve({}); }
    });
  });
}

function getSessionFromReq(req) {
  const auth = req.headers['authorization'] || '';
  const token = auth.replace('Bearer ', '');
  return sessions.get(token);
}

function json(res, code, body) {
  res.writeHead(code, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
}

// ============================ Stream helpers ============================

/**
 * Emit an SSE event to the response.
 */
function sse(res, event) {
  res.write(`data: ${JSON.stringify(event)}\n\n`);
}

/**
 * Simulate a streaming chat response.
 * Sends:
 *   1. conversation_created event
 *   2. message_start (for Model A and Model B in battle mode)
 *   3. text_delta events (chunked)
 *   4. message_complete for each model
 */
async function streamChat(req, res, body) {
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });

  const prompt = body.prompt || 'Hello';
  const modality = body.modality || 'chat';
  const mode = body.mode || 'battle';
  const conversationId = body.conversationId || uuid();

  // 1. Conversation created
  sse(res, {
    type: 'conversation_created',
    id: conversationId,
    modality, mode,
    title: prompt.slice(0, 50)
  });

  // 2. For battle mode, send two model responses
  const models = mode === 'battle'
    ? [
        { label: 'A', name: 'GPT-5', org: 'OpenAI', response: generateResponse(prompt, 'gpt') },
        { label: 'B', name: 'Claude 4.5', org: 'Anthropic', response: generateResponse(prompt, 'claude') }
      ]
    : [
        { label: 'A', name: 'Direct Model', org: 'arena.ai', response: generateResponse(prompt, 'direct') }
      ];

  for (const m of models) {
    const messageId = uuid();
    sse(res, { type: 'message_start', messageId, modelLabel: m.label, modelName: m.name, modelOrganization: m.org });

    // Stream the response in chunks
    const chunks = chunkText(m.response, 8);  // 8 chars per chunk
    for (const chunk of chunks) {
      await sleep(30);  // 30ms between chunks - realistic typing speed
      sse(res, { type: 'text_delta', messageId, delta: chunk, modelLabel: m.label });
    }

    sse(res, { type: 'message_complete', messageId, finalContent: m.response, modelLabel: m.label, modelName: m.name });
  }

  // 3. For image modality, emit image_generated
  if (modality === 'image') {
    const imgId = uuid();
    sse(res, {
      type: 'image_generated',
      messageId: imgId,
      imageUrl: `https://picsum.photos/seed/${encodeURIComponent(prompt)}/1024/1024`,
      modelLabel: 'A'
    });
  }

  // 4. For webdev modality, emit a preview URL
  if (modality === 'webdev') {
    const wdId = uuid();
    sse(res, {
      type: 'webdev_preview',
      messageId: wdId,
      previewUrl: `https://webdev-preview.arena.ai/${wdId}`,
      modelLabel: 'A'
    });
  }

  // 5. For agent mode, emit agent_step events
  if (mode === 'agent') {
    const agentSteps = [
      { action: 'Search web', result: 'Found 5 relevant pages' },
      { action: 'Read page 1', result: 'Extracted key information' },
      { action: 'Synthesize answer', result: 'Generated response' }
    ];
    for (let i = 0; i < agentSteps.length; i++) {
      const stepId = uuid();
      await sleep(500);
      sse(res, {
        type: 'agent_step',
        messageId: stepId,
        stepNumber: i + 1,
        action: agentSteps[i].action,
        result: agentSteps[i].result
      });
    }
  }

  res.write('data: [DONE]\n\n');
  res.end();
}

function generateResponse(prompt, modelType) {
  const p = prompt.toLowerCase();
  if (p.includes('2+2') || p.includes('2 plus 2')) {
    return modelType === 'gpt' ? '4' : '2 + 2 = 4';
  }
  if (p.includes('hello') || p.includes('hi')) {
    return modelType === 'gpt'
      ? 'Hello! How can I help you today?'
      : 'Hi there! What would you like to talk about?';
  }
  if (p.includes('landing page')) {
    return 'I would create a sleek, modern landing page with a hero section, features grid, and call-to-action buttons. The design would use a purple-to-blue gradient, Inter typography, and smooth scroll animations.';
  }
  if (p.includes('image') || p.includes('صورة')) {
    return 'I would generate an image based on your description. The image will be 1024x1024 by default, with options to upscale.';
  }
  // Default response
  return `This is a simulated response from ${modelType}. You said: "${prompt.slice(0, 100)}". In a real scenario, I would provide a thoughtful, detailed answer with context and examples.`;
}

function chunkText(text, size) {
  const chunks = [];
  for (let i = 0; i < text.length; i += size) {
    chunks.push(text.slice(i, i + size));
  }
  return chunks;
}

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

// ============================ HTTP Server ============================

const server = http.createServer(async (req, res) => {
  cors(res);
  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsed = url.parse(req.url, true);
  const path = parsed.pathname;
  const query = parsed.query;
  const body = ['POST', 'PUT', 'PATCH'].includes(req.method) ? await readBody(req) : {};

  console.log(`${new Date().toISOString()} ${req.method} ${path}`);

  try {
    // ============ Auth ============
    if (path === '/nextjs-api/sign-up' && req.method === 'POST') {
      const id = uuid();
      const user = {
        id, aud: 'authenticated', role: 'authenticated',
        email: `anonymous-${id.slice(0, 8)}@arena.ai`,
        is_anonymous: true,
        created_at: new Date().toISOString()
      };
      const token = `sim-token-${id}`;
      sessions.set(token, user);
      return json(res, 200, user);
    }

    if (path === '/nextjs-api/sign-in/email' && req.method === 'POST') {
      const { email, password } = body;
      const userRecord = users.get(email);
      if (!userRecord || userRecord.password !== password) {
        return json(res, 401, { error: 'Invalid credentials' });
      }
      const token = `sim-token-${userRecord.id}`;
      sessions.set(token, userRecord);
      res.setHeader('Set-Cookie', `arena-auth-prod-v1=base64-${Buffer.from(JSON.stringify({ access_token: token, user: userRecord })).toString('base64')}; Path=/; HttpOnly; SameSite=Lax`);
      return json(res, 200, userRecord);
    }

    if (path === '/nextjs-api/sign-out' && req.method === 'POST') {
      const auth = req.headers['authorization'] || '';
      sessions.delete(auth.replace('Bearer ', ''));
      return json(res, 200, { ok: true });
    }

    if (path === '/api/me' && req.method === 'GET') {
      const user = getSessionFromReq(req);
      if (!user) return json(res, 401, { error: 'Unauthorized' });
      return json(res, 200, user);
    }

    // ============ History ============
    if (path === '/api/history/unified' && req.method === 'GET') {
      const items = Array.from(conversations.values())
        .filter(c => query.includeArchived === 'true' || !c.archived_at)
        .sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
        .slice(0, parseInt(query.limit || 20))
        .map(c => ({
          id: c.id,
          title: c.title,
          modality: c.modality,
          mode: c.mode,
          createdAt: c.created_at,
          updatedAt: c.created_at,
          isArchived: false
        }));
      return json(res, 200, { items, hasMore: false });
    }

    // ============ Auto-modality ============
    if (path === '/nextjs-api/auto-modality' && req.method === 'POST') {
      const prompt = (body.prompt || '').toLowerCase();
      let modality = 'chat';
      if (prompt.includes('image') || prompt.includes('صورة')) modality = 'image';
      else if (prompt.includes('video') || prompt.includes('فيديو')) modality = 'video';
      else if (prompt.includes('landing') || prompt.includes('dashboard') || prompt.includes('game') || prompt.includes('app')) modality = 'webdev';
      return json(res, 200, { modality, confidence: 0.92 });
    }

    // ============ Chat streaming ============
    if (path === '/nextjs-api/stream/create-evaluation' && req.method === 'POST') {
      return streamChat(req, res, body);
    }

    const postMatch = path.match(/^\/nextjs-api\/stream\/post-to-evaluation\/([a-f0-9-]+)$/);
    if (postMatch && req.method === 'POST') {
      body.conversationId = postMatch[1];
      return streamChat(req, res, body);
    }

    const stopMatch = path.match(/^\/nextjs-api\/stream\/stop\/([a-f0-9-]+)\/messages\/([a-f0-9-]+)$/);
    if (stopMatch && req.method === 'POST') {
      return json(res, 200, { ok: true, stopped: true });
    }

    const rerunMatch = path.match(/^\/nextjs-api\/stream\/rerun\/([a-f0-9-]+)$/);
    if (rerunMatch && req.method === 'POST') {
      body.conversationId = rerunMatch[1];
      return streamChat(req, res, body);
    }

    const resampleMatch = path.match(/^\/nextjs-api\/stream\/resample\/([a-f0-9-]+)$/);
    if (resampleMatch && req.method === 'POST') {
      body.conversationId = resampleMatch[1];
      return streamChat(req, res, body);
    }

    // ============ Vote ============
    if (path === '/api/vote' && req.method === 'POST') {
      const { evaluationSessionId, value } = body;
      votes.set(evaluationSessionId, value);
      return json(res, 200, { ok: true, value });
    }

    // ============ Leaderboard ============
    if (path === '/api/leaderboard' && req.method === 'GET') {
      return json(res, 200, {
        category: query.category || 'overall',
        entries: [
          { rank: 1, model: { id: 'gpt5', name: 'GPT-5', organization: 'OpenAI' }, arenaScore: 1428, votes: 12400 },
          { rank: 2, model: { id: 'claude45', name: 'Claude 4.5 Sonnet', organization: 'Anthropic' }, arenaScore: 1415, votes: 11800 },
          { rank: 3, model: { id: 'gemini25pro', name: 'Gemini 2.5 Pro', organization: 'Google' }, arenaScore: 1407, votes: 10900 },
          { rank: 4, model: { id: 'llama4-70b', name: 'Llama 4 70B', organization: 'Meta' }, arenaScore: 1389, votes: 9800 }
        ]
      });
    }

    // ============ Homepage (so WebView can load) ============
    if (path === '/' && req.method === 'GET') {
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(`
<!DOCTYPE html>
<html>
<head>
  <title>Arena AI - Simulation</title>
  <style>
    body { background: #0a0a0f; color: white; font-family: sans-serif; padding: 40px; }
    h1 { color: #6e56ec; }
    .chat { background: #13131a; padding: 20px; border-radius: 12px; margin-top: 20px; }
    input { width: 100%; padding: 12px; background: #1a1a23; color: white; border: 1px solid #27272a; border-radius: 8px; }
    button { padding: 12px 24px; background: #6e56ec; color: white; border: none; border-radius: 8px; cursor: pointer; margin-top: 8px; }
    .msg { margin: 8px 0; padding: 8px; border-radius: 8px; }
    .user { background: #6e56ec20; }
    .assistant { background: #3ddc9720; }
  </style>
</head>
<body>
  <h1>Arena AI - Simulation Server</h1>
  <p>Connected to simulation backend at port ${PORT}</p>
  <div class="chat">
    <div id="messages"></div>
    <input id="input" placeholder="Ask anything..." onkeydown="if(event.key==='Enter')send()">
    <button onclick="send()">Send</button>
  </div>
  <script>
    async function send() {
      const input = document.getElementById('input');
      const text = input.value;
      if (!text) return;
      input.value = '';
      const msgs = document.getElementById('messages');
      const userDiv = document.createElement('div');
      userDiv.className = 'msg user';
      userDiv.textContent = 'You: ' + text;
      msgs.appendChild(userDiv);

      const resp = await fetch('/nextjs-api/stream/create-evaluation', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: text, modality: 'chat', mode: 'battle' })
      });
      const reader = resp.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      const aDiv = document.createElement('div');
      aDiv.className = 'msg assistant';
      aDiv.textContent = 'Model A: ';
      msgs.appendChild(aDiv);
      const bDiv = document.createElement('div');
      bDiv.className = 'msg assistant';
      bDiv.textContent = 'Model B: ';
      msgs.appendChild(bDiv);
      let currentDiv = aDiv;
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\\n');
        buffer = lines.pop();
        for (const line of lines) {
          if (!line.startsWith('data: ')) continue;
          const data = line.slice(6).trim();
          if (data === '[DONE]') continue;
          try {
            const evt = JSON.parse(data);
            if (evt.type === 'message_start' && evt.modelLabel === 'B') currentDiv = bDiv;
            if (evt.type === 'text_delta') currentDiv.textContent += evt.delta;
          } catch (e) {}
        }
      }
    }
  </script>
</body>
</html>
      `);
      return;
    }

    // 404
    json(res, 404, { error: 'Not found', path });
  } catch (err) {
    console.error('Error:', err);
    json(res, 500, { error: 'Internal server error', message: err.message });
  }
});

server.listen(PORT, () => {
  console.log('╔══════════════════════════════════════════════════════════╗');
  console.log('║                                                          ║');
  console.log('║   Arena.ai Simulation Server                             ║');
  console.log(`║   Listening on http://localhost:${PORT}                    ║`);
  console.log('║                                                          ║');
  console.log('║   Test endpoints:                                        ║');
  console.log('║     POST /nextjs-api/sign-in/email                       ║');
  console.log('║     GET  /api/me                                         ║');
  console.log('║     GET  /api/history/unified                            ║');
  console.log('║     POST /nextjs-api/stream/create-evaluation            ║');
  console.log('║                                                          ║');
  console.log('║   Test credentials:                                      ║');
  console.log('║     Email:    Ai9900@bjedu.tech                          ║');
  console.log('║     Password: Ai9900@bjedu.tech                          ║');
  console.log('║                                                          ║');
  console.log('╚══════════════════════════════════════════════════════════╝');
});
