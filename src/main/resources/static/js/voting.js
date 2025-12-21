(() => {
  const STYLE_ID = 'voting-radio-grid-styles';
  const DEFAULT_API_BASE = '/api';
  const API_BASE = (() => {
    const meta = document.querySelector('meta[name="api-base"]')?.content;
    const explicit = window.API_BASE || meta || DEFAULT_API_BASE;
    return explicit.endsWith('/') ? explicit.slice(0, -1) : explicit;
  })();
  const CATEGORIES = ['KING', 'QUEEN', 'PRINCE', 'PRINCESS', 'COUPLE'];

  // Storage helpers (session-scoped for anonymity)
  const getStoredToken = () => sessionStorage.getItem('voteToken');
  const saveToken = (token) => sessionStorage.setItem('voteToken', token);
  const clearToken = () => sessionStorage.removeItem('voteToken');
  const getStoredPin = () => sessionStorage.getItem('votingPin');
  const savePin = (pin) => sessionStorage.setItem('votingPin', pin);

  const selectionValueKey = (category) => `sel:${category}`;
  const selectedObjectKey = (category) => `sel:${category}:obj`;
  const saveSelection = (category, obj) => {
    sessionStorage.setItem(selectionValueKey(category), String(obj?.candidateNumber || ''));
    sessionStorage.setItem(selectedObjectKey(category), JSON.stringify(obj || {}));
  };
  const loadSelection = (category) => {
    const obj = sessionStorage.getItem(selectedObjectKey(category));
    if (!obj) return null;
    try { return JSON.parse(obj); } catch (e) { return null; }
  };
  const clearSelections = () => {
    CATEGORIES.forEach(c => {
      sessionStorage.removeItem(selectionValueKey(c));
      sessionStorage.removeItem(selectedObjectKey(c));
    });
  };

  // Skeleton helpers
  const pageSkeletons = new Set();
  const registerSkeleton = (id) => { const el = document.getElementById(id); if (el) pageSkeletons.add(el); return el; };
  const registerAllSkeletons = () => ['selection-loading', 'options-skeleton'].forEach(registerSkeleton);
  const showSkeletons = () => pageSkeletons.forEach(el => el.classList.remove('hidden'));
  const hideSkeletons = () => pageSkeletons.forEach(el => el.classList.add('hidden'));

  // UX feedback
  let audioContext = null;
  const getAudioContext = () => { if (!audioContext) audioContext = new (window.AudioContext || window.webkitAudioContext)(); return audioContext; };
  const playKeypadFeedback = (btn) => { if (btn) { btn.classList.add('keypad-pressed'); setTimeout(() => btn.classList.remove('keypad-pressed'), 150); } try { const ctx = getAudioContext(); const osc = ctx.createOscillator(); const gain = ctx.createGain(); osc.connect(gain); gain.connect(ctx.destination); osc.frequency.value = 1200; osc.type = 'sine'; gain.gain.setValueAtTime(0.1, ctx.currentTime); gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.08); osc.start(ctx.currentTime); osc.stop(ctx.currentTime + 0.08);} catch(e) {} };
  const playRadioFeedback = (labelEl) => { if (labelEl) { labelEl.style.transform = 'scale(1.1)'; labelEl.style.transition = 'transform 0.15s ease'; setTimeout(() => { labelEl.style.transform = 'scale(1)'; }, 150); } try { const ctx = getAudioContext(); const osc = ctx.createOscillator(); const gain = ctx.createGain(); osc.connect(gain); gain.connect(ctx.destination); osc.frequency.value = 880; osc.type = 'sine'; gain.gain.setValueAtTime(0.08, ctx.currentTime); gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.1); osc.start(ctx.currentTime); osc.stop(ctx.currentTime + 0.1);} catch(e) {} };

  // API helpers
  const verifyPin = async (pin) => {
    const res = await fetch(`${API_BASE}/pins/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pin })
    });
    if (res.status === 404) return { valid: false };
    if (res.status === 409) return { valid: false, message: 'PIN already used' };
    if (!res.ok) throw new Error('Failed to verify PIN');
    const data = await res.json();
    return { valid: true, token: data.token };
  };

  const fetchCandidates = async (category) => {
    const res = await fetch(`${API_BASE}/candidates/${String(category).toUpperCase()}`);
    if (!res.ok) throw new Error(await res.text() || 'Failed to load candidates');
    return res.json();
  };

  const submitVotes = async ({ token, votes }) => {
    const res = await fetch(`${API_BASE}/voting/bulk-vote`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Vote-Token': token },
      body: JSON.stringify({ votes })
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.message || body.reason || 'Vote submission failed.');
    // On successful vote submission, mark this browser as having voted.
    try { setVotedFlag(true); } catch (e) { console.debug('Failed to set voted flag after submit', e); }
    return body;
  };

  const ensureTokenOrThrow = () => {
    const t = getStoredToken();
    if (!t) throw new Error('No vote session. Please re-enter your PIN.');
    return t;
  };

  // Inject CSS for layout
  function injectStyles() {
    if (document.getElementById(STYLE_ID)) return;
    const css = `
      .radio-grid { display: grid; gap: 12px; justify-items: center; }
      .radio-option { background: #f8f8f8; border: 2px solid #ddd; border-radius: 12px; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 8px; cursor: pointer; transition: all 0.2s ease; }
      .radio-option:hover { border-color: #3b82f6; background: #eff6ff; }
      .radio-option.selected { border-color: #3b82f6; background: #dbeafe; }
      .radio-option .number { font-size: 1.5rem; font-weight: bold; color: #1e3a8a; padding: 8px; }
      .radio-label { display:flex; flex-direction:column; align-items:center; justify-content:center; width:100%; min-height:72px; padding:8px; box-sizing:border-box; border-radius:8px; cursor:pointer; user-select:none; }
      .radio-label input[type="radio"] { width:28px; height:28px; margin-top:8px; }
      .radio-grid .spacer { visibility:hidden; pointer-events:none; }
      #next-btn, #prev-btn { display: block !important; width: 100%; margin-top: 20px; }
    `;
    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.appendChild(document.createTextNode(css));
    document.head.appendChild(style);
  }

  function layoutOptions(container) {
    if (!container) return;
    injectStyles();
    container.style.gridTemplateColumns = `repeat(5, 1fr)`;
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

  const enforcePairedRadios = (category, selectedNumber) => {
    const pairing = { KING: 'PRINCE', PRINCE: 'KING', QUEEN: 'PRINCESS', PRINCESS: 'QUEEN' };
    const paired = pairing[category];
    if (!paired || selectedNumber == null) return;
    const radios = document.querySelectorAll(`input[type="radio"][data-category="${paired}"]`);
    radios.forEach(r => {
      if (r.value === String(selectedNumber)) {
        r.disabled = true;
        if (r.checked) {
          r.checked = false;
          const label = r.closest('.radio-option');
          if (label) label.classList.remove('selected');
          alert(`You already voted Candidate No.${selectedNumber} for ${category}. You cannot choose Candidate No.${selectedNumber} for ${paired}.`);
        }
      } else {
        r.disabled = false;
      }
    });
  };

  // Selection page init
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

    registerAllSkeletons();
    const ensureButtonsOutside = () => {
      if (container && nextBtn && container.contains(nextBtn)) container.after(nextBtn);
      if (container && prevBtn && container.contains(prevBtn)) nextBtn?.after(prevBtn);
    };
    ensureButtonsOutside();

    showSkeletons();
    let candidates = [];
    try { candidates = await fetchCandidates(category); } catch (err) {
      if (container) container.innerHTML = `<p class="text-red-600 text-center">${err.message}</p>`;
      if (nextBtn) { nextBtn.disabled = true; nextBtn.classList.add('opacity-50', 'cursor-not-allowed'); }
      hideSkeletons();
      return;
    }
    hideSkeletons();

    if (!Array.isArray(candidates) || candidates.length === 0) {
      if (container) container.innerHTML = `<p class="text-gray-700 text-center">No candidates available.</p>`;
      if (nextBtn) { nextBtn.disabled = true; nextBtn.classList.add('opacity-50', 'cursor-not-allowed'); }
      return;
    }

    let selectedNumber = loadSelection(category)?.candidateNumber || null;
    const placeholderImage = (candidate) => candidate?.imageUrl?.trim() ? candidate.imageUrl : 'https://placehold.co/300x400/f3f4f6/1e3a8a?text=Candidate';
    const updateCandidateDisplay = (candidate) => {
      if (!candidate) return;
      if (candidateImage) candidateImage.src = placeholderImage(candidate);
      if (candidateNumber) candidateNumber.textContent = `No ${candidate.candidateNumber}`;
      if (candidateName) candidateName.textContent = candidate.name || '';
      if (candidateDepartment) candidateDepartment.textContent = candidate.department || '';
    };
    const updateNextButton = () => {
      if (!nextBtn) return;
      nextBtn.disabled = !selectedNumber;
      nextBtn.style.opacity = selectedNumber ? '1' : '0.7';
      nextBtn.style.cursor = selectedNumber ? 'pointer' : 'not-allowed';
    };

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
        input.setAttribute('data-category', category);
        input.addEventListener('change', (evt) => {
          selectedNumber = parseInt(evt.target.value, 10);
          const cand = candidates.find(c => c.candidateNumber === selectedNumber);
          const labelEl = evt.target.closest('.radio-option');
          playRadioFeedback(labelEl);
          container.querySelectorAll('.radio-option').forEach(opt => opt.classList.remove('selected'));
          if (labelEl) labelEl.classList.add('selected');
          if (cand) { saveSelection(category, cand); updateCandidateDisplay(cand); }
          updateNextButton();
          enforcePairedRadios(category, selectedNumber);
        });
      });
    }

    if (selectedNumber) {
      const saved = candidates.find(c => c.candidateNumber === selectedNumber);
      if (saved) {
        const input = document.querySelector(`input[name="selection"][value="${selectedNumber}"]`);
        if (input) { input.checked = true; input.closest('.radio-option')?.classList.add('selected'); }
        updateCandidateDisplay(saved);
        enforcePairedRadios(category, selectedNumber);
      } else { selectedNumber = null; }
    }
    if (!selectedNumber && candidates.length > 0) updateCandidateDisplay(candidates[Math.floor(Math.random() * candidates.length)]);
    updateNextButton();

    nextBtn?.addEventListener('click', () => {
      if (!selectedNumber) { errorModal?.classList.remove('hidden'); return; }
      const cand = candidates.find(c => c.candidateNumber === selectedNumber);
      if (cand) saveSelection(category, cand);
      if (nextUrl) window.location.href = nextUrl;
    });
    if (prevBtn && prevUrl) prevBtn.addEventListener('click', () => (window.location.href = prevUrl));
    document.querySelectorAll('[data-close-modal]').forEach((btn) => btn.addEventListener('click', () => { const id = btn.getAttribute('data-close-modal'); document.getElementById(id)?.classList.add('hidden'); }));
  }

  function initAuto() { injectStyles(); observeContainer('options-container'); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initAuto); else initAuto();

  const loadSelectionsForSummary = () => CATEGORIES.map(category => ({ category, selection: loadSelection(category) }));

  // --- Browser Lock (client-side anti-multi-vote) ---
  // Uses localStorage + a long-lived cookie as a backup. This is a client-side deterrent
  // (prevents casual double-votes on the same browser). Tech-savvy users can bypass via
  // private/incognito windows or by clearing storage — that's expected and documented.
  const VOTED_FLAG_KEY = 'is_voter_done';
  const setVotedFlag = (v = true) => {
    try {
      localStorage.setItem(VOTED_FLAG_KEY, v ? 'true' : 'false');
      // long lived cookie (1 year)
      document.cookie = `voted=${v ? 'true' : 'false'}; max-age=31536000; path=/`;
    } catch (e) {
      // silently ignore storage errors
      console.debug('setVotedFlag failed', e);
    }
  };
  const clearVotedFlag = () => {
    try {
      localStorage.removeItem(VOTED_FLAG_KEY);
      document.cookie = 'voted=; max-age=0; path=/';
    } catch (e) { console.debug('clearVotedFlag failed', e); }
  };
  const hasVoted = () => {
    try {
      const ls = localStorage.getItem(VOTED_FLAG_KEY);
      if (ls === 'true') return true;
      // fallback to cookie
      const cookie = document.cookie.split(';').map(s => s.trim()).find(s => s.startsWith('voted='));
      if (cookie) return cookie.split('=')[1] === 'true';
      return false;
    } catch (e) {
      return false;
    }
  };

  window.VotingApp = {
    API_BASE,
    CATEGORIES,
    verifyPin: async (pin) => {
      const result = await verifyPin(pin);
      if (result.valid && result.token) { saveToken(result.token); savePin(pin); }
      return { valid: result.valid, message: result.message };
    },
    submitVotes: async (votes) => submitVotes({ token: ensureTokenOrThrow(), votes }),
    saveSelection,
    loadSelection,
    clearSelections,
    clearToken,
    getStoredPin,
    savePin,
    fetchCandidates,
    loadSelectionsForSummary,
    initSelectionPage,
    layoutOptions,
    playKeypadFeedback,
    playRadioFeedback,
    // Browser Lock API for frontend/admin testing
    hasVoted,
    setVotedFlag,
    clearVotedFlag,
  };
})();
