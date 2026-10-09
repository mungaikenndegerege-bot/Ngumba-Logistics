/* =====================================================================
   Ngumba Logistics — Fleet Manager Web App JS
   All communication is via fetch() to the REST API at /api/*
   ===================================================================== */

'use strict';

// ========================= STATE =========================
let trucks = [];
let constants = { routes: [], miscCategories: [] };

const today = () => new Date().toISOString().slice(0, 10);
const fmt = v => Number(v).toLocaleString('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const ksh = v => 'KSh ' + fmt(v);

// ========================= INIT =========================
document.addEventListener('DOMContentLoaded', () => {
  // Tab navigation
  document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });

  // Close modal on overlay click
  document.querySelectorAll('.modal-overlay').forEach(el => {
    el.addEventListener('click', e => { if (e.target === el) closeModal(el.id); });
  });

  // Keyboard: Escape closes topmost modal
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
      const open = document.querySelector('.modal-overlay.open');
      if (open) closeModal(open.id);
    }
  });

  initReportControls();
  loadConstants().then(() => {
    loadDashboard();
    loadTrucks();
  });
});

// ========================= TAB SWITCHING =========================
function switchTab(name) {
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.tab').forEach(t => t.classList.toggle('active', t.id === 'tab-' + name));

  // Lazy-load tab data
  if (name === 'dashboard') loadDashboard();
  if (name === 'trucks')    loadTrucks();
  if (name === 'trips')     loadTrips();
  if (name === 'parts')     loadParts();
  if (name === 'staff')     loadStaff();
  if (name === 'misc')      loadMisc();
  if (name === 'bank')      loadBank();
  if (name === 'report')    loadReport();
}

// ========================= API HELPERS =========================
async function api(path, opts = {}) {
  const res = await fetch('/api' + path, {
    headers: { 'Content-Type': 'application/json' },
    ...opts
  });
  if (!res.ok) {
    const msg = await res.text();
    throw new Error(msg || res.statusText);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

async function loadConstants() {
  constants = await api('/constants');
  // Populate route datalist
  const dl = document.getElementById('route-list');
  dl.innerHTML = '';
  constants.routes.forEach(r => {
    const opt = document.createElement('option'); opt.value = r; dl.appendChild(opt);
  });
  // Populate misc category datalist
  const cdl = document.getElementById('cat-list');
  cdl.innerHTML = '';
  constants.miscCategories.forEach(c => {
    const opt = document.createElement('option'); opt.value = c; cdl.appendChild(opt);
  });
}

// ========================= DASHBOARD =========================
async function loadDashboard() {
  try {
    const d = await api('/dashboard');
    document.getElementById('d-trucks').textContent = d.truckCount;
    document.getElementById('d-trips').textContent  = d.tripCount;
    document.getElementById('d-income').textContent = ksh(d.monthIncome);
    const totalCosts = d.monthTripCost + d.monthParts + d.monthMisc + d.monthAuthorisedPayroll;
    document.getElementById('d-costs').textContent  = ksh(totalCosts);
    document.getElementById('d-banked').textContent = ksh(d.monthBanked);
    document.getElementById('d-net').textContent    = ksh(d.monthNet);

    const netCard = document.getElementById('d-net-card');
    netCard.classList.remove('green', 'red');
    netCard.classList.add(d.monthNet >= 0 ? 'green' : 'red');

    document.getElementById('d-b-income').textContent   = ksh(d.monthIncome);
    document.getElementById('d-b-tripcost').textContent = ksh(d.monthTripCost);
    document.getElementById('d-b-parts').textContent    = ksh(d.monthParts);
    document.getElementById('d-b-misc').textContent     = ksh(d.monthMisc);
    document.getElementById('d-b-payroll').textContent  = ksh(d.monthAuthorisedPayroll);
    document.getElementById('d-b-pending').textContent  = ksh(d.monthPendingPayroll);
    const netEl = document.getElementById('d-b-net');
    netEl.textContent = ksh(d.monthNet);
    netEl.className = 'money ' + (d.monthNet >= 0 ? 'pos' : 'neg');

    // Pending payroll notice
    const notice = document.getElementById('payroll-notice');
    const noticeText = document.getElementById('payroll-notice-text');
    if (d.monthPendingPayroll > 0) {
      noticeText.textContent = `Payroll of ${ksh(d.monthPendingPayroll)} is pending for ${d.currentMonth} — not yet deducted from net.`;
      notice.style.display = 'flex';
      // Pre-fill payroll modal
      document.getElementById('pr-month').value = d.currentMonth;
      document.getElementById('payroll-modal-desc').textContent =
        `This will authorise and deduct ${ksh(d.monthPendingPayroll)} in staff salaries for ${d.currentMonth}.`;
    } else {
      notice.style.display = 'none';
    }

    // Alerts
    const alertsDiv = document.getElementById('alerts-list');
    if (!d.alerts || d.alerts.length === 0) {
      alertsDiv.innerHTML = '<p class="muted">No alerts — all trucks are in order.</p>';
    } else {
      alertsDiv.innerHTML = d.alerts.map(a => {
        const overdue = a.days < 0;
        const soon    = a.days >= 0 && a.days <= 7;
        const icon    = overdue ? '🔴' : soon ? '🟠' : '🟡';
        let msg;
        if (a.type === 'service') {
          msg = overdue
            ? `<strong>${a.plate}</strong> service OVERDUE by ${-a.days} days (was ${a.date})`
            : `<strong>${a.plate}</strong> service due in ${a.days} days (${a.date})`;
        } else {
          msg = overdue
            ? `<strong>${a.plate}</strong> insurance EXPIRED ${-a.days} days ago (${a.date})`
            : `<strong>${a.plate}</strong> insurance expires in ${a.days} days (${a.date})`;
        }
        return `<div class="alert-item"><span class="alert-icon">${icon}</span><span>${msg}</span></div>`;
      }).join('');
    }
  } catch (e) {
    showToast('Could not load dashboard: ' + e.message, 'error');
  }
}

// ========================= TRUCKS =========================
async function loadTrucks() {
  try {
    trucks = await api('/trucks');
    const tbody = document.getElementById('trucks-tbody');
    if (!trucks.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No trucks yet. Add your first truck.</td></tr>';
      return;
    }
    tbody.innerHTML = trucks.map(t => {
      const badge = statusBadge(t.serviceDaysLeft, t.insuranceDaysLeft);
      return `<tr>
        <td><strong>${esc(t.plate)}</strong></td>
        <td>${esc(t.model)}</td>
        <td>${esc(t.driver)}</td>
        <td>${t.nextService}</td>
        <td>${t.insuranceExpiry}</td>
        <td>${badge}</td>
        <td>
          <button class="btn-icon" title="Edit dates" onclick="openEditTruck('${esc(t.plate)}')">✏️</button>
          <button class="btn-icon" title="Delete" onclick="confirmDelete('Truck ${esc(t.plate)}?', deleteTruck, '${esc(t.plate)}')">🗑️</button>
        </td>
      </tr>`;
    }).join('');

    // Refresh truck dropdowns
    refreshTruckCombos();
  } catch (e) {
    showToast('Could not load trucks: ' + e.message, 'error');
  }
}

function statusBadge(svcDays, insDays) {
  const worst = Math.min(svcDays, insDays);
  if (worst < 0)  return '<span class="badge badge-danger">Overdue</span>';
  if (worst <= 7) return '<span class="badge badge-danger">Critical</span>';
  if (worst <= 30) return '<span class="badge badge-warn">Due Soon</span>';
  return '<span class="badge badge-ok">OK</span>';
}

function refreshTruckCombos() {
  const ids = ['tr-plate', 'p-plate'];
  ids.forEach(id => {
    const sel = document.getElementById(id);
    const cur = sel.value;
    sel.innerHTML = trucks.map(t => `<option value="${esc(t.plate)}">${esc(t.plate)}</option>`).join('');
    if (cur) sel.value = cur;
  });
  // Misc truck combo has a "General" option
  const mp = document.getElementById('m-plate');
  mp.innerHTML = '<option value="">(General / whole fleet)</option>' +
    trucks.map(t => `<option value="${esc(t.plate)}">${esc(t.plate)}</option>`).join('');
}

async function submitTruck(e) {
  e.preventDefault();
  const body = {
    plate:          document.getElementById('t-plate').value,
    model:          document.getElementById('t-model').value,
    driver:         document.getElementById('t-driver').value,
    nextService:    document.getElementById('t-service').value,
    insuranceExpiry:document.getElementById('t-insurance').value
  };
  try {
    await api('/trucks', { method: 'POST', body: JSON.stringify(body) });
    closeModal('truck-modal');
    document.getElementById('truck-form').reset();
    await loadTrucks();
    showToast('Truck added', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

function openEditTruck(plate) {
  const t = trucks.find(x => x.plate === plate);
  if (!t) return;
  document.getElementById('te-plate').value    = t.plate;
  document.getElementById('te-service').value  = t.nextService;
  document.getElementById('te-insurance').value = t.insuranceExpiry;
  openModal('truck-edit-modal');
}

async function submitTruckEdit(e) {
  e.preventDefault();
  const plate = document.getElementById('te-plate').value;
  const body = {
    nextService:    document.getElementById('te-service').value,
    insuranceExpiry:document.getElementById('te-insurance').value
  };
  try {
    await api('/trucks/' + encodeURIComponent(plate), { method: 'PUT', body: JSON.stringify(body) });
    closeModal('truck-edit-modal');
    await loadTrucks();
    showToast('Truck updated', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function deleteTruck(plate) {
  try {
    await api('/trucks/' + encodeURIComponent(plate), { method: 'DELETE' });
    await loadTrucks();
    showToast('Truck deleted', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ========================= TRIPS =========================
async function loadTrips() {
  try {
    const trips = await api('/trips');
    const tbody = document.getElementById('trips-tbody');
    if (!trips.length) {
      tbody.innerHTML = '<tr><td colspan="13" class="empty-state">No trips recorded yet.</td></tr>';
      return;
    }
    tbody.innerHTML = trips.map(t => `<tr>
      <td>${t.id}</td>
      <td>${t.date}</td>
      <td>${esc(t.plate)}</td>
      <td>${esc(t.route)}</td>
      <td>${esc(t.cargo)}</td>
      <td>${t.mileage ? t.mileage + ' km' : '—'}</td>
      <td class="money">${fmt(t.income)}</td>
      <td class="money">${fmt(t.fuel)}</td>
      <td class="money">${fmt(t.borderAndTolls)}</td>
      <td class="money">${fmt(t.broker)}</td>
      <td class="money" title="${esc(t.otherDesc || '')}">${fmt(t.other)}${t.otherDesc ? ' <small>(' + esc(t.otherDesc) + ')</small>' : ''}</td>
      <td class="money ${t.profit >= 0 ? 'pos' : 'neg'}">${fmt(t.profit)}</td>
      <td><button class="btn-icon" title="Delete" onclick="confirmDelete('Trip #${t.id}?', deleteTrip, ${t.id})">🗑️</button></td>
    </tr>`).join('');
  } catch (e) {
    showToast('Could not load trips: ' + e.message, 'error');
  }
}

async function submitTrip(e) {
  e.preventDefault();
  const body = {
    plate:         document.getElementById('tr-plate').value,
    route:         document.getElementById('tr-route').value,
    cargo:         document.getElementById('tr-cargo').value,
    date:          document.getElementById('tr-date').value,
    mileage:       parseInt(document.getElementById('tr-mileage').value) || 0,
    income:        parseFloat(document.getElementById('tr-income').value) || 0,
    fuel:          parseFloat(document.getElementById('tr-fuel').value) || 0,
    borderAndTolls:parseFloat(document.getElementById('tr-border').value) || 0,
    broker:        parseFloat(document.getElementById('tr-broker').value) || 0,
    other:         parseFloat(document.getElementById('tr-other').value) || 0,
    otherDesc:     document.getElementById('tr-otherdesc').value.trim()
  };
  try {
    await api('/trips', { method: 'POST', body: JSON.stringify(body) });
    closeModal('trip-modal');
    document.getElementById('trip-form').reset();
    await loadTrips();
    showToast('Trip recorded', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function deleteTrip(id) {
  try {
    await api('/trips/' + id, { method: 'DELETE' });
    await loadTrips();
    showToast('Trip deleted', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ========================= SPARE PARTS =========================
async function loadParts() {
  try {
    const parts = await api('/parts');
    const tbody = document.getElementById('parts-tbody');
    if (!parts.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No spare parts recorded yet.</td></tr>';
      return;
    }
    tbody.innerHTML = parts.map(p => `<tr>
      <td>${p.date}</td>
      <td>${esc(p.plate)}</td>
      <td>${esc(p.name)}</td>
      <td>${p.qty}</td>
      <td class="money">${fmt(p.cost)}</td>
      <td><button class="btn-icon" title="Delete" onclick="confirmDelete('this spare part entry?', deletePart, ${p.id})">🗑️</button></td>
    </tr>`).join('');
  } catch (e) {
    showToast('Could not load parts: ' + e.message, 'error');
  }
}

async function submitPart(e) {
  e.preventDefault();
  const body = {
    plate: document.getElementById('p-plate').value,
    name:  document.getElementById('p-name').value,
    qty:   parseInt(document.getElementById('p-qty').value) || 1,
    cost:  parseFloat(document.getElementById('p-cost').value) || 0,
    date:  document.getElementById('p-date').value
  };
  try {
    await api('/parts', { method: 'POST', body: JSON.stringify(body) });
    closeModal('part-modal');
    document.getElementById('part-form').reset();
    await loadParts();
    showToast('Spare part added', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function deletePart(id) {
  try {
    await api('/parts/' + id, { method: 'DELETE' });
    await loadParts();
    showToast('Entry deleted', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ========================= STAFF =========================
async function loadStaff() {
  try {
    const staff = await api('/staff');
    const tbody = document.getElementById('staff-tbody');
    if (!staff.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No staff added yet.</td></tr>';
      document.getElementById('staff-total').textContent = '';
      return;
    }
    tbody.innerHTML = staff.map(s => `<tr>
      <td>${esc(s.name)}</td>
      <td>${esc(s.role)}</td>
      <td>${esc(s.idNumber) || '<span class="muted">—</span>'}</td>
      <td>${esc(s.licenseNumber) || '<span class="muted">—</span>'}</td>
      <td class="money">${fmt(s.monthlySalary)}</td>
      <td><button class="btn-icon" title="Delete" onclick="confirmDelete('${esc(s.name)} from staff?', deleteStaff, ${s.id})">🗑️</button></td>
    </tr>`).join('');
    const total = staff.reduce((sum, s) => sum + s.monthlySalary, 0);
    document.getElementById('staff-total').textContent = 'Total Monthly Payroll: ' + ksh(total);
  } catch (e) {
    showToast('Could not load staff: ' + e.message, 'error');
  }
}

async function submitStaff(e) {
  e.preventDefault();
  const body = {
    name:          document.getElementById('s-name').value,
    role:          document.getElementById('s-role').value,
    idNumber:      document.getElementById('s-idnumber').value.trim(),
    licenseNumber: document.getElementById('s-license').value.trim(),
    monthlySalary: parseFloat(document.getElementById('s-salary').value) || 0
  };
  try {
    await api('/staff', { method: 'POST', body: JSON.stringify(body) });
    closeModal('staff-modal');
    document.getElementById('staff-form').reset();
    await loadStaff();
    showToast('Staff member added', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function deleteStaff(id) {
  try {
    await api('/staff/' + id, { method: 'DELETE' });
    await loadStaff();
    showToast('Staff member removed', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ========================= MISC EXPENSES =========================
async function loadMisc() {
  try {
    const misc = await api('/misc');
    const tbody = document.getElementById('misc-tbody');
    if (!misc.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No miscellaneous expenses recorded yet.</td></tr>';
      return;
    }
    tbody.innerHTML = misc.map(x => `<tr>
      <td>${x.date}</td>
      <td>${esc(x.category)}</td>
      <td>${esc(x.description)}</td>
      <td>${x.plate ? esc(x.plate) : '<em class="muted">General</em>'}</td>
      <td class="money">${fmt(x.amount)}</td>
      <td><button class="btn-icon" title="Delete" onclick="confirmDelete('this expense?', deleteMisc, ${x.id})">🗑️</button></td>
    </tr>`).join('');
  } catch (e) {
    showToast('Could not load misc expenses: ' + e.message, 'error');
  }
}

async function submitMisc(e) {
  e.preventDefault();
  const body = {
    category:    document.getElementById('m-cat').value,
    description: document.getElementById('m-desc').value,
    plate:       document.getElementById('m-plate').value,
    amount:      parseFloat(document.getElementById('m-amount').value) || 0,
    date:        document.getElementById('m-date').value
  };
  try {
    await api('/misc', { method: 'POST', body: JSON.stringify(body) });
    closeModal('misc-modal');
    document.getElementById('misc-form').reset();
    await loadMisc();
    showToast('Expense added', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function deleteMisc(id) {
  try {
    await api('/misc/' + id, { method: 'DELETE' });
    await loadMisc();
    showToast('Expense deleted', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ========================= MONTHLY REPORT =========================
const MONTHS = ['January','February','March','April','May','June',
                'July','August','September','October','November','December'];

function initReportControls() {
  const ms = document.getElementById('report-month');
  MONTHS.forEach((m, i) => {
    const o = document.createElement('option');
    o.value = i + 1; o.textContent = m; ms.appendChild(o);
  });
  ms.value = new Date().getMonth() + 1;

  const ys = document.getElementById('report-year');
  const yr = new Date().getFullYear();
  for (let y = yr - 5; y <= yr + 1; y++) {
    const o = document.createElement('option'); o.value = y; o.textContent = y; ys.appendChild(o);
  }
  ys.value = yr;
}

async function loadReport() {
  const year  = document.getElementById('report-year').value;
  const month = document.getElementById('report-month').value;
  const monthName = MONTHS[month - 1];
  const el = document.getElementById('report-content');
  el.innerHTML = '<p class="muted">Loading…</p>';
  try {
    const r = await api(`/report?year=${year}&month=${month}`);
    const net = r.net;

    el.innerHTML = `
      <div class="report-section">
        <h3>Summary — ${monthName} ${year}</h3>
        <div class="summary-grid">
          <div class="summary-item"><div class="label">Trip Income</div><div class="value">${ksh(r.income)}</div></div>
          <div class="summary-item"><div class="label">Trip Expenses</div><div class="value">${ksh(r.tripCost)}</div></div>
          <div class="summary-item"><div class="label">Spare Parts</div><div class="value">${ksh(r.parts)}</div></div>
          <div class="summary-item"><div class="label">Miscellaneous</div><div class="value">${ksh(r.misc)}</div></div>
          <div class="summary-item"><div class="label">Authorised Payroll</div><div class="value">${ksh(r.payroll)}</div></div>
          <div class="summary-item"><div class="label">Total Banked</div><div class="value">${ksh(r.banked||0)}</div></div>
          <div class="summary-item ${net >= 0 ? 'highlight' : 'highlight-neg'}">
            <div class="label">NET PROFIT / (LOSS)</div>
            <div class="value">${ksh(net)}</div>
          </div>
        </div>
      </div>

      <div class="report-section">
        <h3>Per Truck</h3>
        ${r.trucks.length === 0 ? '<p class="muted">No trucks.</p>' : `
        <div class="table-wrap">
          <table>
            <thead><tr><th>Truck</th><th>Trips</th><th class="money">Income</th><th class="money">Costs</th><th class="money">Profit</th></tr></thead>
            <tbody>
              ${r.trucks.map(t => `<tr>
                <td>${esc(t.plate)}</td><td>${t.trips}</td>
                <td class="money">${fmt(t.income)}</td>
                <td class="money">${fmt(t.cost)}</td>
                <td class="money ${t.profit >= 0 ? 'pos' : 'neg'}">${fmt(t.profit)}</td>
              </tr>`).join('')}
            </tbody>
          </table>
        </div>`}
      </div>

      <div class="report-section">
        <h3>Trips</h3>
        ${r.trips.length === 0 ? '<p class="muted">No trips this month.</p>' : `
        <div class="table-wrap">
          <table>
            <thead><tr><th>Date</th><th>Truck</th><th>Route</th><th>Cargo</th><th>km</th><th class="money">Income</th><th class="money">Fuel</th><th class="money">Border/Tolls</th><th class="money">Broker</th><th class="money">Other</th><th class="money">Profit</th></tr></thead>
            <tbody>
              ${r.trips.map(t => `<tr>
                <td>${t.date}</td><td>${esc(t.plate)}</td><td>${esc(t.route)}</td><td>${esc(t.cargo)}</td>
                <td>${t.mileage ? t.mileage + ' km' : '—'}</td>
                <td class="money">${fmt(t.income)}</td>
                <td class="money">${fmt(t.fuel||0)}</td>
                <td class="money">${fmt(t.borderAndTolls||0)}</td>
                <td class="money">${fmt(t.broker||0)}</td>
                <td class="money" title="${esc(t.otherDesc||'')}">${fmt(t.other)}${t.otherDesc ? '<br><small class="muted">'+esc(t.otherDesc)+'</small>' : ''}</td>
                <td class="money ${t.profit >= 0 ? 'pos' : 'neg'}">${fmt(t.profit)}</td>
              </tr>`).join('')}
            </tbody>
          </table>
        </div>`}
      </div>

      <div class="report-section">
        <h3>Spare Parts</h3>
        ${r.parts.length === 0 ? '<p class="muted">None.</p>' : `
        <div class="table-wrap">
          <table>
            <thead><tr><th>Date</th><th>Truck</th><th>Part</th><th>Qty</th><th class="money">Cost</th></tr></thead>
            <tbody>
              ${r.parts.map(p => `<tr>
                <td>${p.date}</td><td>${esc(p.plate)}</td><td>${esc(p.name)}</td>
                <td>${p.qty}</td><td class="money">${fmt(p.cost)}</td>
              </tr>`).join('')}
            </tbody>
          </table>
        </div>`}
      </div>

      <div class="report-section">
        <h3>Miscellaneous Expenses</h3>
        ${r.misc.length === 0 ? '<p class="muted">None.</p>' : `
        <div class="table-wrap">
          <table>
            <thead><tr><th>Date</th><th>Category</th><th>Description</th><th>Truck</th><th class="money">Amount</th></tr></thead>
            <tbody>
              ${r.misc.map(x => `<tr>
                <td>${x.date}</td><td>${esc(x.category)}</td><td>${esc(x.description)}</td>
                <td>${x.plate || '<em>General</em>'}</td><td class="money">${fmt(x.amount)}</td>
              </tr>`).join('')}
            </tbody>
          </table>
        </div>`}
      </div>

      <div class="report-section">
        <h3>Payroll</h3>
        ${r.staff.length === 0 ? '<p class="muted">No staff.</p>' : `
        <div class="table-wrap">
          <table>
            <thead><tr><th>Name</th><th>Role</th><th>ID No.</th><th>Licence No.</th><th class="money">Monthly Salary</th></tr></thead>
            <tbody>
              ${r.staff.map(s => `<tr>
                <td>${esc(s.name)}</td><td>${esc(s.role)}</td>
                <td>${esc(s.idNumber) || '—'}</td>
                <td>${esc(s.licenseNumber) || '—'}</td>
                <td class="money">${fmt(s.monthlySalary)}</td>
              </tr>`).join('')}
            </tbody>
          </table>
        </div>`}
      </div>
    `;
    // Store report data for CSV export
    window._lastReport = r;
    window._lastReportLabel = `${monthName} ${year}`;
  } catch (e) {
    el.innerHTML = '<p class="muted">Could not load report.</p>';
    showToast(e.message, 'error');
  }
}

function exportReportCsv() {
  const r = window._lastReport;
  if (!r) { showToast('Generate a report first', 'error'); return; }
  const label = window._lastReportLabel || 'Report';
  let csv = `NGUMBA LOGISTICS MONTHLY REPORT,${label}\n\nSUMMARY\nItem,Amount (KSh)\n`;
  csv += `Trip income,${r.income}\nTrip expenses,${r.tripCost}\nSpare parts,${r.parts}\n`;
  csv += `Miscellaneous,${r.misc}\nStaff payroll,${r.payroll}\nNet profit/(loss),${r.net}\n\n`;

  csv += 'TRIPS\nDate,Truck,Route,Cargo,Mileage(km),Income,Fuel,Border/Tolls,Broker,Other,Other Description,Profit\n';
  r.trips.forEach(t => { csv += `${t.date},${q(t.plate)},${q(t.route)},${q(t.cargo)},${t.mileage||0},${t.income},${t.fuel||0},${t.borderAndTolls||0},${t.broker||0},${t.other},${q(t.otherDesc||'')},${t.profit}\n`; });

  csv += '\nSPARE PARTS\nDate,Truck,Part,Qty,Cost\n';
  r.parts.forEach(p => { csv += `${p.date},${q(p.plate)},${q(p.name)},${p.qty},${p.cost}\n`; });

  csv += '\nMISCELLANEOUS\nDate,Category,Description,Truck,Amount\n';
  r.misc.forEach(x => { csv += `${x.date},${q(x.category)},${q(x.description)},${q(x.plate)},${x.amount}\n`; });

  csv += '\nPAYROLL\nName,Role,ID Number,Licence Number,Monthly Salary\n';
  r.staff.forEach(s => { csv += `${q(s.name)},${q(s.role)},${q(s.idNumber)},${q(s.licenseNumber)},${s.monthlySalary}\n`; });

  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url  = URL.createObjectURL(blob);
  const a    = document.createElement('a');
  a.href = url; a.download = `Ngumba_Report_${label.replace(' ', '_')}.csv`;
  a.click(); URL.revokeObjectURL(url);
  showToast('CSV downloaded', 'success');
}

function q(s) { return '"' + (s || '').replace(/"/g, '""') + '"'; }

// ========================= PDF EXPORT =========================
function exportReportPdf() {
  const r = window._lastReport;
  if (!r) { showToast('Generate a report first', 'error'); return; }
  const label = window._lastReportLabel || 'Report';

  const kshP = v => 'KSh ' + Number(v).toLocaleString('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const fmtP = v => Number(v).toLocaleString('en-KE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const escP = s => s ? String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') : '';

  const net = r.net;
  const netClass = net >= 0 ? 'pos' : 'neg';

  // Build a self-contained HTML page styled for printing / PDF
  const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Ngumba Logistics — ${escP(label)}</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: Arial, sans-serif; font-size: 11px; color: #1a1a1a; padding: 20px; }
  h1 { font-size: 18px; margin-bottom: 2px; }
  .sub { font-size: 11px; color: #555; margin-bottom: 18px; }
  h2 { font-size: 13px; margin: 18px 0 6px; border-bottom: 2px solid #0a7d3b; padding-bottom: 4px; color: #0a7d3b; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 6px; }
  th { background: #f0f0f0; padding: 5px 7px; text-align: left; font-size: 10px; text-transform: uppercase; letter-spacing: .04em; border: 1px solid #ccc; }
  td { padding: 5px 7px; border: 1px solid #ddd; vertical-align: top; }
  .r { text-align: right; }
  .pos { color: #0a7d3b; font-weight: 700; }
  .neg { color: #c0392b; font-weight: 700; }
  .total td { font-weight: 700; background: #fafafa; border-top: 2px solid #aaa; }
  .summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 6px; }
  .summary-item { border: 1px solid #ddd; border-radius: 4px; padding: 8px 10px; }
  .summary-item .lbl { font-size: 9px; text-transform: uppercase; color: #666; margin-bottom: 3px; }
  .summary-item .val { font-size: 13px; font-weight: 700; }
  .highlight { background: #e8f5ee; border-color: #0a7d3b; }
  .highlight .val { color: #0a7d3b; }
  .highlight-neg { background: #fdecea; border-color: #c0392b; }
  .highlight-neg .val { color: #c0392b; }
  .muted { color: #999; font-style: italic; }
  small { font-size: 9px; color: #777; }
  @media print {
    body { padding: 10px; }
    @page { margin: 15mm; size: A4; }
  }
</style>
</head>
<body>
<h1>NGUMBA LOGISTICS</h1>
<div class="sub">Monthly Report — ${escP(label)} &nbsp;|&nbsp; Generated ${new Date().toLocaleDateString('en-KE', {day:'2-digit',month:'long',year:'numeric'})}</div>

<h2>Summary</h2>
<div class="summary-grid">
  <div class="summary-item"><div class="lbl">Trip Income</div><div class="val">${kshP(r.income)}</div></div>
  <div class="summary-item"><div class="lbl">Trip Expenses</div><div class="val">${kshP(r.tripCost)}</div></div>
  <div class="summary-item"><div class="lbl">Spare Parts</div><div class="val">${kshP(r.parts)}</div></div>
  <div class="summary-item"><div class="lbl">Miscellaneous</div><div class="val">${kshP(r.misc)}</div></div>
  <div class="summary-item"><div class="lbl">Authorised Payroll</div><div class="val">${kshP(r.payroll)}</div></div>
  <div class="summary-item"><div class="lbl">Total Banked</div><div class="val">${kshP(r.banked || 0)}</div></div>
  <div class="summary-item ${net >= 0 ? 'highlight' : 'highlight-neg'}"><div class="lbl">Net Profit / (Loss)</div><div class="val ${netClass}">${kshP(net)}</div></div>
</div>

<h2>Per Truck</h2>
${r.trucks.length === 0 ? '<p class="muted">No trucks.</p>' : `
<table>
  <thead><tr><th>Truck</th><th>Trips</th><th class="r">Income</th><th class="r">Costs</th><th class="r">Profit</th></tr></thead>
  <tbody>
    ${r.trucks.map(t => `<tr>
      <td>${escP(t.plate)}</td><td class="r">${t.trips}</td>
      <td class="r">${fmtP(t.income)}</td><td class="r">${fmtP(t.cost)}</td>
      <td class="r ${t.profit >= 0 ? 'pos' : 'neg'}">${fmtP(t.profit)}</td>
    </tr>`).join('')}
  </tbody>
</table>`}

<h2>Trips</h2>
${r.trips.length === 0 ? '<p class="muted">No trips this month.</p>' : `
<table>
  <thead><tr><th>Date</th><th>Truck</th><th>Route</th><th>Cargo</th><th class="r">km</th><th class="r">Income</th><th class="r">Fuel</th><th class="r">Border/Tolls</th><th class="r">Broker</th><th class="r">Other</th><th class="r">Profit</th></tr></thead>
  <tbody>
    ${r.trips.map(t => `<tr>
      <td>${t.date}</td><td>${escP(t.plate)}</td><td>${escP(t.route)}</td><td>${escP(t.cargo)}</td>
      <td class="r">${t.mileage ? t.mileage : '—'}</td>
      <td class="r">${fmtP(t.income)}</td>
      <td class="r">${fmtP(t.fuel || 0)}</td>
      <td class="r">${fmtP(t.borderAndTolls || 0)}</td>
      <td class="r">${fmtP(t.broker || 0)}</td>
      <td class="r">${fmtP(t.other)}${t.otherDesc ? '<br><small>' + escP(t.otherDesc) + '</small>' : ''}</td>
      <td class="r ${t.profit >= 0 ? 'pos' : 'neg'}">${fmtP(t.profit)}</td>
    </tr>`).join('')}
  </tbody>
</table>`}

<h2>Spare Parts</h2>
${r.parts.length === 0 ? '<p class="muted">None.</p>' : `
<table>
  <thead><tr><th>Date</th><th>Truck</th><th>Part</th><th class="r">Qty</th><th class="r">Cost</th></tr></thead>
  <tbody>
    ${r.parts.map(p => `<tr>
      <td>${p.date}</td><td>${escP(p.plate)}</td><td>${escP(p.name)}</td>
      <td class="r">${p.qty}</td><td class="r">${fmtP(p.cost)}</td>
    </tr>`).join('')}
  </tbody>
</table>`}

<h2>Miscellaneous Expenses</h2>
${r.misc.length === 0 ? '<p class="muted">None.</p>' : `
<table>
  <thead><tr><th>Date</th><th>Category</th><th>Description</th><th>Truck</th><th class="r">Amount</th></tr></thead>
  <tbody>
    ${r.misc.map(x => `<tr>
      <td>${x.date}</td><td>${escP(x.category)}</td><td>${escP(x.description)}</td>
      <td>${x.plate || 'General'}</td><td class="r">${fmtP(x.amount)}</td>
    </tr>`).join('')}
  </tbody>
</table>`}

<h2>Payroll</h2>
${r.staff.length === 0 ? '<p class="muted">No staff.</p>' : `
<table>
  <thead><tr><th>Name</th><th>Role</th><th>ID No.</th><th>Licence No.</th><th class="r">Monthly Salary</th></tr></thead>
  <tbody>
    ${r.staff.map(s => `<tr>
      <td>${escP(s.name)}</td><td>${escP(s.role)}</td>
      <td>${escP(s.idNumber) || '—'}</td><td>${escP(s.licenseNumber) || '—'}</td>
      <td class="r">${fmtP(s.monthlySalary)}</td>
    </tr>`).join('')}
    <tr class="total"><td colspan="4">Total Payroll</td><td class="r">${kshP(r.payroll)}</td></tr>
  </tbody>
</table>`}

<script>window.onload = function() { window.print(); }</script>
</body>
</html>`;

  const blob = new Blob([html], { type: 'text/html;charset=utf-8;' });
  const url  = URL.createObjectURL(blob);
  const win  = window.open(url, '_blank');
  if (!win) {
    showToast('Allow pop-ups to export PDF', 'error');
  } else {
    showToast('Print dialog opening — choose "Save as PDF"', 'success');
  }
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}

// ========================= BANK DEPOSITS =========================
async function loadBank() {
  try {
    const deps = await api('/bank');
    const tbody = document.getElementById('bank-tbody');
    if (!deps.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No bank deposits recorded yet.</td></tr>';
      document.getElementById('bank-total').textContent = '';
      return;
    }
    tbody.innerHTML = deps.map(d => `<tr>
      <td>${d.date}</td>
      <td>${esc(d.bank)}</td>
      <td>${esc(d.reference) || '<span class="muted">—</span>'}</td>
      <td class="money">${fmt(d.amount)}</td>
      <td><button class="btn-icon" title="Delete" onclick="confirmDelete('this deposit?', deleteBank, ${d.id})">🗑️</button></td>
    </tr>`).join('');
    const total = deps.reduce((s, d) => s + d.amount, 0);
    document.getElementById('bank-total').textContent = 'Total Banked: ' + ksh(total);
  } catch (e) {
    showToast('Could not load bank deposits: ' + e.message, 'error');
  }
}

async function submitBank(e) {
  e.preventDefault();
  const body = {
    date:      document.getElementById('b-date').value,
    bank:      document.getElementById('b-bank').value.trim(),
    reference: document.getElementById('b-ref').value.trim(),
    amount:    parseFloat(document.getElementById('b-amount').value) || 0
  };
  try {
    await api('/bank', { method: 'POST', body: JSON.stringify(body) });
    closeModal('bank-modal');
    document.getElementById('bank-form').reset();
    await loadBank();
    showToast('Deposit recorded', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function deleteBank(id) {
  try {
    await api('/bank/' + id, { method: 'DELETE' });
    await loadBank();
    showToast('Deposit deleted', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ========================= PAYROLL RUNS =========================
async function submitPayrollRun(e) {
  e.preventDefault();
  const body = {
    month:        document.getElementById('pr-month').value,
    authorisedBy: document.getElementById('pr-auth').value.trim()
  };
  try {
    await api('/payroll/run', { method: 'POST', body: JSON.stringify(body) });
    closeModal('payroll-modal');
    document.getElementById('payroll-form').reset();
    await loadDashboard();
    showToast('Payroll authorised and deducted', 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ========================= MODAL HELPERS =========================
function openModal(id) {
  // Set default dates on modal open
  const dateFields = {
    'truck-modal': ['t-service', 't-insurance'],
    'trip-modal':  ['tr-date'],
    'part-modal':  ['p-date'],
    'misc-modal':  ['m-date']
  };
  if (dateFields[id]) {
    dateFields[id].forEach(f => {
      const el = document.getElementById(f);
      if (el && !el.value) el.value = today();
    });
  }
  // Default service/insurance dates for new truck
  if (id === 'truck-modal') {
    const svc = document.getElementById('t-service');
    const ins = document.getElementById('t-insurance');
    if (!svc.value) {
      const d = new Date(); d.setMonth(d.getMonth() + 3);
      svc.value = d.toISOString().slice(0, 10);
    }
    if (!ins.value) {
      const d = new Date(); d.setFullYear(d.getFullYear() + 1);
      ins.value = d.toISOString().slice(0, 10);
    }
  }
  // Ensure truck combos are populated
  if (['trip-modal', 'part-modal', 'misc-modal'].includes(id)) {
    refreshTruckCombos();
  }
  document.getElementById(id).classList.add('open');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}

// ========================= CONFIRM DELETE =========================
function confirmDelete(msg, fn, arg) {
  document.getElementById('confirm-msg').textContent = 'Delete ' + msg;
  const btn = document.getElementById('confirm-yes');
  btn.onclick = () => { closeModal('confirm-modal'); fn(arg); };
  openModal('confirm-modal');
}

// ========================= TOAST =========================
let _toastTimer;
function showToast(msg, type = 'info') {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = 'toast show ' + type;
  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => { el.classList.remove('show'); }, 3000);
}

// ========================= SECURITY HELPER =========================
function esc(s) {
  if (!s) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
