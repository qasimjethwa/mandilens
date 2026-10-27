#!/usr/bin/env python3
"""Generate data/sample_mandi_prices.csv — clearly-labelled ILLUSTRATIVE sample
data (not real market prices). Replace with a real Agmarknet export to forecast
on live data. 3 crops x 3 mandis x 120 days, ending 2026-10-26.

Price model per series: base level + slow trend + weekly mandi cycle
(weekend arrivals dip) + noise + one realistic shock event.
"""
import math, random
from datetime import date, timedelta

random.seed(20261027)
END = date(2026, 10, 26)
DAYS = 120

SERIES = [
    # crop, mandi, base Rs/quintal, trend/day, weekly amp, noise, shock(day, mult)
    ("onion",  "Vashi",    1850,  4.5, 130, 150, (88, 1.45)),   # late-Aug rain shock
    ("onion",  "Lasalgaon",1750,  4.0, 120, 140, (88, 1.55)),
    ("onion",  "Azadpur",  1950,  5.0, 140, 160, (88, 1.40)),
    ("tomato", "Vashi",    1250, -3.0, 180, 190, (52, 0.55)),   # glut crash mid-Sep
    ("tomato", "Lasalgaon",1150, -2.5, 170, 180, (52, 0.60)),
    ("tomato", "Azadpur",  1350, -3.5, 190, 200, (52, 0.50)),
    ("potato", "Vashi",    1150,  1.5,  60,  70, (30, 1.18)),
    ("potato", "Lasalgaon",1050,  1.2,  55,  65, (30, 1.15)),
    ("potato", "Azadpur",  1250,  1.8,  65,  75, (30, 1.20)),
]

rows = []
for crop, mandi, base, trend, wamp, noise, (shock_day, shock_mult) in SERIES:
    level = base
    for i in range(DAYS):
        d = END - timedelta(days=DAYS - 1 - i)
        # slow drift + weekly cycle (Sunday = day 6 dip) + noise
        weekly = wamp * math.sin(2 * math.pi * (d.weekday() + 1) / 7)
        shock = 1.0
        if shock_day <= i < shock_day + 6:
            shock = 1 + (shock_mult - 1) * math.sin(math.pi * (i - shock_day) / 6)
        price = (base + trend * i + weekly) * shock + random.gauss(0, noise)
        price = max(150, round(price))
        rows.append(f"{crop},{mandi},{d.isoformat()},{price}")

out = ["# SAMPLE DATA — illustrative prices for demo purposes, NOT real market data.",
       "# Replace with a real Agmarknet export (crop,mandi,date,price_inr_per_quintal).",
       "# Source for real data: https://agmarknet.gov.in/",
       "crop,mandi,date,price_inr_per_quintal"] + rows
open("data/sample_mandi_prices.csv", "w").write("\n".join(out) + "\n")
print(f"wrote {len(rows)} rows")
