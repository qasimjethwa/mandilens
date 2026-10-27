/* MandiLens demo app — loads data.json, renders chart + stats + model table. */
"use strict";

const CROPS = ["onion", "tomato", "potato"];
const MANDIS = ["Vashi", "Lasalgaon", "Azadpur"];
const CROP_LABEL = { onion: "Onion", tomato: "Tomato", potato: "Potato" };

let DATA = null;      // { "onion@Vashi": {dates:[], prices:[]} }
let METRICS = null;   // java-side metrics.json (cross-check)
let crop = "onion", mandi = "Vashi";

const $ = (id) => document.getElementById(id);
const inr = (n) => "₹" + Math.round(n).toLocaleString("en-IN");
const fmtDate = (iso) => {
  const d = new Date(iso + "T00:00:00");
  return d.toLocaleDateString("en-IN", { day: "numeric", month: "short" });
};

function buildControls() {
  $("cropPills").innerHTML = CROPS.map((c) =>
    `<button class="pill${c === crop ? " on" : ""}" data-crop="${c}">${CROP_LABEL[c]}</button>`).join("");
  $("cropPills").querySelectorAll("button").forEach((b) =>
    b.addEventListener("click", () => { crop = b.dataset.crop; buildControls(); render(); }));
  $("mandiSel").innerHTML = MANDIS.map((m) => `<option${m === mandi ? " selected" : ""}>${m}</option>`).join("");
  $("mandiSel").onchange = (e) => { mandi = e.target.value; render(); };
}

function render() {
  const key = `${crop}@${mandi}`;
  const s = DATA[key];
  if (!s) return;
  const f = forecast7(s.prices, s.dates);
  const rows = MODEL_KEYS.map((k) => ({ name: Forecasters[k].name, ...backtest(Forecasters[k], s.prices) }))
    .sort((a, b) => a.mape - b.mape);

  $("stToday").textContent = inr(f.today);
  $("stTodaySub").textContent = `₹ / quintal · ${fmtDate(s.dates[s.dates.length - 1])}`;
  if (f.gain >= 0) {
    $("stBest").textContent = fmtDate(f.bestDate);
    $("stBestSub").textContent = `${inr(f.bestPrice)} / quintal`;
    $("stGain").textContent = "+" + inr(f.gain);
    $("stGain").style.color = "var(--good)";
  } else {
    $("stBest").textContent = "Sell today";
    $("stBestSub").textContent = `best day ahead is ${inr(f.bestPrice)} — below today`;
    $("stGain").textContent = "−" + inr(-f.gain) + " if you wait";
    $("stGain").style.color = "var(--bad)";
  }
  $("stModel").textContent = f.model.name.replace(" (7d)", "").replace(" (30d)", "");
  $("stMape").textContent = `MAPE ${f.model.mape.toFixed(1)}% · backtested 21 days`;
  $("rangeNote").textContent = `${s.dates.length} days · ${fmtDate(s.dates[0])} → ${fmtDate(s.dates[s.dates.length - 1])}`;

  $("modelTable").innerHTML = rows.map((r, i) =>
    `<tr class="${i === 0 ? "win" : ""}"><td>${r.name}</td>
     <td class="num">${r.mape.toFixed(1)}%</td><td class="num">${Math.round(r.mae)}</td>
     <td class="num">${Math.round(r.rmse)}</td></tr>`).join("");

  if (METRICS) {
    const m = METRICS.series.find((x) => x.key === key);
    if (m) {
      const agree = Math.abs(m.mape - f.model.mape) < 0.05 && m.bestModel === f.model.name;
      $("javaCheck").innerHTML = agree
        ? `Java engine agrees: <b>${m.bestModel}</b>, MAPE ${m.mape.toFixed(1)}% — the browser runs the same models, bit-for-bit.`
        : `Java engine: <b>${m.bestModel}</b>, MAPE ${m.mape.toFixed(1)}%.`;
    }
  }
  drawChart(s, f);
}

function drawChart(s, f) {
  const cv = $("chart"), ctx = cv.getContext("2d");
  const W = cv.width, H = cv.height;
  ctx.clearRect(0, 0, W, H);
  const N = 60;
  const hist = s.prices.slice(-N);
  const histDates = s.dates.slice(-N);
  const all = [...hist, ...f.high];
  const lo = Math.min(...hist, ...f.low) * 0.97, hi = Math.max(...all) * 1.03;
  const padL = 70, padR = 24, padT = 26, padB = 34;
  const x = (i, total) => padL + (i / (total - 1)) * (W - padL - padR);
  const y = (v) => padT + (1 - (v - lo) / (hi - lo)) * (H - padT - padB);
  const total = N + 7;

  // gridlines + y labels
  ctx.font = "12px -apple-system, Segoe UI, sans-serif";
  ctx.fillStyle = "#A8A29E";
  for (let g = 0; g <= 4; g++) {
    const v = lo + ((hi - lo) * g) / 4;
    ctx.fillText("₹" + Math.round(v).toLocaleString("en-IN"), 8, y(v) + 4);
    ctx.strokeStyle = "#EFE8D6"; ctx.lineWidth = 1;
    ctx.beginPath(); ctx.moveTo(padL, y(v)); ctx.lineTo(W - padR, y(v)); ctx.stroke();
  }
  // x labels
  ctx.fillStyle = "#78716C";
  [0, 20, 40, 59].forEach((i) => ctx.fillText(fmtDate(histDates[i]), x(i, total) - 18, H - 10));
  f.dates.forEach((d, i) => ctx.fillText(fmtDate(d), x(N + i, total) - 18, H - 10));

  // 95% band
  ctx.beginPath();
  f.dates.forEach((_, i) => { const px = x(N + i, total), py = y(f.high[i]); i ? ctx.lineTo(px, py) : ctx.moveTo(px, py); });
  for (let i = 6; i >= 0; i--) ctx.lineTo(x(N + i, total), y(f.low[i]));
  ctx.closePath(); ctx.fillStyle = "rgba(233,160,59,0.22)"; ctx.fill();

  // history line
  ctx.beginPath();
  hist.forEach((v, i) => { const px = x(i, total), py = y(v); i ? ctx.lineTo(px, py) : ctx.moveTo(px, py); });
  ctx.strokeStyle = "#2D6A4F"; ctx.lineWidth = 3; ctx.lineJoin = "round"; ctx.stroke();

  // forecast line (dashed amber)
  ctx.beginPath();
  ctx.moveTo(x(N - 1, total), y(hist[N - 1]));
  f.point.forEach((v, i) => ctx.lineTo(x(N + i, total), y(v)));
  ctx.strokeStyle = "#E9A03B"; ctx.lineWidth = 3; ctx.setLineDash([7, 5]); ctx.stroke();
  ctx.setLineDash([]);

  // forecast points
  f.point.forEach((v, i) => {
    ctx.beginPath(); ctx.arc(x(N + i, total), y(v), 4.5, 0, 7);
    ctx.fillStyle = "#E9A03B"; ctx.fill(); ctx.strokeStyle = "#fff"; ctx.lineWidth = 2; ctx.stroke();
  });
  // today dot
  ctx.beginPath(); ctx.arc(x(N - 1, total), y(hist[N - 1]), 5.5, 0, 7);
  ctx.fillStyle = "#1B4332"; ctx.fill(); ctx.strokeStyle = "#fff"; ctx.lineWidth = 2; ctx.stroke();

  // best-day marker
  const bx = x(N + f.bestIdx, total), by = y(f.bestPrice);
  ctx.beginPath(); ctx.arc(bx, by, 9, 0, 7);
  ctx.fillStyle = "#C05621"; ctx.fill(); ctx.strokeStyle = "#fff"; ctx.lineWidth = 3; ctx.stroke();
  ctx.font = "bold 13px -apple-system, Segoe UI, sans-serif";
  ctx.fillStyle = "#C05621";
  const lbl = "BEST DAY · " + inr(f.bestPrice);
  const tw = ctx.measureText(lbl).width;
  const lx = Math.min(Math.max(bx - tw / 2 - 8, padL), W - padR - tw - 16);
  ctx.fillText(lbl, lx + 8, by - 18);
}

function parseCsv(text) {
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter((l) => l && !l.startsWith("#"));
  lines.shift(); // header
  const byKey = {};
  for (const line of lines) {
    const [c, m, d, p] = line.split(",").map((s) => s.trim());
    const price = parseFloat(p);
    if (!c || !m || !d || isNaN(price)) continue;
    const k = `${c.toLowerCase()}@${m}`;
    (byKey[k] = byKey[k] || { dates: [], prices: [] });
    byKey[k].dates.push(d); byKey[k].prices.push(price);
  }
  return byKey;
}

$("csvFile").addEventListener("change", (e) => {
  const file = e.target.files[0];
  if (!file) return;
  const rd = new FileReader();
  rd.onload = () => {
    const parsed = parseCsv(String(rd.result));
    const keys = Object.keys(parsed);
    if (!keys.length) { $("uploadMsg").textContent = "No valid rows found — need crop,mandi,date,price."; return; }
    DATA = parsed;
    const first = keys[0].split("@");
    crop = first[0]; mandi = first[1];
    if (!CROPS.includes(crop)) { CROPS.push(crop); CROP_LABEL[crop] = crop[0].toUpperCase() + crop.slice(1); }
    if (!MANDIS.includes(mandi)) MANDIS.push(mandi);
    buildControls(); render();
    $("uploadMsg").textContent = `Loaded ${keys.length} series from ${file.name} — forecasting on your data now.`;
  };
  rd.readAsText(file);
});

Promise.all([
  fetch("data.json").then((r) => r.json()),
  fetch("metrics.json").then((r) => r.json()).catch(() => null),
]).then(([d, m]) => {
  DATA = d.series; METRICS = m;
  buildControls(); render();
}).catch((err) => {
  document.querySelector(".wrap").insertAdjacentHTML("beforeend",
    `<p style="color:#B3261E;margin-top:20px">Demo data failed to load: ${err.message}</p>`);
});
