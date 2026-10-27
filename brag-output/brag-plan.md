# brag-plan — MandiLens launch video

## Angle
The sell-timing problem, proof-first: farmers lose money when prices crash *after* harvest. MandiLens tells them next week's price before they load the truck — and is honest about where it works (onion 5.5% error) and where it doesn't (tomato 22%).

## Hook (first 2s)
"WHAT IF FARMERS KNEW NEXT WEEK'S PRICE?" — huge kinetic type, the words NEXT WEEK'S PRICE land in harvest amber.

## Highlights
1. **Four models race** — Seasonal Naive, 7-day Moving Average, 30-day Linear Trend, Ensemble median. Animated MAPE bars; Moving Average wins onion at 5.5%.
2. **Backtested honestly** — walk-forward, last 21 days, 1 day ahead, no look-ahead. Tomato's 22% error shown, not hidden.
3. **The forecast in motion** — the real onion@Vashi chart draws itself: 60 days of history, 7-day forecast band, best-day marker. "Sell today — prices are heading down" for this week's onion.

## Punchline / outro
"MANDILENS — code + live demo at github.com/qasimjethwa/mandilens". Built in a day.

## Tone
Warm, confident, plain-spoken. No hype adjectives — the numbers are the swagger.

## Visual identity (merged styles)
1. **Warm agritech editorial** — the demo's own palette: paper cream #FBF6EC, deep leaf green #1B4332, harvest amber #E9A03B, terracotta #C05621. Serif display feel via heavy black type.
2. **Brutalist kinetic type** — huge black-weight headlines slamming in with overshoot easing; mono labels for data.
3. **The real chart, in motion** — onion@Vashi prices from docs/data.json + forecast from docs/metrics.json, drawn live; not a mock.
(Cosmos.so/explore/ui-ux attempted again for reference — still a JS-walled gallery, text fetch returns placeholder titles only, no usable visuals. The three styles above merge the demo's own design with the approved sample look.)

## Storyboard (20s, 30fps)
| t | scene | content |
|---|---|---|
| 0.0–2.5 | hook | "WHAT IF FARMERS KNEW" / "NEXT WEEK'S PRICE?" (amber) — sub "they sell at a loss when prices crash after harvest" |
| 2.5–5.5 | reveal | MANDILENS wordmark, "Crop price forecasting for Indian farmers", pills "PURE JAVA · 4 MODELS · NO BACKEND" |
| 5.5–9.5 | race | "FOUR MODELS RACE" — 4 MAPE bars animate in (onion@Vashi): MA 6.7% wins, Seasonal 10.0%, Trend 10.4%, Ensemble 7.6% |
| 9.5–13.5 | forecast | real chart draws: history line + amber forecast band + best-day marker; "7-DAY FORECAST" / "best day to sell — or sell today" |
| 13.5–17.0 | honest numbers | counters: "ONION 5.5% · POTATO 5.0% · TOMATO 22%" + "honest about the hard one" |
| 17.0–20.0 | outro | MANDILENS + pills github.com/qasimjethwa/mandilens and live demo URL; "built in a day" |

## Audio
Warm major-key electronic bed (C major, ~100bpm arp, soft kick) + spoken voiceover (tts), music ducked −14dB under voice.

## Voiceover script (voiceover.txt, ~48 words)
"What if farmers knew next week's mandi price? MandiLens races four forecasting models, backtests them with no look-ahead, and picks the winner for each crop. Onion: five-point-five percent error. It forecasts seven days ahead — and names the best day to sell. Code and live demo at the link below."
