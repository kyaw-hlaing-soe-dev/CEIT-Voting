(() => {
  // Allow overriding the API base via a meta tag (<meta name="api-base" ...>)
  // or a global window.API_BASE. Falls back to the default /api path.
  const API_BASE = (() => {
    const metaBase = document.querySelector('meta[name="api-base"]')?.content;
    const explicitBase = window.API_BASE || metaBase || '/api';
    return explicitBase.endsWith('/') ? explicitBase.slice(0, -1) : explicitBase;
  })();
  const CATEGORIES = ['KING', 'QUEEN', 'PRINCE', 'PRINCESS', 'COUPLE'];

  const capitalize = (value) => value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
  const categoryKey = (category) => category.toLowerCase();
  const selectionValueKey = (category) => `${categoryKey(category)}Selection`;
  const selectedObjectKey = (category) => `selected${capitalize(category.toLowerCase())}`;

  const placeholderImage = (candidate) => {
    if (candidate && candidate.imageUrl && candidate.imageUrl.trim().length > 0) {
      return candidate.imageUrl;
    }
    return 'https://placehold.co/300x400/f3f4f6/1e3a8a?text=Candidate';
  };

  const getDeviceId = () => {
    let deviceId = localStorage.getItem('votingDeviceId');
    if (!deviceId) {
      if (typeof crypto !== 'undefined' && crypto.randomUUID) {
        deviceId = crypto.randomUUID();
      } else {
        deviceId = `device-${Date.now()}-${Math.random().toString(16).slice(2)}`;
      }
      localStorage.setItem('votingDeviceId', deviceId);
    }
    return deviceId;
  };

  const getStoredPin = () => localStorage.getItem('votingPin') || '';
  const savePin = (pin) => localStorage.setItem('votingPin', pin);

  const saveSelection = (category, candidate) => {
    const valueKey = selectionValueKey(category);
    const objectKey = selectedObjectKey(category);
    localStorage.setItem(valueKey, String(candidate.candidateNumber));
    localStorage.setItem(objectKey, JSON.stringify(candidate));
  };

  const loadSelection = (category) => {
    const objectKey = selectedObjectKey(category);
    const valueKey = selectionValueKey(category);

    const stored = localStorage.getItem(objectKey);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch (err) {
        // ignore parse errors and fall back to number
      }
    }

    const storedNumber = localStorage.getItem(valueKey);
    if (storedNumber) {
      return { candidateNumber: parseInt(storedNumber, 10) };
    }
    return null;
  };

  const clearSelections = () => {
    CATEGORIES.forEach((category) => {
      localStorage.removeItem(selectionValueKey(category));
      localStorage.removeItem(selectedObjectKey(category));
    });
  };

  const requirePinOrRedirect = () => {
    const pin = getStoredPin();
    if (!pin) {
      window.location.href = '/pin';
      return null;
    }
    return pin;
  };

  const verifyPin = async (pin) => {
    const response = await fetch(`${API_BASE}/auth/verify-pin`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pin })
    });

    if (response.status === 404) {
      return { valid: false, alreadyVoted: false };
    }

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Unable to verify PIN at the moment.');
    }

    return response.json();
  };

  const fetchCandidates = async (category) => {
    const response = await fetch(`${API_BASE}/candidates/${category.toLowerCase()}`);
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to load candidates.');
    }
    return response.json();
  };

  const submitVotes = async ({ pin, deviceId, votes }) => {
    const response = await fetch(`${API_BASE}/voting/bulk-vote`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pin, deviceId, votes })
    });

    const contentType = response.headers.get('content-type');
    const isJson = contentType && contentType.includes('application/json');
    const body = isJson ? await response.json() : await response.text();

    if (!response.ok) {
      const message = isJson && body && body.message ? body.message : (body || 'Vote submission failed.');
      throw new Error(message);
    }

    return body;
  };

  const initSelectionPage = async (config) => {
    const { category, nextUrl, prevUrl } = config;
    const pin = requirePinOrRedirect();
    if (!pin) {
      return;
    }
    // Device ID will be generated only when user clicks Confirm button

    const optionsContainer = document.getElementById('options-container');
    const errorModal = document.getElementById('selection-error-modal');
    const nextBtn = document.getElementById('next-btn');
    const candidateImage = document.getElementById('candidate-image');
    const candidateNumber = document.getElementById('candidate-number');
    const candidateName = document.getElementById('candidate-name');
    const candidateDepartment = document.getElementById('candidate-department');

    let candidates = [];
    try {
      candidates = await fetchCandidates(category);
    } catch (err) {
      if (optionsContainer) {
        optionsContainer.innerHTML = `<p class="text-red-600 text-center">${err.message}</p>`;
      }
      if (nextBtn) {
        nextBtn.disabled = true;
        nextBtn.classList.add('opacity-50', 'cursor-not-allowed');
      }
      return;
    }

    if (!Array.isArray(candidates) || candidates.length === 0) {
      if (optionsContainer) {
        optionsContainer.innerHTML = '<p class="text-gray-700 text-center">No candidates available.</p>';
      }
      if (nextBtn) {
        nextBtn.disabled = true;
        nextBtn.classList.add('opacity-50', 'cursor-not-allowed');
      }
      return;
    }

    let selectedNumber = loadSelection(category)?.candidateNumber;

    const updateCandidateDisplay = (candidate) => {
      if (!candidate) {
        return;
      }
      if (candidateImage) {
        candidateImage.src = placeholderImage(candidate);
      }
      if (candidateNumber) {
        candidateNumber.textContent = `No ${candidate.candidateNumber}`;
      }
      if (candidateName) {
        candidateName.textContent = candidate.name || '';
      }
      if (candidateDepartment) {
        candidateDepartment.textContent = candidate.department || '';
      }
    };

    const updateNextButton = () => {
      if (!nextBtn) {
        return;
      }
      if (selectedNumber) {
        nextBtn.style.opacity = '1';
        nextBtn.style.cursor = 'pointer';
        nextBtn.disabled = false;
      } else {
        nextBtn.style.opacity = '0.7';
        nextBtn.style.cursor = 'not-allowed';
        nextBtn.disabled = true;
      }
    };

    const renderOptions = () => {
      if (!optionsContainer) {
        return;
      }
      optionsContainer.innerHTML = '';
      optionsContainer.classList.add('radio-grid');

      candidates.forEach((candidate) => {
        const label = document.createElement('label');
        label.className = 'radio-label';
        label.htmlFor = `option-${candidate.candidateNumber}`;

        label.innerHTML = `
          <span class="text-sm font-medium text-gray-700">${candidate.candidateNumber}</span>
          <input type="radio" id="option-${candidate.candidateNumber}" name="selection" value="${candidate.candidateNumber}" class="radio-custom-selection">
        `;

        optionsContainer.appendChild(label);
      });

      optionsContainer.querySelectorAll('input[name="selection"]').forEach((input) => {
        input.addEventListener('change', (event) => {
          selectedNumber = parseInt(event.target.value, 10);
          const candidate = candidates.find((c) => c.candidateNumber === selectedNumber);
          if (candidate) {
            saveSelection(category, candidate);
            updateCandidateDisplay(candidate);
          }
          updateNextButton();
        });
      });
    };

    renderOptions();

    // Only auto-select if there's a previously saved selection
    if (selectedNumber) {
      const savedCandidate = candidates.find((c) => c.candidateNumber === selectedNumber);
      if (savedCandidate) {
        const initialInput = document.querySelector(`input[name="selection"][value="${selectedNumber}"]`);
        if (initialInput) {
          initialInput.checked = true;
        }
        updateCandidateDisplay(savedCandidate);
      } else {
        // Saved selection doesn't match any candidate, clear it
        selectedNumber = null;
        updateNextButton();
      }
    } else {
      // No previous selection - don't auto-select anything
      selectedNumber = null;
      updateNextButton();
    }

    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        if (!selectedNumber) {
          if (errorModal) {
            errorModal.classList.remove('hidden');
          }
          return;
        }
        const candidate = candidates.find((c) => c.candidateNumber === selectedNumber);
        if (candidate) {
          saveSelection(category, candidate);
        }
        window.location.href = nextUrl;
      });
    }

    if (prevUrl) {
      const prevBtn = document.getElementById('prev-btn');
      if (prevBtn) {
        prevBtn.addEventListener('click', () => window.location.href = prevUrl);
      }
    }

    document.querySelectorAll('[data-close-modal]').forEach((btn) => {
      btn.addEventListener('click', () => {
        const targetId = btn.getAttribute('data-close-modal');
        const modal = document.getElementById(targetId);
        if (modal) {
          modal.classList.add('hidden');
        }
      });
    });
  };

  const loadSelectionsForSummary = () => {
    return CATEGORIES.map((category) => ({
      category,
      selection: loadSelection(category)
    }));
  };

  window.VotingApp = {
    getDeviceId,
    getStoredPin,
    savePin,
    verifyPin,
    fetchCandidates,
    submitVotes,
    saveSelection,
    loadSelection,
    loadSelectionsForSummary,
    clearSelections,
    initSelectionPage,
    CATEGORIES
  };
})();

