(() => {
  // --- Configuration ---
  const STYLE_ID = 'voting-radio-grid-styles';
  const DEFAULT_API_BASE = '/api';
  const API_BASE = (() => {
    const meta = document.querySelector('meta[name="api-base"]')?.content;
    const explicit = window.API_BASE || meta || DEFAULT_API_BASE;
    return explicit.endsWith('/') ? explicit.slice(0, -1) : explicit;
  })();
  const CATEGORIES = ['KING', 'QUEEN', 'PRINCE', 'PRINCESS', 'COUPLE'];

  // --- Utilities ---
  const $ = (sel, root = document) => root.querySelector(sel);
  const debounce = (fn, ms = 80) => {
    let t;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), ms);
    };
  };
  const capitalize = (s = '') => s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();

  // --- Device ID + Pin Storage ---
  // Use advanced fingerprinting for unique device identification
  const getDeviceId = async () => {
    // Try to use advanced fingerprinting if available
    if (window.DeviceFingerprint) {
      try {
        const fingerprint = await window.DeviceFingerprint.get();
        localStorage.setItem('deviceId', fingerprint);
        return fingerprint;
      } catch (e) {
        console.warn('Fingerprint generation failed, using fallback:', e);
      }
    }
    // Fallback to stored or random ID
    let deviceId = localStorage.getItem('deviceId');
    if (!deviceId) {
      deviceId = 'dev-' + Math.random().toString(36).slice(2);
      localStorage.setItem('deviceId', deviceId);
    }
    return deviceId;
  };

  // Synchronous version for compatibility
  const getDeviceIdSync = () => {
    return localStorage.getItem('deviceId') || 'dev-' + Math.random().toString(36).slice(2);
  };

  const getStoredPin = () => localStorage.getItem('votingPin');
  const savePin = (pin) => localStorage.setItem('votingPin', pin);

  const selectionValueKey = (category) => `sel:${category}`;
  const selectedObjectKey = (category) => `sel:${category}:obj`;
  
  const saveSelection = (category, obj) => {
    localStorage.setItem(selectionValueKey(category), String(obj?.candidateNumber || ''));
    localStorage.setItem(selectedObjectKey(category), JSON.stringify(obj || {}));
  };
  
  const loadSelection = (category) => {
    const obj = localStorage.getItem(selectedObjectKey(category));
    if (obj) {
      try {
        return JSON.parse(obj);
      } catch (e) {
        return null;
      }
    }
    return null;
  };
  
  const clearSelections = () => {
    CATEGORIES.forEach(c => {
      localStorage.removeItem(selectionValueKey(c));
      localStorage.removeItem(selectedObjectKey(c));
    });
  };

  // --- Skeleton helpers ---
  const pageSkeletons = new Set();
  const registerSkeleton = (id) => {
    const el = document.getElementById(id);
    if (el) pageSkeletons.add(el);
    return el;
  };
  const registerAllSkeletons = () => {
    // Register common skeleton IDs
    ['selection-loading', 'options-skeleton'].forEach(registerSkeleton);
  };
  const showSkeletons = () => pageSkeletons.forEach(el => el.classList.remove('hidden'));
  const hideSkeletons = () => pageSkeletons.forEach(el => el.classList.add('hidden'));

  // --- Keypad feedback ---
  let audioContext = null;
  const getAudioContext = () => {
    if (!audioContext) {
      audioContext = new (window.AudioContext || window.webkitAudioContext)();
    }
    return audioContext;
  };
  const playKeypadFeedback = (btn) => {
    if (btn) {
      btn.classList.add('keypad-pressed');
      setTimeout(() => btn.classList.remove('keypad-pressed'), 150);
    }
    try {
      const ctx = getAudioContext();
      const oscillator = ctx.createOscillator();
      const gainNode = ctx.createGain();
      oscillator.connect(gainNode);
      gainNode.connect(ctx.destination);
      oscillator.frequency.value = 1200;
      oscillator.type = 'sine';
      gainNode.gain.setValueAtTime(0.1, ctx.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.08);
      oscillator.start(ctx.currentTime);
      oscillator.stop(ctx.currentTime + 0.08);
    } catch (e) {}
  };

  // Sound and visual feedback for radio button selection
  const playRadioFeedback = (labelEl) => {
    // Visual scale effect
    if (labelEl) {
      labelEl.style.transform = 'scale(1.1)';
      labelEl.style.transition = 'transform 0.15s ease';
      setTimeout(() => {
        labelEl.style.transform = 'scale(1)';
      }, 150);
    }
    // Play selection sound (higher pitch, pleasant tone)
    try {
      const ctx = getAudioContext();
      const oscillator = ctx.createOscillator();
      const gainNode = ctx.createGain();
      oscillator.connect(gainNode);
      gainNode.connect(ctx.destination);
      oscillator.frequency.value = 880; // A5 note - pleasant selection sound
      oscillator.type = 'sine';
      gainNode.gain.setValueAtTime(0.08, ctx.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.1);
      oscillator.start(ctx.currentTime);
      oscillator.stop(ctx.currentTime + 0.1);
    } catch (e) {}
  };

  // --- API helpers ---
  const verifyPin = async (pin) => {
    // Get device fingerprint before sending request
    let fingerprint = null;
    if (window.DeviceFingerprint) {
      try {
        fingerprint = await window.DeviceFingerprint.get();
      } catch (e) {
        console.warn('Failed to generate fingerprint:', e);
      }
    }

    const res = await fetch(`${API_BASE}/auth/verify-pin`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        pin,
        fingerprint: fingerprint // Send fingerprint to server
      }),
    });

    // Handle rate limiting
    if (res.status === 429) {
      const data = await res.json();
      throw new Error(data.message || 'Too many attempts. Please wait before trying again.');
    }

    // Handle invalid PIN
    if (res.status === 404) {
      try {
        const data = await res.json();
        const remaining = data.remainingAttempts;
        return { valid: false, remainingAttempts: remaining };
      } catch (e) {
        return { valid: false };
      }
    }

    if (!res.ok) {
      throw new Error('Failed to verify PIN');
    }

    return res.json();
  };

  // Check device voting status
  const checkDeviceStatus = async () => {
    try {
      // Get fingerprint for device check
      let fingerprint = null;
      if (window.DeviceFingerprint) {
        try {
          fingerprint = await window.DeviceFingerprint.get();
        } catch (e) {}
      }

      const res = await fetch(`${API_BASE}/auth/check-device`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fingerprint })
      });

      if (!res.ok) {
        return { hasVoted: false };
      }
      return res.json();
    } catch (e) {
      return { hasVoted: false };
    }
  };

  const fetchCandidates = async (category) => {
    // Ensure the path matches controller enum handling; use uppercase category to be explicit
    const res = await fetch(`${API_BASE}/candidates/${String(category).toUpperCase()}`);
    if (!res.ok) throw new Error(await res.text() || 'Failed to load candidates');
    return res.json();
  };

  const submitVotes = async ({ pin, deviceId, votes }) => {
    // Client-side validation: ensure no candidateNumber is reused across paired categories for the same voter
    // Build a map of category -> number
    const incoming = new Map();
    for (const item of votes) {
      if (!item || !item.category) continue;
      incoming.set(item.category, item.candidateNumber);
    }

    const paired = {
      KING: ['PRINCE'],
      PRINCE: ['KING'],
      QUEEN: ['PRINCESS'],
      PRINCESS: ['QUEEN']
    };

    for (const [cat, num] of incoming.entries()) {
      const pair = paired[cat];
      if (!pair || !num) continue;
      for (const p of pair) {
        if (incoming.has(p) && incoming.get(p) === num) {
          throw new Error(`Invalid selection: Candidate No.${num} selected for both ${cat} and ${p}.`);
        }
      }
    }

    // Get all device identification data for multi-factor verification
    let fingerprint = null;
    let hardwareHash = null;
    let screenInfo = null;

    if (window.DeviceFingerprint) {
      try {
        // Get all device data at once
        const deviceData = await window.DeviceFingerprint.getDeviceData();
        fingerprint = deviceData.fingerprint;
        hardwareHash = deviceData.hardwareHash;
        screenInfo = deviceData.screenInfo;
      } catch (e) {
        console.warn('Device fingerprinting failed:', e);
        // Try individual methods as fallback
        try {
          fingerprint = await window.DeviceFingerprint.get();
        } catch (e2) {}
        try {
          hardwareHash = await window.DeviceFingerprint.getHardwareHash();
        } catch (e3) {}
        try {
          screenInfo = window.DeviceFingerprint.getScreenInfo();
        } catch (e4) {}
      }
    }

    // Ensure we have a valid deviceId
    const finalDeviceId = fingerprint || deviceId || ('dev-' + Math.random().toString(36).slice(2));

    const requestBody = {
      pin,
      deviceId: finalDeviceId,
      fingerprint: fingerprint,
      hardwareHash: hardwareHash,
      screenInfo: screenInfo,
      votes: votes
    };

    const res = await fetch(`${API_BASE}/voting/bulk-vote`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestBody),
    });

    const contentType = res.headers.get('content-type') || '';
    const body = contentType.includes('application/json')
      ? await res.json()
      : await res.text();

    if (!res.ok) {
      const msg = body && body.message ? body.message : (body || 'Vote submission failed.');
      throw new Error(msg);
    }

    return body;
  };

  // --- Inject CSS styles ---
  function injectStyles() {
    if (document.getElementById(STYLE_ID)) return;

    const css = `
      .radio-grid {
        display: grid;
        gap: 12px;
        justify-items: center;
      }
      .radio-option {
        background: #f8f8f8;
        border: 2px solid #ddd;
        border-radius: 12px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 8px;
        cursor: pointer;
        transition: all 0.2s ease;
      }
      .radio-option:hover {
        border-color: #3b82f6;
        background: #eff6ff;
      }
      .radio-option.selected {
        border-color: #3b82f6;
        background: #dbeafe;
      }
      .radio-option .number {
        font-size: 1.5rem;
        font-weight: bold;
        color: #1e3a8a;
        padding: 8px;
      }
      .radio-label {
        display:flex;
        flex-direction:column;
        align-items:center;
        justify-content:center;
        width:100%;
        min-height:72px;
        padding:8px;
        box-sizing:border-box;
        border-radius:8px;
        cursor:pointer;
        user-select:none;
      }
      .radio-label input[type="radio"] {
        width:28px;
        height:28px;
        margin-top:8px;
      }
      .radio-grid .spacer {
        visibility:hidden;
        pointer-events:none;
      }

      /* BUTTONS MUST NEVER BE INSIDE GRID */
      #next-btn,
      #prev-btn {
        display: block !important;
        width: 100%;
        margin-top: 20px;
      }
    `;

    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.appendChild(document.createTextNode(css));
    document.head.appendChild(style);
  }

  // --- ALWAYS 5 COLUMN DESKTOP LAYOUT ---
  function layoutOptions(container) {
    if (!container) return;
    injectStyles();

    container.style.gridTemplateColumns = `repeat(5, 1fr)`;

    // Remove old spacers
    Array.from(container.querySelectorAll('.spacer')).forEach(s => s.remove());

    const children = Array.from(container.children).filter(n => n.nodeType === 1);
    const total = children.length;

    if (total <= 5) return;

    const bottomCount = total - 5;
    if (bottomCount <= 0 || bottomCount >= 5) return;

    const offset = Math.floor((5 - bottomCount) / 2);
    const firstBottom = container.children[5] || null;

    for (let i = 0; i < offset; i++) {
      const spacer = document.createElement('div');
      spacer.className = 'spacer';
      container.insertBefore(spacer, firstBottom);
    }
  }

  const debouncedLayout = debounce(() => {
    const c = document.getElementById('options-container');
    if (c) layoutOptions(c);
  }, 100);

  window.addEventListener('resize', debouncedLayout);

  function observeContainer(id = 'options-container') {
    const container = document.getElementById(id);
    if (!container) return;

    layoutOptions(container);

    const mo = new MutationObserver(() => {
      clearTimeout(container.__layoutTimeout);
      container.__layoutTimeout = setTimeout(() => layoutOptions(container), 60);
    });

    mo.observe(container, { childList: true });
    container.__layoutObserver = mo;
  }

  // --- Selection Page Init ---
  async function initSelectionPage({ category, nextUrl, prevUrl } = {}) {
    const pin = getStoredPin();
    if (!pin) { window.location.href = '/pin'; return; }

    const container = document.getElementById('options-container');
    const errorModal = document.getElementById('selection-error-modal');
    const nextBtn = document.getElementById('next-btn');
    const prevBtn = document.getElementById('prev-btn');
    const candidateImage = document.getElementById('candidate-image');
    const candidateNumber = document.getElementById('candidate-number');
    const candidateName = document.getElementById('candidate-name');
    const candidateDepartment = document.getElementById('candidate-department');

    // Register all skeleton elements for this page
    registerAllSkeletons();

    // ⭐ NEW FIX: Ensure buttons NEVER live inside the grid container
    const ensureButtonsOutside = () => {
      if (container && nextBtn && container.contains(nextBtn)) {
        container.after(nextBtn);
      }
      if (container && prevBtn && container.contains(prevBtn)) {
        nextBtn.after(prevBtn);
      }
    };
    ensureButtonsOutside();

    showSkeletons();
    let candidates = [];
    try {
      candidates = await fetchCandidates(category);
    } catch (err) {
      if (container)
        container.innerHTML = `<p class="text-red-600 text-center">${err.message}</p>`;
      if (nextBtn) {
        nextBtn.disabled = true;
        nextBtn.classList.add('opacity-50', 'cursor-not-allowed');
      }
      hideSkeletons();
      return;
    }
    hideSkeletons();

    if (!Array.isArray(candidates) || candidates.length === 0) {
      if (container)
        container.innerHTML = `<p class="text-gray-700 text-center">No candidates available.</p>`;
      if (nextBtn) {
        nextBtn.disabled = true;
        nextBtn.classList.add('opacity-50', 'cursor-not-allowed');
      }
      return;
    }

    let selectedNumber = loadSelection(category)?.candidateNumber || null;

    const placeholderImage = (candidate) =>
      candidate?.imageUrl?.trim()
        ? candidate.imageUrl
        : 'https://placehold.co/300x400/f3f4f6/1e3a8a?text=Candidate';

    const updateCandidateDisplay = (candidate) => {
      if (!candidate) return;
      if (candidateImage) candidateImage.src = placeholderImage(candidate);
      if (candidateNumber) candidateNumber.textContent = `No ${candidate.candidateNumber}`;
      if (candidateName) candidateName.textContent = candidate.name || '';
      if (candidateDepartment)
        candidateDepartment.textContent = candidate.department || '';
    };

    const updateNextButton = () => {
      if (!nextBtn) return;

      if (selectedNumber) {
        nextBtn.style.opacity = '1';
        nextBtn.style.cursor = 'pointer';
        nextBtn.disabled = false;
      } else {
        nextBtn.style.opacity = '0.7';
        nextBtn.disabled = true;
      }
    };

    // Render Options
    if (container) {
      container.innerHTML = '';
      container.classList.add('radio-grid');

      candidates.forEach((candidate) => {
        const label = document.createElement('label');
        label.className = 'radio-option';
        label.htmlFor = `option-${candidate.candidateNumber}`;
        label.innerHTML = `
          <span class="number">${candidate.candidateNumber}</span>
          <input type="radio" id="option-${candidate.candidateNumber}" name="selection" value="${candidate.candidateNumber}" />
        `;
        container.appendChild(label);
      });

      layoutOptions(container);

      container.querySelectorAll('input[name="selection"]').forEach((input) => {
        input.addEventListener('change', (evt) => {
          selectedNumber = parseInt(evt.target.value, 10);
          const cand = candidates.find(c => c.candidateNumber === selectedNumber);

          // Play sound and visual feedback
          const labelEl = evt.target.closest('.radio-option');
          playRadioFeedback(labelEl);

          // Update selected state for all options
          container.querySelectorAll('.radio-option').forEach(opt => {
            opt.classList.remove('selected');
          });
          if (labelEl) labelEl.classList.add('selected');

          if (cand) {
            saveSelection(category, cand);
            updateCandidateDisplay(cand);
          }
          updateNextButton();
        });
      });
    }

    // Restore saved selection or show first candidate
    if (selectedNumber) {
      const saved = candidates.find(c => c.candidateNumber === selectedNumber);
      if (saved) {
        const input = document.querySelector(
          `input[name="selection"][value="${selectedNumber}"]`
        );
        if (input) {
          input.checked = true;
          // Highlight the selected option
          const labelEl = input.closest('.radio-option');
          if (labelEl) labelEl.classList.add('selected');
        }
        updateCandidateDisplay(saved);
      } else selectedNumber = null;
    }

    // Show random candidate's data on initial load if no selection saved
    if (!selectedNumber && candidates.length > 0) {
      const randomIndex = Math.floor(Math.random() * candidates.length);
      updateCandidateDisplay(candidates[randomIndex]);
    }

    updateNextButton();

    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        if (!selectedNumber) {
          if (errorModal) errorModal.classList.remove('hidden');
          return;
        }

        const cand = candidates.find(c => c.candidateNumber === selectedNumber);
        if (cand) saveSelection(category, cand);

        if (nextUrl) window.location.href = nextUrl;
      });
    }

    if (prevBtn && prevUrl)
      prevBtn.addEventListener('click', () => (window.location.href = prevUrl));

    document.querySelectorAll('[data-close-modal]').forEach((btn) =>
      btn.addEventListener('click', () => {
        const id = btn.getAttribute('data-close-modal');
        const m = document.getElementById(id);
        if (m) m.classList.add('hidden');
      })
    );
  }

  // --- Init ---
  function initAuto() {
    injectStyles();
    observeContainer('options-container');
  }

  if (document.readyState === 'loading')
    document.addEventListener('DOMContentLoaded', initAuto);
  else initAuto();

  // --- Load all selections for summary page ---
  const loadSelectionsForSummary = () => {
    return CATEGORIES.map(category => ({
      category,
      selection: loadSelection(category)
    }));
  };

  // --- Public API ---
  window.VotingApp = Object.assign(window.VotingApp || {}, {
    API_BASE,
    CATEGORIES,
    getDeviceId,
    getDeviceIdSync,
    getStoredPin,
    savePin,
    verifyPin,
    checkDeviceStatus,
    fetchCandidates,
    submitVotes,
    saveSelection,
    loadSelection,
    clearSelections,
    loadSelectionsForSummary,
    initSelectionPage,
    layoutOptions,
    playKeypadFeedback,
    playRadioFeedback,
  });
})();
