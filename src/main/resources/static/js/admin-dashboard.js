document.addEventListener("DOMContentLoaded", () => {
    const pinForm = document.getElementById("pinForm");
    const pinInput = document.getElementById("pinInput");
    const pinMessage = document.getElementById("pinMessage");
    const dashboard = document.getElementById("dashboard");
    const resultsTableBody = document.querySelector("#results-table tbody");

    const API_BASE = "/api/admin";
    const ADMIN_PIN = (window.KTUVoting && window.KTUVoting.adminPin) ? String(window.KTUVoting.adminPin) : "88296";

    document.getElementById("pinSubmit").addEventListener("click", async () => {
        const pin = pinInput.value;
        if (pin !== ADMIN_PIN) {
            pinMessage.style.display = "block";
            pinMessage.textContent = "Invalid PIN. Access denied.";
            return;
        }

        pinMessage.style.display = "none";
        dashboard.style.display = "block";
        document.getElementById("pinSection").style.display = "none";

        startLiveResultsRefresh();
        populateCandidates();
    });

    async function startLiveResultsRefresh() {
        setInterval(async () => {
            try {
                // NOTE: backend expects 'adminPin' query parameter
                const response = await fetch(`${API_BASE}/results?adminPin=${ADMIN_PIN}`);
                if (!response.ok) throw new Error("Failed to fetch results");

                const results = await response.json();
                updateResultsTable(results);
                document.getElementById('lastUpdated').textContent = 'Last updated: ' + new Date().toLocaleTimeString();
            } catch (error) {
                console.error("Error fetching live results:", error);
            }
        }, 5000);
    }

    function updateResultsTable(results) {
        resultsTableBody.innerHTML = "";
        results.forEach((candidate, index) => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td class="p-2 border">${index + 1}</td>
                <td class="p-2 border">${candidate.name}</td>
                <td class="p-2 border">${candidate.department || ''}</td>
                <td class="p-2 border">${candidate.category}</td>
                <td class="p-2 border">${candidate.voteCount}</td>
            `;
            resultsTableBody.appendChild(row);
        });
    }

    async function populateCandidates() {
        try {
            const response = await fetch(`${API_BASE}/candidates?adminPin=${ADMIN_PIN}`);
            if (!response.ok) throw new Error("Failed to fetch candidates");

            const candidates = await response.json();
            renderCandidateList(candidates);
        } catch (error) {
            console.error("Error fetching candidates:", error);
        }
    }

    function renderCandidateList(candidates) {
        const list = document.getElementById('candidatesList');
        list.innerHTML = '';
        candidates.forEach(c => {
            const item = document.createElement('div');
            item.className = 'p-2 border rounded flex items-center justify-between';
            item.innerHTML = `
                <div>
                    <div class="font-semibold">${c.name} <span class="text-xs text-gray-500">(#${c.candidateNumber})</span></div>
                    <div class="text-xs text-gray-600">${c.department || ''} — ${c.category}</div>
                </div>
                <div class="flex gap-2">
                    <button data-id="${c.id}" class="editBtn px-2 py-1 bg-yellow-100 rounded">Edit</button>
                    <button data-id="${c.id}" class="deleteBtn px-2 py-1 bg-red-100 rounded">Delete</button>
                </div>
            `;
            list.appendChild(item);
        });

        // hook buttons
        document.querySelectorAll('.deleteBtn').forEach(btn => btn.addEventListener('click', async (e) => {
            const id = e.target.getAttribute('data-id');
            if (!confirm('Delete candidate?')) return;
            try {
                const res = await fetch(`${API_BASE}/candidates/${id}?adminPin=${ADMIN_PIN}`, { method: 'DELETE' });
                if (res.status === 204) {
                    populateCandidates();
                } else {
                    alert('Failed to delete candidate');
                }
            } catch (err) {
                console.error(err);
                alert('Error deleting candidate');
            }
        }));

        document.querySelectorAll('.editBtn').forEach(btn => btn.addEventListener('click', (e) => {
            const id = e.target.getAttribute('data-id');
            openEditModal(id);
        }));
    }

    // Create form
    document.getElementById('createBtn').addEventListener('click', async () => {
        const payload = {
            category: document.getElementById('createCategory').value,
            candidateNumber: parseInt(document.getElementById('createNumber').value, 10),
            name: document.getElementById('createName').value,
            department: document.getElementById('createDept').value,
            imageUrl: null,
            voteCount: 0
        };

        try {
            const res = await fetch(`${API_BASE}/candidates?adminPin=${ADMIN_PIN}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error('Failed to create');
            document.getElementById('createMsg').style.display = 'block';
            document.getElementById('createMsg').textContent = 'Created';
            setTimeout(() => { document.getElementById('createMsg').style.display = 'none'; }, 2000);
            populateCandidates();
        } catch (err) {
            console.error(err);
            alert('Error creating candidate');
        }
    });

    document.getElementById('refreshCandidatesBtn').addEventListener('click', () => populateCandidates());

    // simple edit modal implemented inline using prompt for brevity
    async function openEditModal(id) {
        try {
            const res = await fetch(`${API_BASE}/candidates/${id}?adminPin=${ADMIN_PIN}`);
            if (!res.ok) throw new Error('Failed to load');
            const c = await res.json();
            const newName = prompt('Name', c.name);
            if (newName === null) return; // cancel
            const newDept = prompt('Department', c.department || '');
            if (newDept === null) return;

            const payload = {
                id: c.id,
                category: c.category,
                candidateNumber: c.candidateNumber,
                name: newName,
                department: newDept,
                imageUrl: c.imageUrl,
                voteCount: c.voteCount
            };

            const put = await fetch(`${API_BASE}/candidates/${id}?adminPin=${ADMIN_PIN}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!put.ok) throw new Error('Update failed');
            populateCandidates();
        } catch (err) {
            console.error(err);
            alert('Error editing candidate');
        }
    }

});
