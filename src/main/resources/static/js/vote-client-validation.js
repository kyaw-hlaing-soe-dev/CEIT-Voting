// javascript
// Client-side validation to prevent selecting the same candidate number in paired categories.
// Attach this script in your voting page (e.g. <script src="/js/vote-client-validation.js"></script>).

const pairing = {
    KING: 'PRINCE',
    PRINCE: 'KING',
    QUEEN: 'PRINCESS',
    PRINCESS: 'QUEEN'
};

function onSelectionChange(event) {
    const el = event.target;
    const category = el.dataset.category;
    const value = el.value; // candidate number as string
    const paired = pairing[category];
    if (!paired) return;

    // handle <select>
    const pairedSelect = document.querySelector(`[data-category="${paired}"]`);
    if (pairedSelect && pairedSelect.tagName === 'SELECT') {
        for (const opt of pairedSelect.options) {
            opt.disabled = (opt.value === value);
        }
    }

    // handle radio groups
    const pairedRadios = document.querySelectorAll(`input[type="radio"][data-category="${paired}"]`);
    pairedRadios.forEach(r => {
        if (r.value === value) {
            r.disabled = true;
            if (r.checked) {
                r.checked = false;
                alert(`You already voted Candidate No.${value} for ${paired}. You cannot choose Candidate No.${value} for ${category}.`);
            }
        } else {
            r.disabled = false;
        }
    });
}

// attach listeners
document.querySelectorAll('select[data-category], input[type="radio"][data-category]').forEach(el => {
    el.addEventListener('change', onSelectionChange);
});
